from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from .routers import health, minwon

def create_app():
    app = FastAPI(title="Seosan AI Service")
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://localhost:8080", "http://127.0.0.1:8080", "*"],
        allow_methods=["*"],
        allow_headers=["*"],
    )
    app.include_router(health.router)
    app.include_router(minwon.router)
    return app

app = create_app()
