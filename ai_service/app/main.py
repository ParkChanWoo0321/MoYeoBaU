from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import os, re, base64, importlib, requests, time, json
import anyio
from app.minwonseo.io import ImageInput
from anyio import fail_after
from app.minwonseo.io import SummarizeRequest
from app.minwonseo.compose import ComposeIn, ComposeOut
from app.minwonseo.composer import compose_document
from app.minwonseo.fields import ComplaintFields
from fastapi import Form, File, UploadFile
from fastapi import UploadFile as _UploadFile
from app.minwonseo.extractor import _run

try:
    from anyio import TimeoutError as AnyioTimeoutError
except Exception:
    try:
        from anyio.exceptions import TimeoutError as AnyioTimeoutError
    except Exception:
        AnyioTimeoutError = TimeoutError

app = FastAPI(
    title="Seosan AI Service (Ollama Summarizer)",
    docs_url="/ai/docs",
    openapi_url="/ai/openapi.json")

OLLAMA_BASE = os.getenv("OLLAMA_BASE", "http://ollama:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")
OLLAMA_ALT_MODEL = os.getenv("OLLAMA_ALT_MODEL", "qwen2.5:3b-instruct")
USE_KSS = os.getenv("USE_KSS", "0") in ("1", "true", "True")

OLLAMA_CONNECT_TIMEOUT = int(os.getenv("OLLAMA_CONNECT_TIMEOUT", "60"))
OLLAMA_READ_TIMEOUT    = int(os.getenv("OLLAMA_READ_TIMEOUT", "600"))
OLLAMA_RETRIES         = int(os.getenv("OLLAMA_RETRIES", "3"))
OLLAMA_BACKOFF         = float(os.getenv("OLLAMA_BACKOFF", "2.0"))
OLLAMA_NUM_PREDICT     = int(os.getenv("OLLAMA_NUM_PREDICT", "400"))
OLLAMA_NUM_CTX         = int(os.getenv("OLLAMA_NUM_CTX", "4096"))

FALLBACK_NUM_PREDICT   = int(os.getenv("FALLBACK_NUM_PREDICT", "250"))
FALLBACK_CTX           = int(os.getenv("FALLBACK_CTX", "3072"))
FALLBACK_READ_TIMEOUT  = int(os.getenv("FALLBACK_READ_TIMEOUT", "240"))
HARD_INPUT_LIMIT       = int(os.getenv("HARD_INPUT_LIMIT", "2200"))
FALLBACK_INPUT_LIMIT   = int(os.getenv("FALLBACK_INPUT_LIMIT", "1200"))

IGNORE_IMAGES_DEFAULT  = os.getenv("IGNORE_IMAGES_DEFAULT", "0") in ("1", "true", "True")
CAPTION_ENABLED        = os.getenv("CAPTION_ENABLED", "1") in ("1", "true", "True")
STRICT_NO_HEURISTIC    = os.getenv("STRICT_NO_HEURISTIC", "0") in ("1","true","True")
_Q_MIN_CHARS = 30

_JSON_KEYS = ["위치", "현상", "문제점", "위험성", "요청사항"]

DEFAULT_META = {
    "org": "서산시청",
    "receiver": "서산시청장 귀하",
    "title_prefix": "[서산시]",
}

def _post_json_with_retry(url, headers=None, payload=None, timeout=None, retries=None, backoff=None):
    if timeout is None:
        timeout = (OLLAMA_CONNECT_TIMEOUT, OLLAMA_READ_TIMEOUT)
    if retries is None:
        retries = OLLAMA_RETRIES
    if backoff is None:
        backoff = OLLAMA_BACKOFF
    last = None
    for i in range(retries + 1):
        try:
            r = requests.post(url, headers=headers, json=payload, timeout=timeout)
            r.raise_for_status()
            return r
        except Exception as e:
            last = e
            if i < retries:
                time.sleep(backoff ** i)
            else:
                raise last

def _normalize(text: str) -> str:
    t = (text or "").strip()
    t = re.sub(r"\[[^\]]*\]", " ", t)
    t = re.sub(r"\s+", " ", t)
    return t.strip()

def _sent_split(text: str) -> List[str]:
    t = _normalize(text)
    if not t:
        return []
    if USE_KSS:
        try:
            kss = importlib.import_module("kss")
            return [s.strip() for s in kss.split_sentences(t) if s.strip()]
        except Exception:
            pass
    tmp = re.sub(r"(다\.|요\.|니다\.|습니다\.|[.!?])\s*", r"\1<eos>", t)
    return [s.strip() for s in tmp.split("<eos>") if s.strip()]

def _ko_cleanup_noise(s: str) -> str:
    s = (s or "").strip()
    if not s:
        return s
    s = re.sub(r"(.)\1{2,}", r"\1\1", s)
    s = re.sub(r"\b(\S+)(\s+\1){2,}\b", r"\1", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s

def _postprocess(s: str, keep_sentences: int = 1, max_chars: int = 200) -> str:
    s = (s or "").strip()
    if not s:
        return s
    s = _ko_cleanup_noise(s)
    tmp = re.sub(r"(다\.|요\.|니다\.|습니다\.|[.!?])\s*", r"\1<eos>", s)
    parts = [p.strip() for p in tmp.split("<eos>") if p.strip()]
    s = " ".join(parts[:keep_sentences]) if keep_sentences > 0 else " ".join(parts)
    if len(s) > max_chars:
        s = s[:max_chars].rstrip() + "…"
    if s and not s.endswith(("다.", "요.", "니다.", "습니다.", ".", "…")):
        s += "."
    return s

def _cap_input(t: str, limit=HARD_INPUT_LIMIT) -> str:
    t = _normalize(t)
    if len(t) <= limit:
        return t
    sents = _sent_split(t)
    out, total = [], 0
    for s in sents:
        n = len(s)
        if total + n > limit:
            break
        out.append(s)
        total += n
    return " ".join(out)

def _load_image(image_item: ImageInput):
    try:
        io = importlib.import_module("io")
        PIL = importlib.import_module("PIL.Image")
        requests_mod = importlib.import_module("requests")
        if image_item.url:
            resp = requests_mod.get(image_item.url, timeout=20)
            resp.raise_for_status()
            return PIL.open(io.BytesIO(resp.content)).convert("RGB")
        if image_item.base64:
            b64 = image_item.base64
            if b64.startswith("data:"):
                b64 = b64.split(",", 1)[-1]
            base64_mod = importlib.import_module("base64")
            raw = base64_mod.b64decode(b64)
            return PIL.open(io.BytesIO(raw)).convert("RGB")
    except Exception:
        return None
    return None

_state = {
    "cap_processor": None,
    "cap_model": None,
    "translator_tok": None,
    "translator_model": None,
    "translator_ready": False,
    "device": None,
}

def _device():
    if _state["device"] is not None:
        return _state["device"]
    try:
        torch = importlib.import_module("torch")
        _state["device"] = "cuda" if torch.cuda.is_available() else "cpu"
    except Exception:
        _state["device"] = "cpu"
    return _state["device"]

def _ensure_caption():
    if _state["cap_model"] is not None:
        return True
    try:
        transformers = importlib.import_module("transformers")
        tok = transformers.BlipProcessor.from_pretrained("Salesforce/blip-image-captioning-base")
        model = transformers.BlipForConditionalGeneration.from_pretrained(
            "Salesforce/blip-image-captioning-base"
        ).to(_device())
        _state["cap_processor"], _state["cap_model"] = tok, model
        return True
    except Exception:
        return False

def _blip_caption(pil_image) -> str:
    if not _ensure_caption():
        return ""
    try:
        torch = importlib.import_module("torch")
        proc, model = _state["cap_processor"], _state["cap_model"]
        inputs = proc(images=pil_image, return_tensors="pt").to(_device())
        with torch.no_grad():
            out = model.generate(**inputs, max_new_tokens=40)
        txt = proc.decode(out[0], skip_special_tokens=True).strip()
        return txt
    except Exception:
        return ""

def _ensure_translator():
    if _state["translator_ready"]:
        return True
    try:
        transformers = importlib.import_module("transformers")
        tok = transformers.MarianTokenizer.from_pretrained("Helsinki-NLP/opus-mt-en-ko")
        model = transformers.MarianMTModel.from_pretrained("Helsinki-NLP/opus-mt-en-ko").to(_device())
        _state["translator_tok"], _state["translator_model"] = tok, model
        _state["translator_ready"] = True
        return True
    except Exception:
        _state["translator_ready"] = False
        return False

def _translate_en2ko(text: str) -> str:
    t = (text or "").strip()
    if not t:
        return ""
    if not _ensure_translator():
        return t
    try:
        torch = importlib.import_module("torch")
        tok, model = _state["translator_tok"], _state["translator_model"]
        batch = tok(t, return_tensors="pt", padding=True).to(_device())
        with torch.no_grad():
            gen = model.generate(**batch, max_length=192, num_beams=4)
        return tok.batch_decode(gen, skip_special_tokens=True)[0]
    except Exception:
        return t

def _get_captions(images: List[ImageInput]) -> List[str]:
    if not CAPTION_ENABLED:
        return []
    caps = []
    for it in images[:3]:
        img = _load_image(it)
        if img is None:
            continue
        en = _blip_caption(img)
        ko = _translate_en2ko(en) if en else ""
        if ko:
            caps.append(ko)
    return caps

def _compose_input(complaint_text: str, captions: List[str]) -> str:
    parts = []
    if complaint_text.strip():
        parts.append(complaint_text.strip())
    if captions:
        parts.append("사진 설명: " + " ".join(captions))
    return " ".join(parts).strip() or "내용 없음"

def _extract_json_object(text: str) -> Optional[dict]:
    if not text:
        return None
    text = text.strip()
    text = re.sub(r"^```(?:json)?|```$", "", text, flags=re.IGNORECASE | re.MULTILINE).strip()
    m = re.search(r"\{[\s\S]*\}", text)
    if not m:
        return None
    frag = m.group(0)
    try:
        return json.loads(frag)
    except Exception:
        try:
            frag2 = re.sub(r"[\u201C\u201D]", '"', frag)
            return json.loads(frag2)
        except Exception:
            return None

def _ollama_available() -> bool:
    try:
        t0 = time.time()
        r = requests.get(f"{OLLAMA_BASE.rstrip('/')}/api/tags", timeout=5)
        ok = (r.status_code == 200)
        print(f"[ollama] available={ok} elapsed={time.time()-t0:.1f}s base={OLLAMA_BASE}")
        return ok
    except Exception as e:
        print(f"[ollama] unavailable: {e} base={OLLAMA_BASE}")
        return False

def _empty_fields() -> ComplaintFields:
    return ComplaintFields(**{k: "" for k in _JSON_KEYS})

def _enforce_sentence(key: str, val: str) -> str:
    s = _ko_cleanup_noise(_normalize(val))[:80]
    if not s:
        return ""
    if re.search(r"(다\.|요\.|니다\.|습니다\.|[.!?…])$", s):
        return s
    if key == "문제점":
        return f"{s}이 발생했습니다."
    if key == "위험성":
        return f"{s} 위험이 있습니다."
    if key == "요청사항":
        return f"{s}이 필요합니다."
    if key == "위치":
        return f"{s}에서 발생했습니다."
    if key == "현상":
        return f"{s}입니다."
    return f"{s}입니다."

def _map_keys(obj: dict) -> dict:
    if not isinstance(obj, dict):
        return {}
    m = {
        "위치":"위치","현상":"현상","문제점":"문제점","위험성":"위험성","요청사항":"요청사항",
        "location":"위치","phenomenon":"현상","problem":"문제점","risk":"위험성","request":"요청사항",
        "loc":"위치","issue":"문제점","hazard":"위험성","ask":"요청사항"
    }
    out = {}
    for k, v in obj.items():
        kk = m.get(str(k).strip())
        if kk:
            out[kk] = v
    return out

def _ollama_structured_once(content: str,
                            model: str,
                            read_timeout: int,
                            num_ctx: int,
                            num_predict: int) -> Optional[ComplaintFields]:
    system_msg = (
        "너는 한국어 민원 분석기다. 입력(민원 텍스트와 사진 설명)을 분석해 "
        "위치, 현상, 문제점, 위험성, 요청사항을 추출한다. 출력은 JSON만 반환한다.\n"
        "스타일 가이드:\n"
        "- 각 값은 1문장 이내, 20~80자 권장, 자연스러운 종결형(‘~합니다/입니다’).\n"
        "- 각 항목은 서로 다른 내용을 담고 중복 표현 금지.\n"
        "- 문제점은 불편·불안 요소를, 위험성은 안전상의 구체적 위험만 기술.\n"
        "- ‘위험이 있습니다’ 표현은 위험성 항목에서만 1회 허용.\n"
        "- 불필요한 반복·군더더기 제거.\n"
        "- 비문 금지(예: ‘있어요이’, ‘발생이 발생했습니다’, ‘있음입니다’ 등).\n"
        "- 요청사항은 반드시 ‘요청합니다’ 또는 ‘부탁드립니다’로 끝맺음.\n"
        "- 사진이 있으면 텍스트와 충돌하지 않는 범위에서 단서 1~2개 반영.\n"
        "- 사실 불명확하면 과장 금지. 모르면 빈 문자열, 단 ‘위치’만은 반드시 채움.\n"
        "- ‘위치’가 불명확하면 ‘장소 불명’으로 기입.\n"
        "- 출력은 JSON만, 키는 한국어로 고정: 위치, 현상, 문제점, 위험성, 요청사항.\n"
    )
    user_msg = (
        "아래 민원 내용을 읽고 위치, 현상, 문제점, 위험성, 요청사항을 JSON으로만 응답해.\n"
        "요구사항:\n"
        "1) 각 값은 1문장, 20~80자, ‘~합니다/입니다’로 끝맺음.\n"
        "2) 항목 간 중복 금지. 동일 문구 반복 금지.\n"
        "3) 문제점은 불편·불안 요소, 위험성은 안전상의 구체적 위험만 기술.\n"
        "4) ‘위험이 있습니다’ 표현은 위험성에서만 최대 1회 사용.\n"
        "5) 어색한 표현(예: ‘있어요이’, ‘발생이 발생했습니다’, ‘있음입니다’, ‘요청이 필요합니다’) 금지.\n"
        "6) 요청사항은 반드시 ‘요청합니다’ 또는 ‘부탁드립니다’로 끝맺음.\n"
        "7) 불확실하면 빈 문자열. 단, ‘위치’는 반드시 채움(없으면 ‘장소 불명’).\n"
        "8) 출력은 반드시 JSON만. 형식 고정:\n"
        '{ "위치": "", "현상": "", "문제점": "", "위험성": "", "요청사항": "" }\n'
        "---- 입력 시작 ----\n"
        f"{content}\n"
        "---- 입력 끝 ----"
    )
    payload_chat = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_msg},
            {"role": "user", "content": user_msg},
        ],
        "options": {
            "temperature": 0.1,
            "repeat_penalty": 1.2,
            "num_ctx": num_ctx,
            "num_predict": num_predict,
        },
        "format": "json",
        "stream": False,
    }
    try:
        r = _post_json_with_retry(
            f"{OLLAMA_BASE.rstrip('/')}/api/chat",
            payload=payload_chat,
            timeout=(OLLAMA_CONNECT_TIMEOUT, read_timeout),
            retries=OLLAMA_RETRIES,
            backoff=OLLAMA_BACKOFF,
        )
        j = r.json()
        txt = (
                j.get("message", {}).get("content")
                or j.get("response", "")
                or j.get("output", "")
                or (j.get("choices", [{}])[0].get("message", {}).get("content") if j.get("choices") else "")
                or ""
        ).strip()
        obj = _extract_json_object(txt)
        if not isinstance(obj, dict):
            return None
        obj = _map_keys(obj)
        out = {}
        for k in _JSON_KEYS:
            v = obj.get(k, "")
            if not isinstance(v, str):
                v = "" if v is None else str(v)
            v = _enforce_sentence(k, v)
            out[k] = v
        return ComplaintFields(**out)
    except Exception:
        try:
            prompt = (
                "다음 입력(민원 텍스트 + 사진 설명)을 읽고 위치, 현상, 문제점, 위험성, 요청사항을 JSON으로 채워라.\n"
                "규칙:\n"
                "1) 각 값은 1문장, 20~70자, 반드시 격식체(‘~합니다/입니다’)로 끝맺음. 구어체(‘있어요’) 사용 금지.\n"
                "2) 중복·군더더기 금지. 같은 의미 반복하지 말 것.\n"
                "   예: '발생이 발생했습니다' → '발생했습니다'.\n"
                "   예: '위험이 있음 위험이 있습니다' → '위험이 있습니다'.\n"
                "3) 비문·어색한 표현 금지 (‘~있음입니다’, ‘요청이 필요합니다’, ‘있어요이’ → ‘요청합니다’, ‘있습니다’ 사용).\n"
                "4) 과장 표현(매우, 큰 위험 등) 최소화. 사실만 간결히 기술.\n"
                "5) 요청사항은 반드시 ‘요청합니다’ 또는 ‘부탁드립니다’로 끝맺음.\n"
                "6) 불확실하면 빈 문자열로 두되, ‘위치’는 반드시 채움(없으면 ‘장소 불명’).\n"
                "7) 출력은 반드시 JSON만. 아래 형식을 그대로 사용:\n"
                '{"위치":"","현상":"","문제점":"","위험성":"","요청사항":""}\n'
                "----- 입력 시작 -----\n"
                f"{content}\n"
                "----- 입력 끝 -----\n"
            )
            payload_gen = {
                "model": model,
                "prompt": prompt,
                "options": {
                    "temperature": 0.1,
                    "repeat_penalty": 1.2,
                    "num_ctx": num_ctx,
                    "num_predict": num_predict,
                },
                "format": "json",
                "stream": False,
            }
            r = _post_json_with_retry(
                f"{OLLAMA_BASE.rstrip('/')}/api/generate",
                payload=payload_gen,
                timeout=(OLLAMA_CONNECT_TIMEOUT, read_timeout),
                retries=OLLAMA_RETRIES,
                backoff=OLLAMA_BACKOFF,
            )
            j = r.json()
            txt = (j.get("response") or "").strip()
            obj = _extract_json_object(txt)
            if not isinstance(obj, dict):
                return None
            obj = _map_keys(obj)
            out = {}
            for k in _JSON_KEYS:
                v = obj.get(k, "")
                if not isinstance(v, str):
                    v = "" if v is None else str(v)
                v = _enforce_sentence(k, v)
                out[k] = v
            return ComplaintFields(**out)
        except Exception:
            return None

def _remaining(deadline: Optional[float]) -> float:
    return max(0.0, deadline - time.time()) if deadline else 1e9

def _ollama_structured_with_fallback(joined: str, deadline: Optional[float] = None) -> ComplaintFields:
    def eff_read_timeout(rem: float) -> int:
        hard_cap = max(5, int(rem) - 2)
        return max(5, min(OLLAMA_READ_TIMEOUT, hard_cap))
    def eff_num_predict(rem: float) -> int:
        return FALLBACK_NUM_PREDICT if rem < 45 else OLLAMA_NUM_PREDICT
    rem = _remaining(deadline)
    if rem < 8:
        if STRICT_NO_HEURISTIC:
            raise HTTPException(status_code=502, detail="llm-unavailable")
        return _heuristic_extract_fields(joined)
    device = _device()
    try_primary = (device == "cuda" and rem >= 45)
    t0 = time.time()
    if try_primary:
        fields = _ollama_structured_once(
            content=joined,
            model=OLLAMA_MODEL,
            read_timeout=eff_read_timeout(rem),
            num_ctx=OLLAMA_NUM_CTX,
            num_predict=eff_num_predict(rem),
        )
        if fields:
            print(f"[summarize] primary model success in {time.time()-t0:.1f}s")
            return fields
    rem = _remaining(deadline)
    if rem < 8:
        if STRICT_NO_HEURISTIC:
            raise HTTPException(status_code=502, detail="llm-unavailable")
        return _heuristic_extract_fields(joined)
    fast_joined = _cap_input(joined, FALLBACK_INPUT_LIMIT)
    fields = _ollama_structured_once(
        content=fast_joined,
        model=OLLAMA_ALT_MODEL,
        read_timeout=eff_read_timeout(rem),
        num_ctx=min(FALLBACK_CTX, OLLAMA_NUM_CTX),
        num_predict=min(FALLBACK_NUM_PREDICT, eff_num_predict(rem)),
    )
    if fields:
        print(f"[summarize] alt model success in {time.time()-t0:.1f}s")
        return fields
    print("[summarize] all LLM attempts failed")
    if STRICT_NO_HEURISTIC:
        raise HTTPException(status_code=502, detail="llm-unavailable")
    print("[summarize] falling back to heuristic")
    return _heuristic_extract_fields(joined)

_ADDR_PAT = re.compile(r"(?:[가-힣A-Za-z0-9]+(?:시|군|구|읍|면|동|리)|[가-힣A-Za-z0-9]+(?:로|길)\s?\d*(?:-\d+)?)")
_REQ_PAT  = re.compile(r"(조치|정비|수리|보수|교체|정리|단속|처리|확인)\S*|해\s*주세요|요청|바랍니다")
_RISK_PAT = re.compile(r"(위험|사고|미끄|감전|화재|추락|파손 심함|야간\s*어두움?)")
_PROB_PAT = re.compile(r"(파손|고장|막힘|싱크홀|파열|누수|불법\s?주정차|악취|소음|불량|방치|불편)")

def _find_first(pat: re.Pattern, text: str) -> str:
    m = pat.search(text)
    return m.group(0) if m else ""

def _heuristic_extract_fields(content: str) -> ComplaintFields:
    t = _normalize(content)
    sents = _sent_split(t)
    first = sents[0] if sents else t
    위치 = _find_first(_ADDR_PAT, t)
    문제점 = _find_first(_PROB_PAT, t)
    위험성 = _find_first(_RISK_PAT, t)
    요청사항 = _find_first(_REQ_PAT, t)
    현상 = first if first else ""
    def wrap(k, x): return _enforce_sentence(k, x)
    return ComplaintFields(
        위치=wrap("위치", 위치),
        현상=wrap("현상", 현상),
        문제점=wrap("문제점", 문제점),
        위험성=wrap("위험성", 위험성),
        요청사항=wrap("요청사항", 요청사항),
    )

def _format_bullets(fields: ComplaintFields) -> str:
    def val(s):
        s = (s or "").strip()
        return s if s else "미상"
    lines = [
        f"• 위치: {val(fields.위치)}",
        f"• 현상: {val(fields.현상)}",
        f"• 문제점: {val(fields.문제점)}",
        f"• 위험성: {val(fields.위험성)}",
        f"• 요청사항: {val(fields.요청사항)}",
    ]
    return "\n".join(lines)

def _json_to_request(data: dict) -> SummarizeRequest:
    if not isinstance(data, dict):
        return SummarizeRequest()
    ct = (data.get("complaint_text") or "").strip()
    raw_imgs = data.get("images") or []
    norm_imgs: List[ImageInput] = []
    if isinstance(raw_imgs, list):
        for it in raw_imgs:
            if isinstance(it, str):
                norm_imgs.append(ImageInput(url=it))
            elif isinstance(it, dict):
                u = it.get("url")
                b = it.get("base64")
                if u or b:
                    norm_imgs.append(ImageInput(url=u, base64=b))
    meta = data.get("meta") or {}
    if not isinstance(meta, dict):
        meta = {}
    return SummarizeRequest(complaint_text=ct, images=norm_imgs, meta=meta)

def _wants_json(request: Request, req_meta: Dict[str, Any]) -> bool:
    if request.query_params.get("format", "").lower() == "json":
        return True
    accept = (request.headers.get("accept") or "").lower()
    if "application/json" in accept:
        return True
    if isinstance(req_meta, dict) and (req_meta.get("response") == "json"):
        return True
    return False

def _fields_to_dict(fields: ComplaintFields, lang: str = "ko") -> dict:
    lang = (lang or "ko").lower()
    if lang in ("en", "eng", "english"):
        return {
            "location": fields.위치 or "",
            "phenomenon": fields.현상 or "",
            "problem": fields.문제점 or "",
            "risk": fields.위험성 or "",
            "request": fields.요청사항 or "",
        }
    else:
        return {
            "위치": fields.위치 or "",
            "현상": fields.현상 or "",
            "문제점": fields.문제점 or "",
            "위험성": fields.위험성 or "",
            "요청사항": fields.요청사항 or "",
        }
def _is_blank(v: str) -> bool:
    return (v is None) or (str(v).strip() == "") or (str(v).strip() == "미상")

def _merge_kofields(base: dict, fill: dict) -> dict:
    keys = ["위치", "현상", "문제점", "위험성", "요청사항"]
    merged = {}
    for k in keys:
        bv = (getattr(base, k, None) if hasattr(base, k) else base.get(k))
        fv = (getattr(fill, k, None) if hasattr(fill, k) else fill.get(k))
        merged[k] = (bv if not _is_blank(bv) else (fv or "")) or ""
    return merged

def _as_kofields(obj) -> ComplaintFields:
    # dict 또는 ComplaintFields 모두 수용
    if isinstance(obj, ComplaintFields):
        return obj
    return ComplaintFields(**obj)

@app.get("/health")
def healthcheck():
    return {"ok": True}

@app.post("/summarize_json")
async def summarize_json(req: SummarizeRequest):
    text = (req.complaint_text or "").strip()
    if not text and not req.images:
        raise HTTPException(status_code=400, detail="complaint_text or images is required")
    deadline = time.time() + max(5, HANDLER_TIMEOUT_SECS - 1)
    mode = (req.meta or {}).get("mode") or ""
    t0 = time.time()
    with fail_after(HANDLER_TIMEOUT_SECS):
        fields = await anyio.to_thread.run_sync(_run, req, deadline, mode)
    elapsed = time.time() - t0
    route = "heuristic" if not _ollama_available() else "llm_or_mixed"
    print(f"[summarize_json] route={route} mode={mode or 'auto'} input_len={len(text)} elapsed={elapsed:.1f}s")
    keys = (req.meta or {}).get("keys") or "ko"
    return _fields_to_dict(fields, keys)

@app.post("/summarize")
async def summarize(request: Request):
    try:
        ct_header = (request.headers.get("content-type") or "").lower()
        ignore_q = (request.query_params.get("ignore_images") or "").lower() in ("1", "true")
        if "application/json" in ct_header:
            data = await request.json()
            req = _json_to_request(data if isinstance(data, dict) else {})
            if ignore_q:
                req.meta["ignore_images"] = True
        elif "multipart/form-data" in ct_header:
            form = await request.form()
            complaint_text = (form.get("complaint_text") or "").strip()
            want_images = CAPTION_ENABLED and not (ignore_q or IGNORE_IMAGES_DEFAULT)
            img_inputs: List[ImageInput] = []
            if want_images:
                for f in form.getlist("images"):
                    try:
                        if hasattr(f, "read"):
                            data = await f.read()
                            mime = getattr(f, "content_type", None) or "image/jpeg"
                            b64 = base64.b64encode(data).decode("utf-8")
                            img_inputs.append(ImageInput(base64=f"data:{mime};base64,{b64}"))
                    except Exception:
                        continue
            req = SummarizeRequest(
                complaint_text=complaint_text,
                images=img_inputs,
                meta={"ignore_images": not want_images or ignore_q}
            )
        else:
            raw = await request.body()
            req = SummarizeRequest(complaint_text=raw.decode("utf-8", "ignore"), images=[], meta={})
            if ignore_q:
                req.meta["ignore_images"] = True
        deadline = time.time() + max(5, HANDLER_TIMEOUT_SECS - 1)
        t0 = time.time()
        with fail_after(HANDLER_TIMEOUT_SECS):
            fields = await anyio.to_thread.run_sync(_run, req, deadline)
        elapsed = time.time() - t0
        print(f"[summarize] elapsed={elapsed:.1f}s")
        wants_json = _wants_json(request, req.meta)
        lang_key = (request.query_params.get("keys") or (req.meta or {}).get("keys") or "ko")
        if wants_json:
            return _fields_to_dict(fields, lang_key)
        else:
            bullets = _format_bullets(fields)
            return PlainTextResponse(bullets)
    except AnyioTimeoutError:
        raise HTTPException(status_code=504, detail="server-timeout: summarize exceeded handler deadline")
    except HTTPException:
        raise
    except Exception as e:
        import traceback, sys
        traceback.print_exc(file=sys.stderr)
        raise HTTPException(status_code=500, detail=f"server-error: {type(e).__name__}: {e}")

@app.post("/ai/minwon/extract", response_model=ComplaintFields, tags=["ai-minwon"])
def api_extract(req: SummarizeRequest):
    try:
        return run_extract(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"extract failed: {e}")

@app.post("/ai/minwon/compose", response_model=ComposeOut, tags=["ai-minwon"])
def api_compose(body: ComposeIn):
    try:
        meta = {**DEFAULT_META, **(body.meta or {})}

        has_fields = bool(body.fields)
        has_text   = bool((body.complaint_text or "").strip())
        has_images = bool(body.images)
        if not (has_fields or has_text or has_images):
            raise HTTPException(status_code=400, detail="complaint_text / images / fields 중 하나는 필요합니다.")

        complaint_text = body.complaint_text or ""
        images = body.images or []

        #  필드 확보
        if has_fields:
            base_fields = _as_kofields(body.fields)
            req = SummarizeRequest(
                complaint_text=complaint_text,
                images=images,
                meta={**meta, "ignore_images": False},
            )
            extracted = _as_kofields(run_extract(req))
            merged = _merge_kofields(
                base_fields.__dict__, extracted.__dict__
            )
            fields = _as_kofields(merged)
        else:
            req = SummarizeRequest(
                complaint_text=complaint_text,
                images=images,
                meta={**meta, "ignore_images": False},
            )
            fields = _as_kofields(run_extract(req))

        # 문서 조립
        result = compose_document(fields, meta, include_html=False)
        title = (result.get("title") or f"{meta.get('title_prefix','')} 민원 신청의 건").strip()

        # 반환
        return ComposeOut(
            title=title,
            body=result.get("body", ""),
            fields=fields,
        )

    except HTTPException:
        raise
    except Exception as e:
        import traceback, sys
        print("compose error:", e, file=sys.stderr)
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"compose failed: {e}")

if __name__ == "__main__":
    import uvicorn
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "8000"))
    reload_flag = os.getenv("RELOAD", "0") in ("1", "true", "True")
    uvicorn.run(app, host=host, port=port, reload=reload_flag)