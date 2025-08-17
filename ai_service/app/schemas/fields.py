#ComplaintFields(위치/현상/문제점/위험성/요청사항)
from pydantic import BaseModel

# 추출 결과 스키마(위치/현상/문제점/위험성/요청사항)
class ComplaintFields(BaseModel):
    위치: str = ""
    현상: str = ""
    문제점: str = ""
    위험성: str = ""
    요청사항: str = ""
