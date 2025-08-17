#ComposeIn/ComposeOut
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from .io import ImageInput
from .fields import ComplaintFields

# 문서화 입력 스키마
class ComposeIn(BaseModel):
    # 둘 중 하나만 줘도 됨:
    # 1) 원문 입력(텍스트/이미지) → 내부에서 추출 후 문서화
    complaint_text: Optional[str] = ""
    images: List[ImageInput] = Field(default_factory=list)

    # 2) 이미 추출된 필드가 있다면 바로 사용
    fields: Optional[ComplaintFields] = None

    # 옵션: 제목 접두/기관명/문체 등
    meta: Dict[str, Any] = Field(default_factory=dict)

# 문서화 출력 스키마
class ComposeOut(BaseModel):
    title: str
    body: str
    html: str
    fields: ComplaintFields  # 최종 사용된 필드(검증/수정 후)
