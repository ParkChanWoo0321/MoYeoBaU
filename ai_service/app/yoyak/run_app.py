import sys, subprocess, pkgutil

REQUIRED = [
    "fastapi>=0.110",
    "uvicorn>=0.27",
    "transformers>=4.40",
    "torch",
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
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "8000"))
    reload_flag = os.getenv("RELOAD", "0") in ("1", "true", "True")
    uvicorn.run("app.main:app", host=host, port=port, reload=reload_flag)

if __name__ == "__main__":
    run()
