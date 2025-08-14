from fastapi import APIRouter
from ..schemas.summarize import SummarizeIn, SummarizeOut
from ..services.extractor import simple_summarize

router = APIRouter()

@router.post("/summarize", response_model=SummarizeOut)
def summarize_api(body: SummarizeIn):
    return SummarizeOut(summary=simple_summarize(body.text, body.max_sentences))
