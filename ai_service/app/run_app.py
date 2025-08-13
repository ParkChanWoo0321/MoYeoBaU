# run_app.py
"""
실행 버튼으로 이 파일만 돌리면, 현재 인터프리터(sys.executable)에
필수 패키지를 자동 설치/업데이트하고 uvicorn으로 app.main:app을 실행합니다.
"""

import sys, subprocess, pkgutil

REQUIRED = [
    "fastapi>=0.110",
    "uvicorn>=0.27",
    "transformers>=4.40",
    "torch",                     # 버전은 환경에 맞게 자동
    "pillow>=10.0",
    "requests>=2.31",
    "sentencepiece>=0.1.99",
    "sentence-transformers>=2.2.2",
    "kss>=4.5.4",
    "numpy>=1.24",
    "scikit-learn>=1.3",
]

def ensure(pkgs):
    need = []
    for spec in pkgs:
        name = spec.split("==")[0].split(">=")[0].split(" ")[0]
        if not pkgutil.find_loader(name.replace("-", "_")):
            need.append(spec)
        else:
            # 설치는 되어 있어도 버전이 낮을 수 있으니 업그레이드 후보로 추가
            if ">=" in spec or "==" in spec:
                need.append(spec)
    if need:
        print("[bootstrap] installing/upgrading:", need)
        subprocess.check_call([sys.executable, "-m", "pip", "install", "-U", *need])
    else:
        print("[bootstrap] all dependencies present")

def run():
    ensure(REQUIRED)
    import os
    import uvicorn
    # uvicorn을 모듈 방식으로 실행하면 Windows에서도 안전
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "8000"))
    reload_flag = os.getenv("RELOAD", "0") in ("1", "true", "True")
    uvicorn.run("app.main:app", host=host, port=port, reload=reload_flag)

if __name__ == "__main__":
    run()
