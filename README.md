# 간편민원서비스 백엔드 API 명세서 (스크린샷 기반)

> 이 문서는 제공된 **8장 스크린샷**만을 바탕으로 작성된 API 명세입니다.

---

## 공통

* 날짜/시간 형식: `2025-08-09T00:46:40.228115600` (ISO-8601)
* 카테고리: `ENVIRONMENT | ADMINISTRATION | FACILITY | SAFETY | OTHER`
* 상태: `PENDING | IN_PROGRESS | COMPLETED`

---

## 사용자 API

### 민원 등록

**POST** `/api/complaints`

```json
{
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "imageUrl": "http://example.com/image.jpg"
}
```

**Response(201)** – 등록된 민원 정보 반환

---

### 내가 접수한 민원 목록

**GET** `/api/complaints/my?userName={userName}&phoneNumber={phoneNumber}`

* Response(200): complaints 배열 반환

---

### 내가 접수한 민원 상세

**GET** `/api/complaints/my/{id}?userName={userName}&phoneNumber={phoneNumber}`

---

### 내가 접수한 민원 수정

**PATCH** `/api/complaints/my/{id}`

* Body: 등록 시와 동일 필드

---

### 내가 접수한 민원 삭제

**DELETE** `/api/complaints/my/{id}?userName={userName}&phoneNumber={phoneNumber}`

* Response: `204 No Content`

---

### 전체 민원 목록

**GET** `/api/complaints`

---

### 대시보드(전체 요약)

**GET** `/api/complaints/dashboard`

* 통계 필드: totalCount, completedRate, categoryCounts 등

---

### 서산 지역 리스트

**GET** `/api/regions/seosan`

* 문자열 배열(지역명) 반환


## 민원 지도 조회

### Request

* URL: `GET /api/complaints/map`

### Response

```json
[
  {
    "latitude": 36.7845,
    "longitude": 126.4532,
    "category": "ENVIRONMENT",
    "status": "COMPLETED"
  }
]
```

## 사진 업로드

### Request

* URL: `POST /api/attachments/upload`
* Body(form-data):

  * Key: `file` (이미지 파일)

### Response

```json
{
  "id": 2,
  "fileName": "image.jpg",
  "filePath": "path/to/image.jpg",
  "fileSize": 1260,
  "url": "/uploads/image.jpg",
  "uploadedAt": "2025-08-07T18:07:13"
}
