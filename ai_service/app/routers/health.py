from fastapi import APIRouter
router = APIRouter()

@router.get("/health")
@router.get("/healthcheck", include_in_schema=False)
def health():
    return {"ok": True}