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
    try:
        io = importlib.import_module("io")
        PIL = importlib.import_module("PIL.Image")
        requests_mod = importlib.import_module("requests")
        if image_item.url:
            resp = requests_mod.get(image_item.url, timeout=10)
            resp.raise_for_status()
            return PIL.open(io.BytesIO(resp.content)).convert("RGB")
        if image_item.base64:
            b64 = image_item.base64
            if b64.startswith("data:"):
                b64 = b64.split(",", 1)[-1]
            base64_mod = importlib.import_module("base64")
            return PIL.open(io.BytesIO(base64_mod.b64decode(b64))).convert("RGB")
    except Exception:
        return None
    return None


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
