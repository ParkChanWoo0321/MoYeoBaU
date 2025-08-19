import requests, json, re

_JSON_GUARD = re.compile(r"\{.*\}", re.S)

def call_ollama(prompt: str, model="llama3.1:8b", timeout=8, temperature=0.2) -> dict:
    url = "http://localhost:11434/api/generate"  # Ollama 기본 포트
    payload = {
        "model": model,
        "prompt": prompt,
        "options": {"temperature": temperature}
    }
    resp = requests.post(url, json=payload, timeout=timeout)
    resp.raise_for_status()
    raw = resp.text
    m = _JSON_GUARD.search(raw)
    if not m:
        raise ValueError("No JSON found in response")
    return json.loads(m.group(0))
