import os, re, requests, json
from typing import Dict, Any
from .fields import ComplaintFields
from .textutils_ko import tidy_meta, tidy_fields, tidy_title, render_body

USE_LLM_COMPOSE = os.getenv("COMPOSE_USE_LLM", "0") == "1"
OLLAMA_BASE     = os.getenv("OLLAMA_BASE", "http://127.0.0.1:11434").rstrip("/")
OLLAMA_MODEL    = os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")
LLM_CONNECT_TO  = int(os.getenv("COMPOSE_CONNECT_TIMEOUT", "2"))
LLM_READ_TO     = int(os.getenv("COMPOSE_READ_TIMEOUT", "8"))

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
    meta = tidy_meta(meta)
    f = tidy_fields(f)

    prefix = _norm(meta.get("title_prefix", ""))
    if prefix and not prefix.endswith(" "):
        prefix += " "

    core = _norm(f.문제점) or "민원 신청의 건"
    title = tidy_title(f"{prefix}{core}".strip())

    body = render_body(meta, f)

    result = {"title": title or "민원 신청의 건", "body": body}
    if include_html:
        result["html"] = "<!-- omitted in MVP -->"
    return result

def compose_document(fields: ComplaintFields, meta: Dict[str, Any], include_html: bool = False) -> Dict[str, str]:
    meta = tidy_meta(meta)
    fields = tidy_fields(fields)

    if not _ollama_available():
        return _fallback_template(fields, meta, include_html=include_html)

    sys = "너는 공공 민원서 작성 도우미다. 존칭을 사용하고 간결하게, 사실만 기술해라. 출력은 JSON 한 개만."
    usr = f"""다음 필드로 '제목'만 개선해줘. 본문은 생성하지 마.
- 위치: {fields.위치 or "미상"}
- 현상: {fields.현상 or "미상"}
- 문제점: {fields.문제점 or "미상"}
- 위험성: {fields.위험성 or "미상"}
- 요청사항: {fields.요청사항 or "미상"}

JSON만:
{{"title":"..."}}"""

    try:
        txt = _post_chat_json(
            [{"role":"system","content":sys}, {"role":"user","content":usr}],
        ).strip()
        m = re.search(r"\{[\s\S]*\}", txt)
        obj = json.loads(m.group(0)) if m else None
        if isinstance(obj, dict) and obj.get("title"):
            title_raw = obj["title"].strip()
            title = tidy_title(title_raw)
            pref = _norm(meta.get("title_prefix",""))
            if pref and not pref.endswith(" "):
                pref += " "
            title = f"{pref}{title}".strip()

            base = _fallback_template(fields, meta, include_html=include_html)
            base["title"] = title
            base["body"]  = render_body(meta, fields)
            return base
    except Exception:
        pass

    return _fallback_template(fields, meta, include_html=include_html)
