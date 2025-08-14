from typing import Optional
import io
try:
    import pytesseract
    from PIL import Image
    HAS_TESS = True
except Exception:
    HAS_TESS = False

async def ocr_image_to_text(file) -> str:
    data = await file.read()
    if not HAS_TESS:
        return ""  # 배포 전 외부 OCR 연동(Clova/GCP 등)로 교체
    img = Image.open(io.BytesIO(data))
    return pytesseract.image_to_string(img, lang="kor+eng")
