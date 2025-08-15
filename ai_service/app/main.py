from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Request
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import os, re, base64, importlib, requests, time, json

app = FastAPI(title="Seosan AI Service (Ollama Summarizer)")

# ===== 환경 변수(없으면 넉넉한 기본값) =====
OLLAMA_BASE = os.getenv("OLLAMA_BASE", "http://127.0.0.1:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")
# 경량 폴백 모델(미설치면 자동 휴리스틱으로 폴백)
OLLAMA_ALT_MODEL = os.getenv("OLLAMA_ALT_MODEL", "qwen2.5:3b-instruct")
USE_KSS = os.getenv("USE_KSS", "0") in ("1", "true", "True")

# 타임아웃/재시도: 크게 설정 (필요 시 환경변수로 조절)
OLLAMA_CONNECT_TIMEOUT = int(os.getenv("OLLAMA_CONNECT_TIMEOUT", "60"))   # 연결 대기
OLLAMA_READ_TIMEOUT    = int(os.getenv("OLLAMA_READ_TIMEOUT", "600"))     # 응답 대기
OLLAMA_RETRIES         = int(os.getenv("OLLAMA_RETRIES", "3"))            # 재시도 횟수
OLLAMA_BACKOFF         = float(os.getenv("OLLAMA_BACKOFF", "2.0"))        # 지수 백오프 배수
OLLAMA_NUM_PREDICT     = int(os.getenv("OLLAMA_NUM_PREDICT", "400"))      # 출력 토큰 상한
OLLAMA_NUM_CTX         = int(os.getenv("OLLAMA_NUM_CTX", "4096"))         # 컨텍스트 크기

# 폴백 단계에서 더 공격적으로 줄일 값
FALLBACK_NUM_PREDICT   = int(os.getenv("FALLBACK_NUM_PREDICT", "250"))
FALLBACK_CTX           = int(os.getenv("FALLBACK_CTX", "3072"))
FALLBACK_READ_TIMEOUT  = int(os.getenv("FALLBACK_READ_TIMEOUT", "240"))    # 2차 시도 읽기 타임아웃
HARD_INPUT_LIMIT       = int(os.getenv("HARD_INPUT_LIMIT", "2200"))        # 1차 입력 컷
FALLBACK_INPUT_LIMIT   = int(os.getenv("FALLBACK_INPUT_LIMIT", "1200"))    # 2차 입력 컷(더 짧게)

# ===== 모델 I/O 스키마 =====
class ImageInput(BaseModel):
    url: Optional[str] = None
    base64: Optional[str] = None

class SummarizeRequest(BaseModel):
    complaint_text: str = ""
    images: List[ImageInput] = Field(default_factory=list)
    meta: Dict[str, Any] = Field(default_factory=dict)

# 내부 사용: 5개 항목
class ComplaintFields(BaseModel):
    위치: str = ""
    현상: str = ""
    문제점: str = ""
    위험성: str = ""
    요청사항: str = ""

_JSON_KEYS = ["위치", "현상", "문제점", "위험성", "요청사항"]

# ===== 공통 유틸 (주석 최소) =====
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

# ======================================================================
# 🧠 (A) 텍스트 인식/전처리 핵심 로직 — 매우 자세한 설명 주석
# ======================================================================
def _cap_input(t: str, limit=HARD_INPUT_LIMIT) -> str:
    """
    [텍스트 인식(입력 전처리) — LLM 컨텍스트 보호 + 의미 단위 유지]

    왜 필요한가?
    - 민원 본문이 길어질수록 LLM은 앞부분 문맥을 선호하고, 전체 길이가 컨텍스트 한계를 넘으면
      중요한 정보를 놓치거나 응답이 불안정해질 수 있습니다.
    - 단순 문자 수 자르기는 문장 중간을 끊어 의미 손실을 유발합니다.

    무엇을 하는가?
    1) _normalize()로 불필요한 공백/대괄호 주석 등을 제거해 토큰 낭비를 줄입니다.
    2) 전체 길이가 limit 이하이면 그대로 반환 → 불필요한 재분절 방지로 속도/품질 유지.
    3) 초과하면 _sent_split()으로 "문장 단위"로 분리하여 앞에서부터 누적해 limit 직전까지 포함.
       - 이렇게 하면 문맥 경계(문장)를 보존하면서 "정보 밀도 높은 앞부분"을 우선 공급합니다.

    결과:
    - LLM이 읽기 좋은 "문장 경계 기반 요약 입력"을 생성 → 추출 정확도/일관성↑, 타임아웃↓
    """
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

# ======================================================================
# 🖼️ (B) 이미지 인식 파이프라인 — 매우 자세한 설명 주석
# ======================================================================
def _load_image(image_item: ImageInput):
    """
    [이미지 로딩(저수준) — URL/base64 → PIL.Image(RGB)]

    입력 형식:
      - image_item.url: http(s) 이미지 경로(S3/CDN/정적 서버 등)
      - image_item.base64: 'data:image/png;base64,...' 또는 순수 base64 페이로드

    설계 원칙:
      1) 모든 이미지를 RGB 3채널로 통일(convert('RGB')):
         - 다양한 포맷(P, LA, RGBA 등)로 인한 전처리/모델 호환 문제 예방.
      2) 네트워크/디코딩 실패가 전체 요청 실패로 번지는 것을 차단:
         - 각 이미지별 예외는 여기서 흡수하고 None 반환 → 상위에서 해당 이미지만 skip.
      3) 보안/안정성:
         - 외부 URL은 타임아웃(기본 10~20초)을 걸어 비정상 응답에 빠르게 탈출.
         - base64는 data URL 헤더 제거 후 디코드, 손상 시 예외 → None.

    처리 흐름:
      (a) URL이 있으면 requests.get(url, timeout=20) → BytesIO → PIL.Image.open → RGB 변환
      (b) base64가 있으면 data 헤더 제거 → base64.b64decode → BytesIO → PIL → RGB
      (c) 둘 다 실패/없으면 None 반환
    """
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
                # 'data:image/png;base64,AAAA...' 접두 제거
                b64 = b64.split(",", 1)[-1]
            base64_mod = importlib.import_module("base64")
            raw = base64_mod.b64decode(b64)
            return PIL.open(io.BytesIO(raw)).convert("RGB")
    except Exception:
        # 개별 이미지 실패는 전체 파이프라인을 멈추지 않음
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
    # BLIP 캡셔닝 모델/프로세서 1회 로드(캐시)
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
    """
    [시맨틱 캡셔닝(고수준) — BLIP로 '이미지 → 영어 문장' 생성]

    목적:
      - 사진의 핵심 객체/상황을 한두 문장 영어로 설명 → 이후 한국어 번역/텍스트 결합에 사용.
      - 본문 텍스트가 빈약해도 이미지가 '증거 신호' 역할을 하여 추출 정확도 보강.

    핵심 단계:
      1) _ensure_caption()으로 Processor/Model을 1회 로드(메모리 캐시).
      2) Processor로 전처리 텐서 생성(proc(images=..., return_tensors="pt")).
      3) torch.no_grad() 하에서 model.generate 실행 (max_new_tokens=40로 장문/헛소리 억제).
      4) proc.decode(..., skip_special_tokens=True)로 텍스트화 → 깔끔한 영어 캡션.

    성능/안정성:
      - GPU 있으면 크게 가속, 없으면 CPU로도 동작(느릴 수 있으므로 이미지 수를 제한).
      - 예외 발생 시 빈 문자열 반환하여 전체 요청은 계속 진행.
    """
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
    # MarianMT EN→KO 번역 모델 1회 로드(캐시)
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
    """
    [캡션 번역 — 영어 → 한국어]

    왜 번역하는가?
      - 최종 요약/추출 프롬프트는 한국어 중심으로 설계되어 있으며,
        모델이 한 언어로 일관된 문맥을 볼수록 정확도가 올라갑니다.

    어떻게 하는가?
      1) MarianMT(EN→KO)를 준비(최초 1회 로드 후 캐시).
      2) 토큰화 → generate(num_beams=4, max_length=192)로 안정성 확보.
      3) special tokens 제거 후 한국어 캡션 반환.

    실패 시:
      - 번역 모델 미사용/오류면 원문(영어) 그대로 반환 → 파이프라인 지속.
    """
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
    """
    [이미지 파이프라인 오케스트레이션 — 다장 처리/번역/정제]

    입력:
      - ImageInput 리스트(URL/base64 혼재 가능)

    동작:
      1) _load_image()로 각 항목을 안전하게 PIL.Image(RGB)로 변환(실패 시 해당 이미지만 skip).
      2) _blip_caption()으로 영어 캡션 생성.
      3) _translate_en2ko()로 한국어로 번역(번역 모델이 불가하면 영어 유지).
      4) 공백이 아닌 결과만 축적.

    성능 팁:
      - CPU 환경에서는 이미지 수가 많을수록 지연↑. 필요 시 상한(예: 3장)으로 제한.
        아래 구현은 과도한 지연 방지를 위해 최대 3장만 사용합니다.
    """
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
    """
    [텍스트+이미지 결합 — 단일 컨텍스트로 통합]

    목적:
      - LLM 추론은 하나의 긴 컨텍스트에서 상호 보완 신호를 볼 때 가장 안정적입니다.
      - 본문 텍스트에 '사진 설명:' 블록을 덧붙여 동일 문맥으로 제공하면,
        누락된 위치/현상 정보가 보강되어 구조화 추출 정확도가 올라갑니다.

    규칙:
      - 본문을 먼저 두고, 이어서 '사진 설명: <캡션1> <캡션2> ...' 형태로 공백 구분 결합.
      - 상대적으로 단순한 규칙(줄바꿈/마크다운 최소화)으로 모델 혼란을 줄입니다.
    """
    parts = []
    if complaint_text.strip():
        parts.append(complaint_text.strip())
    if captions:
        parts.append("사진 설명: " + " ".join(captions))
    return " ".join(parts).strip() or "내용 없음"

# ======================================================================
# (C) 구조화 추출 — 단일 LLM 호출 + 단계적 폴백
# ======================================================================
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
        requests.get(f"{OLLAMA_BASE.rstrip('/')}/api/tags", timeout=2)
        return True
    except Exception:
        return False

def _empty_fields() -> ComplaintFields:
    return ComplaintFields(**{k: "" for k in _JSON_KEYS})

def _ollama_structured_once(content: str,
                            model: str,
                            read_timeout: int,
                            num_ctx: int,
                            num_predict: int) -> Optional[ComplaintFields]:
    """
    단일 모델로 한 번 시도.
    chat + format=json 사용, 미지원/에러 시 generate로 폴백.
    """
    system_msg = (
        "너는 한국어 민원 분석기다. 입력(민원 내용+사진 설명)에서 "
        "위치, 현상, 문제점, 위험성, 요청사항 5가지를 간결히 채워라. "
        "각 값은 0~80자 한국어 문장/구. 모르면 빈 문자열. 출력은 JSON만."
    )
    user_msg = (
        "아래 내용을 읽고 JSON으로만 응답해.\n"
        '{ "위치": "", "현상": "", "문제점": "", "위험성": "", "요청사항": "" }\n'
        "----\n"
        f"{content}\n"
        "----"
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
        out = {}
        for k in _JSON_KEYS:
            v = obj.get(k, "")
            if not isinstance(v, str):
                v = "" if v is None else str(v)
            v = _ko_cleanup_noise(_normalize(v))[:80]
            out[k] = v
        return ComplaintFields(**out)
    except Exception:
        # chat+format이 미지원/오류일 수 있으니 generate로 한 번 더 시도
        try:
            prompt = (
                "너는 한국어 민원 분석기다. 입력(민원 내용+사진 설명)에서 "
                "위치, 현상, 문제점, 위험성, 요청사항 5가지를 간결히 채워라. "
                "각 값은 0~80자 한국어 문장/구. 모르면 빈 문자열만 둬라.\n"
                "출력은 반드시 JSON만. 키는 다음과 같이 고정:\n"
                '{ "위치": "", "현상": "", "문제점": "", "위험성": "", "요청사항": "" }\n'
                "-----\n"
                f"{content}\n"
                "-----\n"
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
            out = {}
            for k in _JSON_KEYS:
                v = obj.get(k, "")
                if not isinstance(v, str):
                    v = "" if v is None else str(v)
                v = _ko_cleanup_noise(_normalize(v))[:80]
                out[k] = v
            return ComplaintFields(**out)
        except Exception:
            return None

def _ollama_structured_with_fallback(joined: str) -> ComplaintFields:
    """
    단계적 폴백:
    1) 주 모델 + 넉넉한 설정
    2) 실패/지연 시 경량 모델 + 더 짧은 입력/출력/타임아웃
    3) 그래도 실패면 휴리스틱 즉시 반환
    """
    # 1차: 주 모델, 넉넉한 입력
    t0 = time.time()
    fields = _ollama_structured_once(
        content=joined,
        model=OLLAMA_MODEL,
        read_timeout=OLLAMA_READ_TIMEOUT,
        num_ctx=OLLAMA_NUM_CTX,
        num_predict=OLLAMA_NUM_PREDICT,
    )
    if fields:
        print(f"[summarize] primary model success in {time.time()-t0:.1f}s")
        return fields

    # 2차: 경량 모델, 더 짧은 입력/출력/타임아웃(빠르게 끝내기)
    print("[summarize] primary failed/timeout -> trying ALT model (fast lane)")
    # 입력 더 줄이기(이미지 무시 + 텍스트 컷은 상위에서 처리)
    fast_joined = _cap_input(joined, FALLBACK_INPUT_LIMIT)
    fields = _ollama_structured_once(
        content=fast_joined,
        model=OLLAMA_ALT_MODEL,
        read_timeout=FALLBACK_READ_TIMEOUT,
        num_ctx=FALLBACK_CTX,
        num_predict=FALLBACK_NUM_PREDICT,
    )
    if fields:
        print(f"[summarize] alt model success in {time.time()-t0:.1f}s")
        return fields

    # 3차: 휴리스틱 즉시
    print("[summarize] all LLM attempts failed -> fallback to heuristic")
    return _heuristic_extract_fields(joined)

# 휴리스틱 백업(LLM 불가 시)
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
    def clean(x): return _ko_cleanup_noise(x)[:80]
    return ComplaintFields(
        위치=clean(위치),
        현상=clean(현상),
        문제점=clean(문제점),
        위험성=clean(위험성),
        요청사항=clean(요청사항),
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

# ===== 파이프라인 결합 =====
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

def _run(req: SummarizeRequest) -> ComplaintFields:
    # 사용자가 meta.ignore_images를 켜면 이미지 무시(속도 우선)
    use_images = not bool(req.meta.get("ignore_images"))
    captions = _get_captions(req.images) if (req.images and use_images) else []
    clean_text = _ko_cleanup_noise(_cap_input(req.complaint_text, HARD_INPUT_LIMIT))
    joined = _compose_input(clean_text, captions)

    if _ollama_available():
        fields = _ollama_structured_with_fallback(joined)
    else:
        fields = _heuristic_extract_fields(joined)

    if not any(getattr(fields, k) for k in _JSON_KEYS):
        fields = _heuristic_extract_fields(joined)
    return fields

# ===== 응답 유틸 =====
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

# ===== 라우팅 =====
@app.get("/healthcheck")
def healthcheck():
    return {"ok": True}

@app.post("/summarize")
async def summarize(request: Request, complaint_text: Optional[str] = Form(None), images: List[UploadFile] = File(None)):
    try:
        ct_header = (request.headers.get("content-type") or "").lower()
        if "application/json" in ct_header:
            data = await request.json()
            req = _json_to_request(data if isinstance(data, dict) else {})
        else:
            img_inputs: List[ImageInput] = []
            if images:
                for f in images:
                    try:
                        data = await f.read()
                        mime = f.content_type or "image/jpeg"
                        b64 = base64.b64encode(data).decode("utf-8")
                        img_inputs.append(ImageInput(base64=f"data:{mime};base64,{b64}"))
                    except Exception:
                        continue
            req = SummarizeRequest(complaint_text=(complaint_text or "").strip(), images=img_inputs)

        t0 = time.time()
        fields = _run(req)
        elapsed = time.time() - t0
        print(f"[summarize] elapsed={elapsed:.1f}s")

        wants_json = _wants_json(request, req.meta)
        lang_key = (request.query_params.get("keys")
                    or (req.meta or {}).get("keys")
                    or "ko")

        if wants_json:
            return _fields_to_dict(fields, lang_key)
        else:
            bullets = _format_bullets(fields)
            return PlainTextResponse(bullets)

    except HTTPException:
        raise
    except Exception as e:
        import traceback, sys
        traceback.print_exc(file=sys.stderr)
        raise HTTPException(status_code=500, detail=f"server-error: {type(e).__name__}: {e}")

if __name__ == "__main__":
    import uvicorn
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "8000"))
    reload_flag = os.getenv("RELOAD", "0") in ("1", "true", "True")
    uvicorn.run("app.main:app", host=host, port=port, reload=reload_flag)
