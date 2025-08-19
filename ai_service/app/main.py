# app/main.py
import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

def create_app():
    app = FastAPI(title="Seosan AI Service")

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"], allow_credentials=False,
        allow_methods=["*"], allow_headers=["*"],
    )

    base_dir = os.path.dirname(__file__)
    static_dir = os.path.join(base_dir, "static")
    uploads_dir = os.path.join(static_dir, "uploads")
    os.makedirs(uploads_dir, exist_ok=True)  # 폴더 자동 생성

    app.mount("/static", StaticFiles(directory=static_dir), name="static")

    # 라우터 include …
    from .routers import health, minwon, files_upload
    app.include_router(health.router)
    app.include_router(minwon.router)
    app.include_router(files_upload.router)
    return app

app = create_app()
