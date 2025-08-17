import re


from ..schemas.io import SummarizeRequest
from ..schemas.fields import ComplaintFields

def simple_summarize(text: str, k: int = 3) -> str:
    sents = [s.strip() for s in re.split(r"(?<=[.!?])\s+|\n+", text) if s.strip()]
    return " ".join(sents[:k]) if sents else text

def run_extract(req: SummarizeRequest) -> ComplaintFields:
    """
    이미지 캡션 → 텍스트 결합 → Ollama(or 휴리스틱) 구조화 추출 흐름.
    """
    # 사용자가 meta.ignore_images를 켜면 이미지 무시(속도 우선)
    use_images = not bool(req.meta.get("ignore_images"))
    captions = _get_captions(req.images) if (req.images and use_images) else []

    # 입력 길이 제한/노이즈 제거
    clean_text = _ko_cleanup_noise(_cap_input(req.complaint_text, HARD_INPUT_LIMIT))
    joined = _compose_input(clean_text, captions)

    # Ollama 가능 여부에 따라 LLM 또는 룰 기반으로 추출
    if _ollama_available():
        fields = _ollama_structured_with_fallback(joined)
    else:
        fields = _heuristic_extract_fields(joined)

    # 혹시라도 전부 빈값이면 마지막으로 룰 기반 재시도
    if not any(getattr(fields, k) for k in ["위치", "현상", "문제점", "위험성", "요청사항"]):
        fields = _heuristic_extract_fields(joined)

    return fields