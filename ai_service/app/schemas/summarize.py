from pydantic import BaseModel

class SummarizeIn(BaseModel):
    text: str
    max_sentences: int = 3

class SummarizeOut(BaseModel):
    summary: str