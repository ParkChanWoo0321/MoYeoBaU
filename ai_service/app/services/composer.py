import os, re, requests, time, json
from typing import Dict, Any
from ..schemas.fields import ComplaintFields

# Ollama 설정(네 환경변수 규칙 재활용)
OLLAMA_BASE   = os.getenv("OLLAMA_BASE", "http://127.0.0.1:11434").rstrip("/")
OLLAMA_MODEL  = os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")
READ_TIMEOUT  = int(os.getenv("OLLAMA_READ_TIMEOUT", "600"))
CONNECT_TO    = int(os.getenv("OLLAMA_CONNECT_TIMEOUT", "60"))

def _norm(s: str) -> str:
    s = (s or "").strip()
    s = re.sub(r"\s+", " ", s)
    return s

def _ollama_available() -> bool:
    try:
        requests.get(f"{OLLAMA_BASE}/api/tags", timeout=2)
        return True
    except Exception:
        return False

def _post_chat_json(messages, num_ctx=2048, num_predict=400, temperature=0.2) -> str:
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
    r = requests.post(f"{OLLAMA_BASE}/api/chat", json=payload,
                      timeout=(CONNECT_TO, READ_TIMEOUT))
    r.raise_for_status()
    j = r.json()
    return (j.get("message", {}) or {}).get("content", "") or j.get("response", "") or ""

def _fallback_template(f: ComplaintFields, meta: Dict[str, Any]) -> Dict[str, str]:
    # 아주 깔끔한 격식체 기본 템플릿 (의존성 없이 HTML 문자열 생성)
    org   = _norm(meta.get("org", ""))         # 접수 기관명(선택)
    title = _norm(meta.get("title_prefix", "")) + (_norm(f.문제점) or "민원 신청의 건")
    title = title.strip()
    if not title.endswith(("건", "의 건")):
        title += ""

    body = f"""\
안녕하세요. {org or "관계자"}님.

아래 사항에 대한 민원을 신청드립니다.

1) 위치: {f.위치 or "미상"}
2) 현상: {f.현상 or "미상"}
3) 문제점: {f.문제점 or "미상"}
4) 위험성: {f.위험성 or "미상"}
5) 요청사항: {f.요청사항 or "미상"}

위 사안에 대한 확인과 적절한 조치를 부탁드립니다.
감사합니다.
""".rstrip()

    html = f"""\
<!doctype html>
<html lang="ko"><head>
<meta charset="utf-8" />
<title>{title}</title>
<style>
  body {{ font-family: -apple-system, 'Noto Sans KR', Arial, sans-serif; line-height:1.7; color:#111; }}
  .doc {{ max-width: 720px; margin: 40px auto; padding: 32px; border:1px solid #e5e7eb; border-radius: 14px; }}
  h1 {{ font-size: 22px; margin: 0 0 16px 0; }}
  .meta {{ color:#6b7280; margin-bottom: 18px; }}
  ul {{ padding-left: 18px; }}
  li {{ margin: 6px 0; }}
  .footer {{ margin-top: 24px; color:#374151; }}
</style></head>
<body><div class="doc">
  <h1>{title}</h1>
  <div class="meta">{org or ""}</div>
  <p>아래 사항에 대한 민원을 신청드립니다.</p>
  <ul>
    <li><b>위치</b>: {f.위치 or "미상"}</li>
    <li><b>현상</b>: {f.현상 or "미상"}</li>
    <li><b>문제점</b>: {f.문제점 or "미상"}</li>
    <li><b>위험성</b>: {f.위험성 or "미상"}</li>
    <li><b>요청사항</b>: {f.요청사항 or "미상"}</li>
  </ul>
  <p class="footer">위 사안에 대한 확인과 적절한 조치를 부탁드립니다.</p>
</div></body></html>
"""
    return {"title": title or "민원 신청의 건", "body": body, "html": html}

def compose_document(fields: ComplaintFields, meta: Dict[str, Any]) -> Dict[str, str]:
    """
    (1) 가능하면 LLM로 격식체 본문 생성 → (2) 실패 시 템플릿 폴백
    meta 예시:
      - title_prefix: "[서산시] "
      - org: "서산시 도시과"
      - style: "brief|polite" (현재는 LLM 프롬프트에만 반영)
    """
    f = fields
    # LLM 프롬프트 (한국어 격식체, 항목 빠지면 상상 금지)
    sys = "너는 공공 민원서 작성 도우미다. 존칭을 사용하고 간결하게, 사실만 기술해라."
    usr = f"""다음 필드를 바탕으로 '민원서 제목'과 '겸손하고 간결한 본문'을 한국어로 작성해.
누락된 항목은 '미상'으로 표기하고 새로운 정보를 지어내지 마.

필드:
- 위치: {f.위치 or "미상"}
- 현상: {f.현상 or "미상"}
- 문제점: {f.문제점 or "미상"}
- 위험성: {f.위험성 or "미상"}
- 요청사항: {f.요청사항 or "미상"}

출력 형식(JSON):
{{
  "title": "제목 한 줄",
  "body": "격식체 본문(3~6문장)"
}}"""

    if _ollama_available():
        try:
            txt = _post_chat_json([
                {"role": "system", "content": sys},
                {"role": "user",   "content": usr},
            ], num_ctx=2048, num_predict=320, temperature=0.2).strip()
            # JSON 파싱 시도
            obj = None
            try:
                m = re.search(r"\{[\s\S]*\}", txt)
                if m: obj = json.loads(m.group(0))
            except Exception:
                obj = None
            if isinstance(obj, dict) and obj.get("title") and obj.get("body"):
                title = obj["title"].strip()
                if meta.get("title_prefix"):
                    title = f"{meta['title_prefix'].strip()} {title}".strip()
                body  = obj["body"].strip()
                # 간단 후처리(길이/문장부호)
                title = title[:80]
                return {**_fallback_template(f, meta), "title": title, "body": body}
        except Exception:
            pass

    # LLM 실패 시 기본 템플릿
    return _fallback_template(f, meta)
