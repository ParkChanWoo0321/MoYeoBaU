import re
from typing import Dict, Any, Union
from .fields import ComplaintFields

_PRONOUN_LOC = {"우리", "여기", "근처", "주변", "이곳", "그곳"}

def _norm(s: str) -> str:
    s = (s or "").strip()
    return re.sub(r"\s+", " ", s)

def tidy_meta(meta: Dict[str, Any]) -> Dict[str, Any]:
    m = dict(meta or {})
    for k in ("org", "receiver", "title_prefix"):
        if isinstance(m.get(k), str):
            m[k] = m[k].strip()
    return m

def _fix_tail_ko(t: str) -> str:
    # “…습니다이/…니다이” → “…습니다/…니다”
    t = re.sub(r"(습니다|니다)(이)(\s|$)", r"\1\3", t)
    # “(이/가) 발생했습니다.” 꼬리 제거
    t = re.sub(r"\s*(이|가)?\s*발생했습니다\.?$", "", t)
    # “…이 필요합니다이” → “…이 필요합니다.”
    t = re.sub(r"\s*이\s*필요합니다이\.?$", "이 필요합니다.", t)
    # 중복 공백
    t = re.sub(r"\s{2,}", " ", t)
    return t.strip()

def tidy_title(s: str) -> str:
    t = _norm(s)
    t = t.replace("[ ", "[").replace(" ]", "]")
    t = _fix_tail_ko(t)
    return t[:80]

def tidy_fields(fields: Union[ComplaintFields, Dict[str, str]]) -> ComplaintFields:
    if isinstance(fields, ComplaintFields):
        d = {
            "위치": fields.위치, "현상": fields.현상, "문제점": fields.문제점,
            "위험성": fields.위험성, "요청사항": fields.요청사항
        }
    else:
        d = dict(fields or {})
    out: Dict[str, str] = {}
    for k in ("위치","현상","문제점","위험성","요청사항"):
        t = _fix_tail_ko(_norm(d.get(k, "") or ""))
        if k == "위치":
            base = re.sub(r"(에서|에)$", "", t)
            if base in _PRONOUN_LOC or len(base) < 2:
                t = "미상"
        out[k] = t[:120] if t else "미상"
    return ComplaintFields(**out)

def render_body(meta: Dict[str, Any], f: ComplaintFields) -> str:
    m = tidy_meta(meta)
    f = tidy_fields(f)
    receiver = _norm(m.get("receiver") or (f"{m.get('org','')} 귀하".strip()) or "관계자 귀하")
    return (
        f"{receiver}\n\n"
        f"아래 사항에 대한 민원을 신청드립니다.\n\n"
        f"1) 위치: {f.위치 or '미상'}\n"
        f"2) 현상: {f.현상 or '미상'}\n"
        f"3) 문제점: {f.문제점 or '미상'}\n"
        f"4) 위험성: {f.위험성 or '미상'}\n"
        f"5) 요청사항: {f.요청사항 or '미상'}\n\n"
        f"확인 및 적절한 조치를 부탁드립니다.\n감사합니다."
    )
