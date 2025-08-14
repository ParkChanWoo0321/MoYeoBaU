from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Request
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import os, re, base64, importlib, requests, time

app = FastAPI(title="Seosan AI Service (Ollama Summarizer)")

OLLAMA_BASE = os.getenv("OLLAMA_BASE", "http://127.0.0.1:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")
USE_KSS = os.getenv("USE_KSS", "0") in ("1", "true", "True")


class ImageInput(BaseModel):
    url: Optional[str] = None
    base64: Optional[str] = None


class SummarizeRequest(BaseModel):
    complaint_text: str = ""
    images: List[ImageInput] = Field(default_factory=list)
    meta: Dict[str, Any] = Field(default_factory=dict)


class SummarizeResponse(BaseModel):
    summary: str
    category: str
    urgency: str
    evidence: Dict[str, Any] = Field(default_factory=dict)


def _post_json_with_retry(url, headers=None, payload=None, timeout=(8, 120), retries=2, backoff=1.6):
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


def _cap_input(t: str, limit=2200) -> str:
    t = _normalize(t)
    if len(t) <= limit:
        return t
    sents = _sent_split(t)
    out, total = [], 0
    for s in sents:
        total += len(s)
        if total > limit:
            break
        out.append(s)
    return " ".join(out)


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


def _load_image(image_item: ImageInput):
    """
    [목적]
    - 클라이언트가 보낸 이미지 데이터를 실제로 메모리에 로드하여
      후속 AI 추론이 가능한 형태(PIL.Image, RGB)로 반환한다.

    [입력]
    - image_item: ImageInput
        · url:  http(s) 이미지 경로 (선택)
        · base64: 'data:<mime>;base64,<payload>' 형태 또는 순수 base64 문자열 (선택)

    [처리 흐름]
    1) url이 있는 경우:
       - requests.get으로 이미지 바이너리를 받아 BytesIO로 감싸서 PIL.Image로 open
       - .convert("RGB")로 통일 (일부 모드는 모델 전처리에서 문제가 될 수 있어 RGB 강제)
       - 네트워크 실패, 404, 포맷 오류 시 예외 → except에서 None 반환
    2) base64가 있는 경우:
       - 'data:' prefix가 있으면 헤더('data:image/png;base64,') 제거
       - base64.b64decode 후 BytesIO → PIL.Image.open → RGB 변환
       - 디코딩 실패/손상된 데이터 시 예외 → except에서 None 반환
    3) url/base64 모두 없으면 None

    [반환]
    - PIL.Image(RGB) 또는 None (실패 시)

    [주의/운영 팁]
    - 외부 URL은 네트워크 지연/실패 가능 → timeout(10초) 설정
    - 대용량 이미지는 메모리 사용량이 커질 수 있음. 필요 시 리사이즈/압축 추가 고려.
    """
    try:
        io = importlib.import_module("io")
        PIL = importlib.import_module("PIL.Image")
        requests_mod = importlib.import_module("requests")
        if image_item.url:
            # ✅ URL 입력: 네트워크 통해 바이너리 다운로드 후 이미지로 로드
            resp = requests_mod.get(image_item.url, timeout=10)
            resp.raise_for_status()
            return PIL.open(io.BytesIO(resp.content)).convert("RGB")
        if image_item.base64:
            # ✅ Base64 입력: data URL 헤더가 있으면 제거하고 순수 페이로드만 디코딩
            b64 = image_item.base64
            if b64.startswith("data:"):
                # 예: data:image/jpeg;base64,<payload>
                b64 = b64.split(",", 1)[-1]
            base64_mod = importlib.import_module("base64")
            raw = base64_mod.b64decode(b64)  # 손상/잘못된 문자열이면 예외 발생
            return PIL.open(io.BytesIO(raw)).convert("RGB")
    except Exception:
        # 로드 실패 시 상위 로직이 적절히 skip할 수 있도록 None 반환
        return None
    return None


def _blip_caption(pil_image) -> str:
    """
    [목적]
    - 로드된 PIL.Image에 대해 BLIP 이미지 캡셔닝 모델로 영어 설명 문장(캡션)을 생성한다.

    [전제조건/내부 의존]
    - _ensure_caption(): 프로세서/모델을 _state에 로딩 (처음 호출 시 다운로드/메모리 탑재)
        · Processor: transformers.BlipProcessor
        · Model:     transformers.BlipForConditionalGeneration
    - _device(): torch.cuda.is_available()에 따라 'cuda' 또는 'cpu' 선택

    [처리 흐름]
    1) _ensure_caption()으로 모델 준비 확인 (미준비/로드 실패 시 빈 문자열 반환)
    2) 프로세서로 PIL.Image → 텐서 변환 (return_tensors="pt")
    3) 디바이스로 텐서 이동(.to(_device()))
    4) torch.no_grad() 문맥에서 model.generate(**inputs, max_new_tokens=40) 호출
       - max_new_tokens=40: 너무 길지 않은 간결한 캡션 유도 (필요 시 조절)
    5) 생성 토큰을 proc.decode(..., skip_special_tokens=True)로 텍스트화 → strip()

    [반환]
    - str: 영어 캡션 (모델 미준비/실패 시 "")

    [주의/운영 팁]
    - 첫 로딩 시 모델 가중치 다운로드로 인해 초기 지연이 발생할 수 있음.
    - CPU 환경에서는 추론 지연 증가. 빈번한 호출 시 이미지 개수를 제한하거나 캐시 전략 고려.
    """
    if not _ensure_caption():
        return ""
    try:
        torch = importlib.import_module("torch")
        proc, model = _state["cap_processor"], _state["cap_model"]
        # 1) PIL 이미지를 BLIP 전처리기로 PyTorch 텐서 변환
        inputs = proc(images=pil_image, return_tensors="pt").to(_device())
        # 2) 추론: no_grad로 메모리/속도 최적화, 토큰 수 제한으로 과도한 길이 방지
        with torch.no_grad():
            out = model.generate(**inputs, max_new_tokens=40)
        # 3) 토큰 → 문자열 디코딩 (특수 토큰 제거), 좌우 공백 제거
        txt = proc.decode(out[0], skip_special_tokens=True).strip()
        return txt
    except Exception:
        # 모델 추론 중 오류(메모리 부족, 입력 이상 등)는 빈 문자열로 안전하게 처리
        return ""


def _translate_en2ko(text: str) -> str:
    """
    [목적]
    - 영어 캡션을 한국어로 번역한다. (이미지 인식 필수 단계는 아니며, 결과 가독성 향상을 위한 선택 단계)
    - 번역 모델이 준비되지 않았거나 오류가 발생하면 원문(영어)을 그대로 반환해 상위 로직을 막지 않는다.

    [전제조건/내부 의존]
    - _ensure_translator(): MarianTokenizer/MarianMTModel(EN→KO) 로드 및 _state 등록
    - _device(): 'cuda' 또는 'cpu'

    [처리 흐름]
    1) 입력이 비어있으면 "" 반환
    2) 번역 모델 준비(_ensure_translator) 실패 시 원문 그대로 반환
    3) 토크나이즈 → 모델 generate(max_length=192, num_beams=4) → 디코딩

    [반환]
    - str: 한국어 번역 결과(성공 시) 또는 원문(모델 비활성/실패 시)
    """
    t = (text or "").strip()
    if not t:
        return ""
    if not _ensure_translator():
        return t
    try:
        torch = importlib.import_module("torch")
        tok, model = _state["translator_tok"], _state["translator_model"]
        # 1) 토크나이즈 & 텐서화
        batch = tok(t, return_tensors="pt", padding=True).to(_device())
        # 2) 생성 (빔 서치 사용): 품질/속도 균형 (필요 시 파라미터 조정)
        with torch.no_grad():
            gen = model.generate(**batch, max_length=192, num_beams=4)
        # 3) 디코딩 후 첫 결과 사용
        return tok.batch_decode(gen, skip_special_tokens=True)[0]
    except Exception:
        # 번역 중 문제 발생 시 원문 반환 (빈 문자열로 버리지 않음)
        return t


def _get_captions(images: List[ImageInput]) -> List[str]:
    """
    [목적]
    - 입력된 이미지(ImageInput) 목록을 순회하며 최대 3장까지 처리한다.
    - 각 이미지를 메모리(PIL.Image)로 로드한 뒤, BLIP 모델로 영어 캡션을 생성하고
      필요 시 한국어로 번역해 최종 캡션 문자열 목록을 반환한다.

    [입력]
    - images: ImageInput(url 또는 base64 둘 중 하나를 가질 수 있음)의 리스트

    [전제조건/내부 의존]
    - _load_image(): URL/Base64를 실제 PIL.Image(RGB)로 변환
    - _blip_caption(): BLIP(Salesforce/blip-image-captioning-base)로 영어 캡션 생성
    - _translate_en2ko(): 영어 문장을 한국어로 번역(번역 모델 사용 불가 시 원문 유지)

    [처리 흐름]
    1) images를 최대 3장까지만 슬라이스해 순회 (성능/지연시간 보호 목적)
    2) 각 이미지 항목을 _load_image로 열기 (열기 실패 시 해당 이미지는 skip)
    3) 성공적으로 로드된 PIL 이미지에 대해 _blip_caption으로 영어 캡션 생성
    4) 생성된 영어 캡션이 있으면 _translate_en2ko로 한국어 번역 (번역 실패/비활성 시 영어 유지)
    5) 최종적으로 빈 문자열이 아닌 캡션만 리스트에 추가하여 반환

    [반환]
    - List[str]: 각 이미지에 대한(번역된) 캡션 문자열들의 리스트 (빈 리스트 가능)
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


def _ollama_available() -> bool:
    try:
        requests.get(f"{OLLAMA_BASE.rstrip('/')}/api/tags", timeout=2)
        return True
    except Exception:
        return False


def _ollama_chat(prompt: str) -> str:
    try:
        r = _post_json_with_retry(
            f"{OLLAMA_BASE.rstrip('/')}/api/chat",
            payload={
                "model": OLLAMA_MODEL,
                "messages": [
                    {"role": "system", "content": "너는 한국어 민원 한줄요약 도우미다. 문제·위치·영향·요청을 간결하게 담아라. 불필요한 수식어와 반복, 어색한 조사는 금지한다."},
                    {"role": "user", "content": prompt},
                ],
                "options": {"temperature": 0.2, "repeat_penalty": 1.2, "num_ctx": 4096},
                "stream": False,
            },
            timeout=(8, 120),
            retries=2,
        )
        j = r.json()
        txt = (
                j.get("message", {}).get("content")
                or j.get("response", "")
                or j.get("output", "")
                or (j.get("choices", [{}])[0].get("message", {}).get("content") if j.get("choices") else "")
                or ""
        )
        return txt.strip()
    except Exception:
        return ""


def _chunk_text_by_sentences(text: str, max_chars: int = 1200) -> List[str]:
    sents = _sent_split(text)
    chunks, cur, cur_len = [], [], 0
    for s in sents:
        if cur_len + len(s) + 1 > max_chars and cur:
            chunks.append(" ".join(cur))
            cur, cur_len = [s], len(s)
        else:
            cur.append(s)
            cur_len += len(s) + 1
    if cur:
        chunks.append(" ".join(cur))
    return chunks if chunks else ([text] if text else [])


def _ollama_summary(primary_text: str, captions: List[str], keep_sentences: int = 1) -> str:
    content = (primary_text or "").strip()
    if captions:
        content += "\n[사진 설명] " + " ".join(captions)
    if not content:
        return ""
    if len(content) <= 2200:
        txt = _ollama_chat(f"다음을 {keep_sentences}문장(문장당 25~60자)으로 한글 요약:\n---\n{content}\n---\n출력은 요약 문장만.")
        return _postprocess(txt, keep_sentences=keep_sentences)
    chunks = _chunk_text_by_sentences(content, max_chars=1200)
    partials = []
    for ch in chunks:
        t = _ollama_chat(f"다음을 1문장(25~60자)으로 핵심만 요약:\n---\n{ch}\n---\n출력은 요약 문장만.")
        t = _postprocess(t, keep_sentences=1)
        if t:
            partials.append(t)
    merged = " ".join(partials)
    txt = _ollama_chat(f"아래 부분 요약들을 통합해 최종 {keep_sentences}문장(문장당 25~60자)으로 한글 요약:\n---\n{merged}\n---\n출력은 요약 문장만.")
    return _postprocess(txt, keep_sentences=keep_sentences)


def _category(text: str) -> str:
    t = text
    rules = [
        ("불법 ?주정차|주차|차가 막", "불법주정차/교통"),
        ("가로등|조명|어두워|전등|전기|소등", "조명/전력"),
        ("소음|냄새|악취|시끄러|진동", "소음/악취"),
        ("쓰레기|무단 ?투기|청소|폐기물", "쓰레기/환경"),
        ("도로|포장|파손|싱크홀|보도|인도|맨홀|과속방지|울퉁불퉁", "도로/안전"),
        ("누수|배수|물 고임", "상하수도"),
    ]
    for pat, lab in rules:
        if re.search(pat, t):
            return lab
    return "기타"


def _urgency(text: str) -> str:
    if re.search("화재|감전|추락|사고|다침|위험|파손 심함|신속|전복", text):
        return "높음"
    if re.search("야간|어두|불편|지연|고장|소등|미끄럼", text):
        return "중간"
    return "낮음"


def _compose_input(complaint_text: str, captions: List[str]) -> str:
    parts = []
    if complaint_text.strip():
        parts.append(complaint_text.strip())
    if captions:
        parts.append("사진 설명: " + " ".join(captions))
    return " ".join(parts).strip() or "내용 없음"


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


def _run(req: SummarizeRequest, keep_sentences: int = 1) -> SummarizeResponse:
    use_images = not bool(req.meta.get("ignore_images"))
    caps = _get_captions(req.images) if (req.images and use_images) else []
    clean_text = _ko_cleanup_noise(_cap_input(req.complaint_text, 2200))
    joined = _compose_input(clean_text, caps)
    if not _ollama_available():
        sents = _sent_split(clean_text or joined)
        summary = _postprocess((sents[0] if sents else "요약할 내용이 충분하지 않습니다."), keep_sentences=1)
        engine = "fallback:first-sentence"
    else:
        summary = _ollama_summary(clean_text, caps, keep_sentences=keep_sentences) or "요약할 내용이 충분하지 않습니다."
        engine = f"ollama:{OLLAMA_MODEL}"
    return SummarizeResponse(
        summary=summary,
        category=_category(joined),
        urgency=_urgency(joined),
        evidence={
            "has_images": bool(req.images),
            "captions_count": len(caps),
            "used_models": {
                "summary": engine,
                "caption": "Salesforce/blip-image-captioning-base" if _state["cap_model"] else "(disabled)",
                "translate": "Helsinki-NLP/opus-mt-en-ko" if _state["translator_ready"] else "(disabled)",
            },
        },
    )


@app.get("/healthcheck")
def healthcheck():
    return {"ok": True}


@app.post("/summarize", response_model=SummarizeResponse)
async def summarize(request: Request, complaint_text: Optional[str] = Form(None), images: List[UploadFile] = File(None)):
    try:
        ct_header = request.headers.get("content-type", "").lower()
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
        return _run(req, keep_sentences=1)
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
