import re

def simple_summarize(text: str, k: int = 3) -> str:
    sents = [s.strip() for s in re.split(r"(?<=[.!?])\s+|\n+", text) if s.strip()]
    return " ".join(sents[:k]) if sents else text
