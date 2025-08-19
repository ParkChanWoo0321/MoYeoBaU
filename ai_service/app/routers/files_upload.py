# app/routers/files_upload.py
import os
import re
import uuid
import time
from pathlib import Path
from typing import List

from fastapi import APIRouter, UploadFile, File, HTTPException, Request
from pydantic import BaseModel

router = APIRouter(prefix="/files", tags=["files"])

# 업로드 정책
ALLOWED_MIME = {"image/jpeg", "image/png", "image/webp", "image/gif"}
MAX_FILES = int(os.getenv("UPLOAD_MAX_FILES", "5"))
MAX_BYTES_PER_FILE = int(os.getenv("UPLOAD_MAX_BYTES", str(10 * 1024 * 1024)))  # 10MB
CHUNK_SIZE = 1024 * 1024  # 1MB

# 저장 루트: app/static (실행 경로와 무관)
APP_DIR = Path(__file__).resolve().parents[1]            # .../app
STATIC_ROOT = APP_DIR / "static"                          # .../app/static
UPLOADS_SUBDIR = os.getenv("UPLOAD_SUBDIR", "uploads")    # 기본 'uploads'

class UploadOut(BaseModel):
    urls: List[str]

def _safe_name(name: str) -> str:
    """파일명 안전화 (영문/숫자/._-만 허용, 길이 제한)"""
    base = (re.sub(r"[^\w.\-]+", "_", (name or "image"))[:80]) or "image"
    return base

def _today_path() -> str:
    # 로컬 타임스탬프 기준(한국 시각에서 쓰기 용이)
    t = time.localtime()
    return f"{t.tm_year:04d}/{t.tm_mon:02d}/{t.tm_mday:02d}"

def _ext_from(filename: str, content_type: str) -> str:
    # 원본 확장자 우선, 없으면 MIME으로 추론
    ext = Path(filename or "").suffix.lower()
    if ext:
        return ext
    mapping = {
        "image/jpeg": ".jpg",
        "image/png": ".png",
        "image/webp": ".webp",
        "image/gif": ".gif",
    }
    return mapping.get(content_type, ".jpg")

@router.post("/upload", response_model=UploadOut)
async def upload(request: Request, files: List[UploadFile] = File(...)):
    print("[files/upload] start")

    if not files:
        raise HTTPException(status_code=400, detail="No files")
    if len(files) > MAX_FILES:
        raise HTTPException(status_code=400, detail=f"Too many files (max {MAX_FILES})")

    # 저장 경로 보장: app/static/uploads/YYYY/MM/DD
    dest_dir = STATIC_ROOT / UPLOADS_SUBDIR / _today_path()
    dest_dir.mkdir(parents=True, exist_ok=True)

    urls: List[str] = []

    for i, f in enumerate(files):
        print(f"[files/upload] file[{i}] name={f.filename} type={f.content_type}")

        if f.content_type not in ALLOWED_MIME:
            raise HTTPException(status_code=400, detail=f"Unsupported content type: {f.content_type}")

        # 안전한 파일명 + 확장자 보정
        ext = _ext_from(f.filename, f.content_type)
        safe_orig = _safe_name(Path(f.filename or "").stem)  # 확장자 제외한 이름만 정제
        fname = f"{uuid.uuid4().hex}-{safe_orig}{ext}"

        out_path = dest_dir / fname

        # 스트리밍 저장 + 용량 제한
        total = 0
        with out_path.open("wb") as out:
            while True:
                chunk = await f.read(CHUNK_SIZE)
                if not chunk:
                    break
                total += len(chunk)
                if total > MAX_BYTES_PER_FILE:
                    out.close()
                    try:
                        out_path.unlink()
                    except FileNotFoundError:
                        pass
                    raise HTTPException(
                        status_code=400,
                        detail=f"File too large: {f.filename} (> {MAX_BYTES_PER_FILE} bytes)"
                    )
                out.write(chunk)

        # 공개 URL: 요청 base_url + /static/ 이하 경로
        # 예) http://127.0.0.1:8000/static/uploads/2025/08/20/xxxx.jpg
        base_url = str(request.base_url).rstrip("/")
        rel = out_path.relative_to(STATIC_ROOT).as_posix()  # uploads/2025/08/20/...
        url = f"{base_url}/static/{rel}"

        print(f"[files/upload] file[{i}] saved -> {url}")
        urls.append(url)

    print("[files/upload] done")
    return UploadOut(urls=urls)
