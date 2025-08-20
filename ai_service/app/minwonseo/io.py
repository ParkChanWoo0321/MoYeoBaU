from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any

class ImageInput(BaseModel):
    url: Optional[str] = None
    base64: Optional[str] = None

class SummarizeRequest(BaseModel):
    complaint_text: str = ""
    images: List[ImageInput] = Field(default_factory=list)
    meta: Dict[str, Any] = Field(default_factory=dict)