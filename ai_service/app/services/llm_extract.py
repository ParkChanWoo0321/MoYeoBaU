from .extractor import _heuristic_extract_fields
from .llm_client import call_ollama
from ..schemas.fields import ComplaintFields

PROMPT_SYS = """당신은 한국어 민원서 추출기입니다.
입력 텍스트에서 위치/현상/문제점/위험성/요청사항을 JSON으로만 추출하세요.
형식:
{"위치":"","현상":"","문제점":"","위험성":"","요청사항":""}
"""

def llm_extract(joined: str) -> ComplaintFields:
    rule = _heuristic_extract_fields(joined)
    if all([rule.위치, rule.현상, rule.문제점, rule.위험성, rule.요청사항]):
        return rule  # 룰만으로 충분

    prompt = PROMPT_SYS + "\n입력:\n" + joined
    try:
        data = call_ollama(prompt)
        # 룰 값이 있으면 유지, 없으면 LLM 값 사용
        merged = {
            "위치": rule.위치 or data.get("위치",""),
            "현상": rule.현상 or data.get("현상",""),
            "문제점": rule.문제점 or data.get("문제점",""),
            "위험성": rule.위험성 or data.get("위험성",""),
            "요청사항": rule.요청사항 or data.get("요청사항",""),
        }
        return ComplaintFields(**merged)
    except Exception:
        return rule
