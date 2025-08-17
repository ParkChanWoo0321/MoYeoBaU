#ImageInput, SummarizeRequest
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any

class ImageInput(BaseModel):
    url: Optional[str] = None
    base64: Optional[str] = None

# 입력 스키마(사용자가 보내는 텍스트/이미지/메타)
class SummarizeRequest(BaseModel):
    complaint_text: str = ""
    images: List[ImageInput] = Field(default_factory=list)
    meta: Dict[str, Any] = Field(default_factory=dict)
