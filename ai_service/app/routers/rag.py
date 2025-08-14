from fastapi import APIRouter
from pydantic import BaseModel
from ..services.learn import ingest_example, search_similar

router = APIRouter(prefix="/ai", tags=["learn"])

class ExampleIn(BaseModel):
    text: str              # 원문(짧게 적은 설명)
    minwon: dict           # 확정된 Minwon JSON(스키마)

@router.post("/examples/ingest")
def ingest_api(body: ExampleIn):
    return ingest_example(body.text, body.minwon, source="confirmed")

@router.get("/examples/search")
def search_api(q: str, k: int = 3):
    return search_similar(q, k)
