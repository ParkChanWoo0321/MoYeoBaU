import os, json, uuid, numpy as np, faiss
from sentence_transformers import SentenceTransformer

# 임베딩 모델 로드
MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"  # 384 dim
_model = SentenceTransformer(MODEL_NAME)
# FAISS 인덱스 준비
_index: faiss.Index = faiss.IndexFlatIP(384)           # cosine≈inner product(정규화 전제)
# 메타데이터 저장 리스트
_metas: list[dict] = []

# 디스크 영속화 경로 설정
DATA_DIR = "data"; os.makedirs(DATA_DIR, exist_ok=True)
IDX_PATH = os.path.join(DATA_DIR, "examples.faiss")
META_PATH = os.path.join(DATA_DIR, "examples.jsonl")

def _save():
    # 현재 메모리 상태(_Index, _metas)를 디스크로 저장
    faiss.write_index(_index, IDX_PATH)
    with open(META_PATH, "w", encoding="utf-8") as f:
        for m in _metas: f.write(json.dumps(m, ensure_ascii=False)+"\n")

def _load():
    # 프로세스 시작 시 디스크에서 인덱스/메타를 복구
    global _index, _metas
    if os.path.exists(IDX_PATH): _index = faiss.read_index(IDX_PATH)
    if os.path.exists(META_PATH):
        with open(META_PATH, "r", encoding="utf-8") as f:
            _metas[:] = [json.loads(x) for x in f]
_load()

def _embed(texts: list[str]) -> np.ndarray:
    """
        문자열 리스트를 임베딩 벡터(배열)로 변환.
        - normalize_embeddings=True: L2 정규화로 벡터 길이를 1로 맞춘다.
          → 내적 점수 = 코사인 유사도와 동일해짐.
        - 반환 dtype은 float32 (FAISS 호환)
        """
    vecs = _model.encode(texts, normalize_embeddings=True)
    return np.array(vecs, dtype="float32")

# 학습(예시를 추가)
def ingest_example(source_text: str, minwon_json: dict, source: str="example"):
    vec = _embed([source_text])
     """
        새 예시를 '학습'처럼 추가(=인덱스/메타 업데이트).
        - source_text: 사용자가 짧게 적은 설명(검색의 기준이 되는 문장)
        - minwon_json: 확정된 민원 JSON(너의 Minwon 스키마 그대로 저장)
        - source: 데이터 출처 태그(예: 'confirmed', 'seed', 'imported' 등)
        동작:
          1) 텍스트 임베딩
          2) 벡터 인덱스에 추가
          3) 메타리스트에 보조정보 추가
          4) 디스크로 저장(_save)
        """
    _index.add(vec)
    _metas.append({"id": str(uuid.uuid4()), "text": source_text, "minwon": minwon_json, "source": source})
    _save()
    return {"ok": True}

# 학습된거 중에 유사한걸 검색
def search_similar(query: str, top_k: int = 3):
     """
        질의(query)와 가장 유사한 예시 top_k개를 찾아 반환.
        반환 포맷:
          [
            { "score": 0.87, "id": "...", "text": "...", "minwon": {...}, "source": "..." },
            ...
          ]
        사용처:
          - minwon_prepare 단계에서 유사 예시의 카테고리/필수항목/문장 톤을 참고해
            자동 보정/가이드 제공.
        """
    if not _metas:
        return []
    qv = _embed([query])
    D, I = _index.search(qv, top_k)
    hits = []
    for score, idx in zip(D[0], I[0]):
        if idx < 0: continue
        hits.append({"score": float(score), **_metas[idx]})
    return hits
