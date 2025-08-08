# 간편민원서비스 백엔드 API 명세서

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

# 간편민원서비스 관리자 API 명세서

**인증 정보**
모든 관리자 API 호출 시 Basic Auth 필요

* **Username:** likelion
* **Password:** hanseo

---

## 우선순위 민원 목록

**GET** `http://localhost:8080/api/admin/priority/complaints?status=PENDING&limit=5`

**Authorization**: Basic Auth

**Response (200)**

```json
[]
```

---

## 카테고리별 상태 카운트

**GET** `http://localhost:8080/api/admin/stats/category?status=IN_PROGRESS`

**Authorization**: Basic Auth

**Response (200)**

```json
[
  {
    "category": "ENVIRONMENT",
    "count": 1
  }
]
```

---

## 관리자 민원 삭제

**DELETE** `http://localhost:8080/api/admin/complaints/9`

**Authorization**: Basic Auth

**Response (204)**

```json
```

---

## 관리자 민원 수정

**PATCH** `http://localhost:8080/api/admin/complaints/9`

**Authorization**: Basic Auth

**Body (JSON)**

```json
{
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "imageUrl": "http://example.com/test2.jpg"
}
```

**Response (200)**

```json
{
  "id": 9,
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "status": "PENDING",
  "imageUrl": "http://example.com/test2.jpg",
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "createdAt": "2025-08-09T00:46:40.228152",
  "updatedAt": "2025-08-09T00:48:03.548793"
}
```

---

## 관리자 민원 상태변경

**PATCH** `http://localhost:8080/api/admin/complaints/9/status`

**Authorization**: Basic Auth

**Body (JSON)**

```json
{ "status": "IN_PROGRESS" }
```

**Response (200)**

```json
{
  "id": 9,
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "status": "IN_PROGRESS",
  "imageUrl": "http://example.com/test2.jpg",
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "createdAt": "2025-08-09T00:46:40.228152",
  "updatedAt": "2025-08-09T00:57:41.128106700"
}
```

---

## 페이징 목록

**GET** `http://localhost:8080/api/admin/complaints/page?page=0&size=5&sort=createdAt,DESC`

**Authorization**: Basic Auth

**Response (200)**

```json
{
  "content": [
    {
      "id": 9,
      "content": "도로에 쓰레기가 치워지지 않아요",
      "address": "해미면",
      "latitude": 36.7845,
      "longitude": 126.4532,
      "category": "ENVIRONMENT",
      "status": "IN_PROGRESS",
      "imageUrl": "http://example.com/test2.jpg",
      "userName": "홍길동",
      "phoneNumber": "01012345678",
      "createdAt": "2025-08-09T00:46:40.228152",
      "updatedAt": "2025-08-09T00:57:41.128107"
    }
  ],
  "pageable": { }
}
```

---

## 지역별 월간/일간 리포트(지역)

**GET** `http://localhost:8080/api/admin/report/region?from=2025-08-01&to=2025-08-31`

**Authorization**: Basic Auth

**Response (200)**

```json
[
  {
    "address": "해미면",
    "status": "IN_PROGRESS",
    "count": 1
  }
]
```

---

## 카테고리별 민원 상세보기

**GET** `http://localhost:8080/api/admin/complaints/category?category=ENVIRONMENT`

**Authorization**: Basic Auth

**Response (200)**

```json
{
  "complaints": [
    {
      "id": 9,
      "content": "도로에 쓰레기가 치워지지 않아요",
      "address": "해미면",
      "latitude": 36.7845,
      "longitude": 126.4532,
      "category": "ENVIRONMENT",
      "status": "IN_PROGRESS",
      "imageUrl": "http://example.com/test2.jpg",
      "userName": "홍길동",
      "phoneNumber": "01012345678",
      "createdAt": "2025-08-09T00:46:40.228152",
      "updatedAt": "2025-08-09T00:57:41.128107"
    }
  ],
  "totalCount": 1
}
```

---

## 오늘 리포트

**GET** `http://localhost:8080/api/admin/report/daily?day=2025-08-07`

**Authorization**: Basic Auth

**Response (200)**

```json
{
  "totalCount": 0,
  "completedCount": 0,
  "processingCount": 0,
  "pendingCount": 0
}
```

---

## 월간 리포트

**GET** `http://localhost:8080/api/admin/report/monthly?yearMonth=2025-08`

**Authorization**: Basic Auth

**Response (200)**

```json
{
  "totalCount": 1,
  "completedCount": 0,
  "processingCount": 1,
  "pendingCount": 0
}
```

---

## 전체 민원 목록 조회

**GET** `http://localhost:8080/api/admin/complaints`

**Authorization**: Basic Auth

**Response (200)**

```json
[
  {
    "id": 9,
    "content": "도로에 쓰레기가 치워지지 않아요",
    "address": "해미면",
    "latitude": 36.7845,
    "longitude": 126.4532,
    "category": "ENVIRONMENT",
    "status": "IN_PROGRESS",
    "imageUrl": "http://example.com/test2.jpg",
    "userName": "홍길동",
    "phoneNumber": "01012345678",
    "createdAt": "2025-08-09T00:46:40.228152",
    "updatedAt": "2025-08-09T00:57:41.128107"
  }
]
```

