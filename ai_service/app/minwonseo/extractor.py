# app/services/extractor.py
import os, re, json, requests
from typing import Dict, Any, List, Optional
from ..schemas.io import SummarizeRequest   # complaint_text, images[], meta
from ..schemas.fields import ComplaintFields

# ===== 설정 (환경변수) =====
USE_LLM          = os.getenv("EXTRACT_USE_LLM", "1") == "1"     # 기본 ON
OLLAMA_URL       = os.getenv("OLLAMA_URL", "http://127.0.0.1:11434").rstrip("/")
EXTRACT_MODEL    = os.getenv("EXTRACT_MODEL", "llama3.1:8b")
CONNECT_TIMEOUT  = int(os.getenv("EXTRACT_CONNECT_TIMEOUT", "2"))
READ_TIMEOUT     = int(os.getenv("EXTRACT_READ_TIMEOUT", "8"))

# ===== 유틸 =====
def _normalize(text: str) -> str:
    t = (text or "").strip()
    t = re.sub(r"\s+", " ", t)
    return t

def _sent_split_ko(text: str) -> List[str]:
    t = _normalize(text)
    if not t:
        return []
    try:
        import kss  # optional
        return [s.strip() for s in kss.split_sentences(t) if s.strip()]
    except Exception:
        tmp = re.sub(r"(다\.|요\.|니다\.|습니다\.|[.!?])\s*", r"\1<eos>", t)
        return [s.strip() for s in tmp.split("<eos>") if s.strip()]

def _urls_hint(images) -> str:
    urls = [getattr(i, "url", "") for i in (images or []) if getattr(i, "url", "")]
    if not urls:
        return ""
    return "참고 이미지 URL: " + " ".join(urls[:5])

# ===== 휴리스틱 규칙 =====
_ADDR_PAT = re.compile(
    r"(?:[가-힣A-Za-z0-9]+(?:시|군|구|읍|면|동|리)|"
    r"[가-힣A-Za-z0-9]+(?:로|길)\s?\d*(?:-\d+)?|"
    r"[가-힣A-Za-z0-9]+(?:아파트|초등학교|학교|공원|역|병원|사거리|교차로|공사장)(?:\s*인근)?)"
)
_REQ_PAT  = re.compile(r"(조치|정비|수리|보수|교체|정리|단속|처리|확인)\S*|해\s*주세요|요청|바랍니다")
_RISK_PAT = re.compile(r"(위험|사고|미끄|감전|화재|추락|파손\s*심함|야간\s*어두움?)")
_PROB_PAT = re.compile(r"(불법\s?주정차|파손|고장|막힘|싱크홀|파열|누수|악취|소음|불량|방치|불편)")

def _first(pat: re.Pattern, text: str) -> str:
    m = pat.search(text)
    return m.group(0) if m else ""

def _heuristic(text: str) -> ComplaintFields:
    t = _normalize(text)
    sents = _sent_split_ko(t)
    first = sents[0] if sents else t

    위치     = _first(_ADDR_PAT, t)
    문제점   = _first(_PROB_PAT, t)
    위험성   = _first(_RISK_PAT, t)
    요청사항 = _first(_REQ_PAT, t)
    현상     = first or ""

    # 간단 보강
    if not 위험성 and ("주정차" in (문제점 + " " + t)):
        위험성 = "보행자·차량 시야 방해로 사고 위험"
    if not 요청사항 and ("주정차" in (문제점 + " " + t)):
        요청사항 = "단속 강화 및 안내 표지 설치 요청"
    if not 요청사항 and ("파손" in (문제점 + " " + t)):
        요청사항 = "긴급 보수 요청"

    def clean(x): return _normalize(x)[:100]
    return ComplaintFields(
        위치=clean(위치 or "미상"),
        현상=clean(현상 or "미상"),
        문제점=clean(문제점 or "미상"),
        위험성=clean(위험성 or "미상"),
        요청사항=clean(요청사항 or "미상"),
    )

# ===== LLM 보강 =====
def _ollama_ok() -> bool:
    if not USE_LLM:
        return False
    try:
        r = requests.get(f"{OLLAMA_URL}/api/tags", timeout=CONNECT_TIMEOUT)
        return r.status_code == 200
    except Exception:
        return False

def _llm_fill_missing(base_text: str, locked: ComplaintFields) -> Optional[ComplaintFields]:
    """
    휴리스틱으로 채운 값은 '잠금'으로 두고, 비어있는 칸만 LLM이 보완하도록 요청.
    """
    sys = (
        "너는 한국어 민원서 '필드 추출기'다. "
        "반드시 다음 스키마의 JSON 객체 한 개만 출력해라. "
        '스키마: {"위치":"","현상":"","문제점":"","위험성":"","요청사항":""} '
        "설명/문장/마크다운 금지. 각 필드 120자 이내."
    )
    locked_dict = {
        "위치": locked.위치, "현상": locked.현상, "문제점": locked.문제점,
        "위험성": locked.위험성, "요청사항": locked.요청사항
    }
    user = (
        f"입력 텍스트:\n{base_text}\n\n"
        f"이미 추출된 값(잠금값, 빈칸만 채워):\n{json.dumps(locked_dict, ensure_ascii=False)}"
    )
    payload = {
        "model": EXTRACT_MODEL,
        "messages": [
            {"role":"system","content":sys},
            {"role":"user","content":user},
        ],
        "options": {"temperature": 0.2},
        "stream": False
    }
    try:
        r = requests.post(
            f"{OLLAMA_URL}/api/chat", json=payload,
            timeout=(CONNECT_TIMEOUT, READ_TIMEOUT)
        )
        r.raise_for_status()
        j = r.json()
        txt = (j.get("message", {}) or {}).get("content", "") or j.get("response", "")
        if not txt:
            return None
        m = re.search(r"\{[\s\S]*\}", txt)
        data = json.loads(m.group(0)) if m else None
        if not isinstance(data, dict):
            return None
        # 잠금 우선 병합 + 정리
        merged: Dict[str, str] = {}
        for k in ("위치","현상","문제점","위험성","요청사항"):
            if getattr(locked, k) and getattr(locked, k) != "미상":
                merged[k] = _normalize(getattr(locked, k))[:120]
            else:
                merged[k] = _normalize(data.get(k, "") or "미상")[:120]
        return ComplaintFields(**merged)
    except Exception:
        return None

def _score(f: ComplaintFields) -> int:
    s = 0
    if f.현상: s += 2
    if f.요청사항: s += 2
    if f.문제점: s += 2
    if f.위험성: s += 1
    if f.위치 and len(f.위치) >= 2: s += 1
    return s

# ===== 공개 함수 =====
def run_extract(req: SummarizeRequest) -> ComplaintFields:
    """
    텍스트(+이미지 URL을 힌트 문자열로만 결합) → (휴리스틱) → 부족하면 LLM 보강 → 결과
    """
    # 1) 텍스트 + URL 힌트 결합
    text = _normalize(getattr(req, "complaint_text", "") or "")
    url_hint = _urls_hint(getattr(req, "images", []))  # URL만 문자열로 참고
    joined = " ".join([t for t in [text, url_hint] if t]).strip()

    # 2) 휴리스틱 1차
    fields_rule = _heuristic(joined)

    # 충분히 채워졌거나 LLM 불가 → 바로 반환
    if _score(fields_rule) >= 6 or not _ollama_ok():
        return fields_rule

    # 3) LLM으로 부족한 칸 보강 (잠금 우선)
    fields_llm = _llm_fill_missing(joined, fields_rule)
    if fields_llm:
        return fields_llm

    # 4) 실패 시 휴리스틱 결과로 폴백
    return fields_rule