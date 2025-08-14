from pydantic import BaseModel, Field

class Applicant(BaseModel):
    name: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None

class Incident(BaseModel):
    date: Optional[str] = None
    location: Optional[str] = None

class Minwon(BaseModel):
    applicant: Applicant = Field(default_factory=Applicant)
    category: Optional[str] = None
    subject: Optional[str] = None
    description: Optional[str] = None
    incident: Incident = Field(default_factory=Incident)
    attachments: list[str] = Field(default_factory=list)
    consent: bool = False
    missing_fields: list[str] = Field(default_factory=list)
