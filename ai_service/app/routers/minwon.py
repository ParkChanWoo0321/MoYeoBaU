from fastapi import APIRouter, Request
from ..schemas.io import SummarizeRequest
from ..schemas.fields import ComplaintFields
from ..schemas.compose import ComposeIn, ComposeOut
from ..services.extractor import run_extract
from ..services.composer import compose_document

router = APIRouter(prefix="/ai/minwon", tags=["minwon"])

@router.post("/prepare", response_model=ComplaintFields)
async def prepare(request: Request):
    """
    (호환용) 기존과 동일: JSON/form-data 모두 허용하려면
    프런트에서 JSON로 보내는 게 가장 단순합니다.
    Body 예: { "complaint_text": "...", "images": [ { "url": "..." } ], "meta": {...} }
    """
    data = await request.json() if "application/json" in (request.headers.get("content-type") or "") else {}
    req = SummarizeRequest(**data) if isinstance(data, dict) else SummarizeRequest()
    fields = run_extract(req)  # ✅ 네 기존 추출 파이프라인 호출
    return fields

@router.post("/compose", response_model=ComposeOut)
async def compose(inbody: ComposeIn):
    """
    민원서 작성 엔드포인트.
    - inbody.fields 가 있으면 그대로 문서화
    - 없으면 complaint_text/images로 먼저 추출 → 문서화
    """
    if inbody.fields:
        fields = inbody.fields
    else:
        req = SummarizeRequest(
            complaint_text=inbody.complaint_text or "",
            images=inbody.images or [],
            meta=inbody.meta or {}
        )
        fields = run_extract(req)

    doc = compose_document(fields, inbody.meta or {})
    return ComposeOut(title=doc["title"], body=doc["body"], html=doc["html"], fields=fields)
