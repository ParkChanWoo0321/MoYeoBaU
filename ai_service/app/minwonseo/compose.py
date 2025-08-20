# schemas/compose.py
from pydantic import BaseModel, Field, ConfigDict
from typing import Optional, List, Dict, Any
from .io import ImageInput
from .fields import ComplaintFields

class ComposeIn(BaseModel):
    # 요청 바디에 모르는 필드가 와도 무시
    model_config = ConfigDict(extra="ignore")

    # 본문/이미지
    complaint_text: Optional[str] = ""
    images: List[ImageInput] = Field(default_factory=list)

    # 이미 추출된 필드가 있으면 바로 사용
    fields: Optional[ComplaintFields] = None

    # ✅ 서산 기본 meta 자동 주입
    meta: Dict[str, Any] = Field(default_factory=lambda: {
        "org": "서산시청",
        "receiver": "서산시청장 귀하",
        "title_prefix": "[서산시]"
    }
                                 )

    # 신청인 정보
    applicant_name: Optional[str] = None
    applicant_phone: Optional[str] = None
    applicant_address: Optional[str] = None  # ex) "서산시 동문동 …"

class ComposeOut(BaseModel):
    title: str
    body: str
    fields: ComplaintFields