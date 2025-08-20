import os, re, requests, json
from typing import Dict, Any
from ..schemas.fields import ComplaintFields

# (옵션) Ollama – 기본은 OFF
USE_LLM_COMPOSE = os.getenv("COMPOSE_USE_LLM", "0") == "1"
OLLAMA_BASE   = os.getenv("OLLAMA_BASE", "http://127.0.0.1:11434").rstrip("/")
OLLAMA_MODEL  = os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")
LLM_CONNECT_TO = int(os.getenv("COMPOSE_CONNECT_TIMEOUT", "2"))
LLM_READ_TO    = int(os.getenv("COMPOSE_READ_TIMEOUT", "8"))

def _norm(s: str) -> str:
    import re
    s = (s or "").strip()
    return re.sub(r"\s+", " ", s)

def _ollama_available() -> bool:
    if not USE_LLM_COMPOSE:
        return False
    try:
        requests.get(f"{OLLAMA_BASE}/api/tags", timeout=2)
        return True
    except Exception:
        return False

def _post_chat_json(messages, num_ctx=2048, num_predict=320, temperature=0.2) -> str:
    payload = {
        "model": OLLAMA_MODEL,
        "messages": messages,
        "options": {
            "num_ctx": num_ctx,
            "num_predict": num_predict,
            "temperature": temperature,
            "repeat_penalty": 1.2,
        },
        "stream": False
    }
    r = requests.post(
        f"{OLLAMA_BASE}/api/chat",
        json=payload,
        timeout=(LLM_CONNECT_TO, LLM_READ_TO)
    )
    r.raise_for_status()
    j = r.json()
    return (j.get("message", {}) or {}).get("content", "") or j.get("response", "") or ""

def _fallback_template(f: ComplaintFields, meta: Dict[str, Any], include_html: bool = False) -> Dict[str, str]:
    prefix = _norm(meta.get("title_prefix", ""))
    if prefix and not prefix.endswith(" "):
        prefix += " "
    core = _norm(f.문제점) or "민원 신청의 건"
    title = f"{prefix}{core}".strip()

    org = _norm(meta.get("org", ""))
    receiver = _norm(meta.get("receiver", ""))
    greet = receiver or (f"{org} 귀하" if org else "관계자 귀하")

    body = f"""\
{greet}

아래 사항에 대한 민원을 신청드립니다.

1) 위치: {f.위치 or "미상"}
2) 현상: {f.현상 or "미상"}
3) 문제점: {f.문제점 or "미상"}
4) 위험성: {f.위험성 or "미상"}
5) 요청사항: {f.요청사항 or "미상"}

위 사안에 대한 확인과 적절한 조치를 부탁드립니다.
감사합니다.
""".rstrip()

    result = {"title": title or "민원 신청의 건", "body": body}
    if include_html:
        # 필요시만 html 생성 (기본은 안 씀)
        result["html"] = "<!-- omitted in MVP -->"
    return result

def compose_document(fields: ComplaintFields, meta: Dict[str, Any], include_html: bool = False) -> Dict[str, str]:
    # 기본: 즉시 폴백
    if not _ollama_available():
        return _fallback_template(fields, meta, include_html=include_html)

    # (옵션) LLM 한 번만 짧게 시도
    sys = "너는 공공 민원서 작성 도우미다. 존칭을 사용하고 간결하게, 사실만 기술해라."
    usr = f"""다음 필드로 제목과 본문을 한국어로.
- 위치: {fields.위치 or "미상"}
- 현상: {fields.현상 or "미상"}
- 문제점: {fields.문제점 or "미상"}
- 위험성: {fields.위험성 or "미상"}
- 요청사항: {fields.요청사항 or "미상"}

JSON만:
{{"title":"...", "body":"..."}}"""

    try:
        txt = _post_chat_json(
            [{"role":"system","content":sys}, {"role":"user","content":usr}],
        ).strip()
        m = re.search(r"\{[\s\S]*\}", txt)
        obj = json.loads(m.group(0)) if m else None
        if isinstance(obj, dict) and obj.get("title") and obj.get("body"):
            title = obj["title"].strip()[:80]
            pref = _norm(meta.get("title_prefix",""))
            if pref and not pref.endswith(" "): pref += " "
            title = f"{pref}{title}".strip()
            base = _fallback_template(fields, meta, include_html=include_html)
            base["title"] = title
            base["body"]  = obj["body"].strip()
            return base
    except Exception:
        pass

    return _fallback_template(fields, meta, include_html=include_html)