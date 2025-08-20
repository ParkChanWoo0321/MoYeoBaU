from pydantic import BaseModel

class ComplaintFields(BaseModel):
    위치: str = "미상"
    현상: str = "미상"
    문제점: str = "미상"
    위험성: str = "미상"
    요청사항: str = "미상"