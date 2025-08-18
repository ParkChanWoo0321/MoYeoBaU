FROM python:3.11-slim-bookworm

ENV DEBIAN_FRONTEND=noninteractive \
    PIP_NO_CACHE_DIR=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN set -eux; \
    rm -rf /var/lib/apt/lists/*; \
    printf 'Acquire::Retries "5";\nAcquire::http::No-Cache "true";\n' > /etc/apt/apt.conf.d/99retries; \
    apt-get update -o Acquire::CompressionTypes::Order::=gz; \
    apt-get install -y --no-install-recommends \
        ca-certificates curl gnupg \
        build-essential \
        libjpeg62-turbo-dev zlib1g-dev \
    ; apt-get clean; rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN python -m pip install --upgrade pip && \
    pip install --default-timeout=120 -r requirements.txt

COPY app ./app
EXPOSE 8000
CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8000"]
