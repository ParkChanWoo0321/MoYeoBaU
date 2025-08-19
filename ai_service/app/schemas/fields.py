# app/schemas/fields.py
from pydantic import BaseModel
from typing import Optional

class ComplaintFields(BaseModel):
    위치: Optional[str] = None
    현상: Optional[str] = None
    문제점: Optional[str] = None
    위험성: Optional[str] = None
    요청사항: Optional[str] = None
