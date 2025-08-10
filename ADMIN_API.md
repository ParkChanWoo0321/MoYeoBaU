# 간편민원서비스 관리자 API 명세서

---

- [USER API 명세서](USER_API.md)
- [AI API 명세서](AI_API.md)

---

**인증 정보**
모든 관리자 API 호출 시 암호 필요

Headers -> X-ADMIN-SECRET : hanseo

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

