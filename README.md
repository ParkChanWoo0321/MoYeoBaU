
# 간편민원서비스 백엔드 API 명세서

## 민원 등록

### Request

* URL: `POST /api/complaints`
* Body(JSON):

```json
{
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "서산시 해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "imageUrl": "http://example.com/image.jpg"
}
```

### Response

```json
{
  "id": 7,
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "서산시 해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "status": "PENDING",
  "imageUrl": "http://example.com/image.jpg",
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "createdAt": "2025-08-07T18:18:29",
  "updatedAt": "2025-08-07T18:18:29"
}
```

## 민원 목록 조회

### Request

* URL: `GET /api/complaints`

### Response

```json
[
  {
    "id": 7,
    "content": "도로에 쓰레기가 치워지지 않아요",
    "address": "서산시 해미면",
    "latitude": 36.7845,
    "longitude": 126.4532,
    "category": "ENVIRONMENT",
    "status": "COMPLETED",
    "imageUrl": "http://example.com/image.jpg",
    "userName": "홍길동",
    "phoneNumber": "01012345678",
    "createdAt": "2025-08-07T18:18:29",
    "updatedAt": "2025-08-07T18:18:37"
  }
]
```

## 내 민원 조회

### Request

* URL: `GET /api/complaints/my?userName={userName}&phoneNumber={phoneNumber}`

### Response

```json
{
  "complaints": [{ ... }],
  "totalCount": 1
}
```

## 민원 상세 조회

### Request

* URL: `GET /api/complaints/my/{id}?userName={userName}&phoneNumber={phoneNumber}`

### Response

```json
{
  "id": 7,
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "서산시 해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "status": "COMPLETED",
  "imageUrl": "http://example.com/image.jpg",
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "createdAt": "2025-08-07T18:18:29",
  "updatedAt": "2025-08-07T18:18:37"
}
```

## 민원 수정

### Request

* URL: `PATCH /api/complaints/my/{id}`
* Body(JSON):

```json
{
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "서산시 해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "imageUrl": "http://example.com/test2.jpg"
}
```

## 민원 삭제

### Request

* URL: `DELETE /api/complaints/my/{id}?userName={userName}&phoneNumber={phoneNumber}`

## 대시보드(전체 요약)

### Request

* URL: `GET /api/complaints/dashboard`

### Response

```json
{
  "totalCount": 1,
  "completedRate": 100.0,
  "categoryCounts": {"ENVIRONMENT": 1, "OTHER": 0, "ADMINISTRATION": 0, "SAFETY": 0, "FACILITY": 0},
  "categoryRates": {"ENVIRONMENT": 100.0, "OTHER": 0.0, "ADMINISTRATION": 0.0, "SAFETY": 0.0, "FACILITY": 0.0}
}
```

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
```

## 상태별 민원 조회

### Request

* URL: `GET /api/complaints/status?status={status}`

### Response

```json
[
  {
    "id": 7,
    "content": "도로에 쓰레기가 치워지지 않아요",
    "address": "서산시 해미면",
    "latitude": 36.7845,
    "longitude": 126.4532,
    "category": "ENVIRONMENT",
    "status": "COMPLETED",
    "imageUrl": "http://example.com/image.jpg",
    "userName": "홍길동",
    "phoneNumber": "01012345678",
    "createdAt": "2025-08-07T18:18:29",
    "updatedAt": "2025-08-07T18:18:37"
  }
]
```

# 간편민원서비스 백엔드 API 명세서

## (관리자) 민원 수정

### Request

* URL: `PATCH /api/admin/complaints/{id}`
* Body(JSON):

```json
{
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "서산시 해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "imageUrl": "http://example.com/test2.jpg"
}
```

### Response

```json
{
  "id": 7,
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "서산시 해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "status": "COMPLETED",
  "imageUrl": "http://example.com/test2.jpg",
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "createdAt": "2025-08-07T18:18:29",
  "updatedAt": "2025-08-08T00:07:04"
}
```

## (관리자) 민원 상태 수정

### Request

* URL: `PATCH /api/admin/complaints/{id}/status`
* Body(JSON):

```json
{
  "status": "COMPLETED"
}
```

### Response

```json
{
  "id": 7,
  "status": "COMPLETED",
  "updatedAt": "2025-08-08T00:07:04"
}
```

## (관리자) 민원 목록 조회

### Request

* URL: `GET /api/admin/complaints`

### Response

```json
{
  "complaints": [{ ... }],
  "totalCount": 1
}
```

## (관리자) 지역별 월간/일간 리포트 발행

### Request

* URL: `GET /api/admin/report?from={fromDate}&to={toDate}`

### Response

```json
{
  "totalCount": 1,
  "completedCount": 1,
  "processingCount": 0,
  "pendingCount": 0
}
```

## (관리자) 카테고리별 민원 상세보기

### Request

* URL: `GET /api/admin/complaints/category?category={category}`

### Response

```json
{
  "complaints": [{ ... }],
  "totalCount": 1
}
```

## (관리자) 우선순위별 지역 목록화

### Request

* URL: `GET /api/admin/priority`

### Response

```json
[]
```

## (관리자) 민원 삭제

### Request

* URL: `DELETE /api/admin/complaints/{id}`
