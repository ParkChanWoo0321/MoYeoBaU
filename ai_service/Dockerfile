FROM python:3.11-slim

# 1) OS 패키지: curl(헬스체크), 빌드필수, 이미지 코덱
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl build-essential \
    libjpeg62-turbo-dev zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

# 2) 기본 설정(선택)
ENV PIP_NO_CACHE_DIR=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# 3) 먼저 requirements만 복사 → 레이어 캐시 효율
COPY requirements.txt .
RUN python -m pip install --upgrade pip && \
    pip install --default-timeout=120 -r requirements.txt

# 4) 앱 소스
COPY app ./app

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
