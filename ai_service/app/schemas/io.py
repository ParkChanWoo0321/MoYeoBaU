# app/schemas/io.py
from pydantic import BaseModel, Field
from typing import List, Dict, Any

class ImageInput(BaseModel):
    url: str  # 이미지는 URL로만 받습니다.

class SummarizeRequest(BaseModel):
    complaint_text: str = ""
    images: List[ImageInput] = Field(default_factory=list)  # URL 배열
    meta: Dict[str, Any] = Field(default_factory=dict)
