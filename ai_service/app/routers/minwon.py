# app/routers/minwon.py
from fastapi import APIRouter, UploadFile, File, Form
from typing import Optional
from ..schemas.minwon import Minwon
from ..services.ocr import ocr_image_to_text   # async 함수라고 가정 (sync면 await 제거)
from ..services.extractor import simple_summarize
import re

router = APIRouter()

PHONE_RE = re.compile(r"(01[016789])[-.\s]?(?:\d{3,4})[-.\s]?(\d{4})")
EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
DATE_RE  = re.compile(r"(\d{4}[./-]\d{1,2}[./-]\d{1,2})|(\d{1,2}월\s?\d{1,2}일)")

@router.post("/minwon/prepare", response_model=Minwon)
async def minwon_prepare(
    text: Optional[str] = Form(default=None),
    file: Optional[UploadFile] = File(default=None),
    consent: bool = Form(default=False),
):
    content = (text or "").strip()

    # 이미지가 있으면 OCR → content에 합치기
    if file is not None:
        ocr_text = await ocr_image_to_text(file)   # sync면 await 제거
        content = (content + "\n" + ocr_text).strip() if content else ocr_text.strip()

    m = Minwon()

    # 1) 제목/본문
    parts = [s.strip() for s in re.split(r"(?<=[.!?])\s+|\n+", content) if s.strip()]
    m.subject = (parts[0] if parts else "민원 신청의 건")[:60]
    m.description = simple_summarize(content, 5)

    # 2) 연락처/이메일
    if (ph := PHONE_RE.search(content)):
        num = re.sub(r"\D", "", ph.group(0))
        if len(num) == 10:
            m.applicant.phone = f"{num[:3]}-{num[3:6]}-{num[6:]}"
        elif len(num) == 11:
            m.applicant.phone = f"{num[:3]}-{num[3:7]}-{num[7:]}"
        else:
            m.applicant.phone = ph.group(0)

    if (em := EMAIL_RE.search(content)):
        m.applicant.email = em.group(0)

    # 3) 날짜/카테고리(초간단)
    if (dt := DATE_RE.search(content)):
        m.incident.date = dt.group(0)

    if "소음" in content:
        m.category = "소음"
    elif "주차" in content:
        m.category = "주차"
    elif "쓰레기" in content or "불법투기" in content:
        m.category = "불법투기"
    else:
        m.category = "기타"

    # 4) 동의 반영 및 필수값 체크
    m.consent = bool(consent)
    required = {
        "applicant.name": m.applicant.name,
        "applicant.phone": m.applicant.phone,
        "subject": m.subject,
        "description": m.description,
        "consent": m.consent,
    }
    m.missing_fields = [k for k, v in required.items() if not v]

    return m
