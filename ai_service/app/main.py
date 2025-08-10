from fastapi import FastAPI
from pydantic import BaseModel
from fastapi.middleware.cors import CORSMiddleware
import re

app = FastAPI(title="Seosan AI Service (MVP)")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://127.0.0.1:8080"],  # 필요하면 "*" 추가
    allow_methods=["*"],
    allow_headers=["*"],
)

class SummarizeIn(BaseModel):
    text: str
    max_sentences: int = 3

class SummarizeOut(BaseModel):
    summary: str

def simple_summarize(text: str, k: int = 3) -> str:
    sents = [s.strip() for s in re.split(r"(?<=[.!?])\s+|\n+", text) if s.strip()]
    return " ".join(sents[:k]) if sents else text

@app.get("/health")
def health():
    return {"ok": True}

@app.post("/ai/summarize", response_model=SummarizeOut)
def summarize_api(body: SummarizeIn):
    return SummarizeOut(summary=simple_summarize(body.text, body.max_sentences))
