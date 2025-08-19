import os, uuid, pathlib, re, time, json, traceback
from typing import List, Optional, Dict, Any

from fastapi import APIRouter, Request, HTTPException, UploadFile, File, Form
from ..schemas.io import SummarizeRequest, ImageInput
from ..schemas.fields import ComplaintFields
from ..schemas.compose import ComposeIn, ComposeOut
from ..services.extractor import run_extract
from ..services.composer import compose_document

router = APIRouter(prefix="/ai/minwon", tags=["minwon"])

DEFAULT_META = {"org":"서산시청","receiver":"서산시청장 귀하","title_prefix":"[서산시]"}

STATIC_DIR = os.getenv("STATIC_DIR", "static")
UPLOAD_SUBDIR = os.getenv("UPLOAD_SUBDIR", "uploads")
STATIC_BASE_URL = os.getenv("STATIC_BASE_URL", "http://localhost:8080/static")
ALLOWED_MIME = {"image/jpeg", "image/png", "image/webp"}
MAX_FILES = int(os.getenv("UPLOAD_MAX_FILES", "5"))
MAX_BYTES = int(os.getenv("UPLOAD_MAX_BYTES", str(10 * 1024 * 1024)))
CHUNK = 1024 * 1024

def _safe_name(name: str) -> str:
    return (re.sub(r"[^\w.\-]+", "_", (name or "image"))[:80]) or "image"

def _today_path() -> str:
    t = time.gmtime()
    return f"{t.tm_year:04d}/{t.tm_mon:02d}/{t.tm_mday:02d}"

@router.post("/compose", response_model=ComposeOut)
async def compose(inbody: ComposeIn):
    try:
        print("[compose] START")
        meta = {**DEFAULT_META, **(inbody.meta or {})}
        meta["applicant"] = {
            k: v for k, v in {
                "name": inbody.applicant_name,
                "phone": inbody.applicant_phone,
                "address": inbody.applicant_address,
            }.items() if v
        }

        if inbody.fields:
            fields = inbody.fields
        else:
            print("[compose] before run_extract")
            req = SummarizeRequest(
                complaint_text=inbody.complaint_text or "",
                images=inbody.images or [],
                meta=meta
            )
            fields = run_extract(req)
            print("[compose] after run_extract")

        if isinstance(fields, dict):
            fields = ComplaintFields(**fields)

        if inbody.applicant_address:
            fields.위치 = inbody.applicant_address

        print("[compose] before compose_document")
        doc = compose_document(fields, meta, include_html=False)
        print("[compose] after compose_document")
        return ComposeOut(title=doc["title"], body=doc["body"], fields=fields)

    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/compose-form", response_model=ComposeOut)
async def compose_form(
    applicant_name: Optional[str] = Form(None),
    applicant_phone: Optional[str] = Form(None),
    applicant_address: Optional[str] = Form(None),
    complaint_text: str = Form(""),
    meta: Optional[str] = Form(None),
    files: List[UploadFile] = File(None)
):
    try:
        print("[compose-form] START")
        meta_dict: Dict[str, Any] = {**DEFAULT_META}
        if meta:
            try: meta_dict.update(json.loads(meta))
            except Exception: pass
        meta_dict["applicant"] = {k: v for k, v in {
            "name": applicant_name, "phone": applicant_phone, "address": applicant_address
        }.items() if v}

        image_inputs: List[ImageInput] = []
        if files:
            if len(files) > MAX_FILES:
                raise HTTPException(400, f"Too many files (max {MAX_FILES})")
            dest_dir = pathlib.Path(STATIC_DIR) / UPLOAD_SUBDIR / _today_path()
            dest_dir.mkdir(parents=True, exist_ok=True)
            for i, f in enumerate(files):
                if f.content_type not in ALLOWED_MIME:
                    raise HTTPException(400, f"Unsupported content type: {f.content_type}")
                fname = f"{uuid.uuid4()}-{_safe_name(f.filename)}"
                out_path = dest_dir / fname
                total = 0
                with open(out_path, "wb") as out:
                    while True:
                        chunk = await f.read(CHUNK)
                        if not chunk: break
                        total += len(chunk)
                        if total > MAX_BYTES:
                            out.close()
                            try: out_path.unlink()
                            except FileNotFoundError: pass
                            raise HTTPException(400, f"File too large (> {MAX_BYTES} bytes)")
                        out.write(chunk)
                rel = out_path.relative_to(STATIC_DIR).as_posix()
                url = f"{STATIC_BASE_URL}/{rel}"
                print(f"[compose-form] file[{i}] -> {url}")
                image_inputs.append(ImageInput(url=url))

        print("[compose-form] before run_extract")
        req = SummarizeRequest(complaint_text=complaint_text or "", images=image_inputs, meta=meta_dict)
        fields = run_extract(req)
        print("[compose-form] after run_extract")

        if isinstance(fields, dict):
            fields = ComplaintFields(**fields)
        if applicant_address:
            fields.위치 = applicant_address

        print("[compose-form] before compose_document")
        doc = compose_document(fields, meta_dict, include_html=False)
        print("[compose-form] after compose_document")
        return ComposeOut(title=doc["title"], body=doc["body"], fields=fields)

    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))
