# 간편민원서비스 **관리자용** API 명세서 (최근 제공된 11장 스크린샷만 반영)

> Base URL: `http://localhost:8080`
> 공통 헤더(모든 관리자 요청): `PASSWORD: hanseo`

---

## 1) 민원 반려하기

**POST** `/api/admin/complaints/{id}/reject`

**Request Body**

```json
{ "reason": "ALREADY_RESOLVED" }
```

**200 Response**

```json
{
    "id": 1,
    "title": "신고합니다",
    "content": "도로에 쓰레기가 치워지지 않아요",
    "address": "해미면",
    "category": "ENVIRONMENT_CLEANING",
    "status": "REJECTED",
    "imageUrls": [],
    "userName": "홍길동",
    "phoneNumber": "01012345678",
    "createdAt": "2025-08-12T19:52:11.018746",
    "updatedAt": "2025-08-13T17:32:39.690867",
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null
}
```

---

## 2) 관리자 민원 수정

**PATCH** `/api/admin/complaints/{id}`

**Request Body**

```json
{
    "id": 1,
    "title": "신고합니다",
    "content": "도로에 쓰레기가 치워지지 않아요",
    "address": "해미면",
    "category": "ENVIRONMENT_CLEANING",
    "status": "PENDING",
    "imageUrls": [
        "http://example.com/test2.jpg"
    ],
    "userName": "홍길동",
    "phoneNumber": "01012345678",
    "createdAt": "2025-08-12T19:52:11.018746",
    "updatedAt": "2025-08-13T17:42:31.385871300",
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null
}
```

**200 Response (예시)**

```json
{
  "id": 1,
  "title": "신고합니다",
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "해미면",
  "category": "ENVIRONMENT_CLEANING",
  "status": "PENDING",
  "imageUrls": ["http://example.com/test2.jpg"],
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "createdAt": "2025-08-12T19:52:11.018746",
  "updatedAt": "2025-08-13T17:42:31.385871300",
  "rejectionReason": "ALREADY_RESOLVED",
  "rejectionDetail": null
}
```

---

## 3) 관리자 민원 삭제

**DELETE** `/api/admin/complaints/{id}`

**200 Response**

```json
{ "success": true, "message": "민원이 정상적으로 삭제되었습니다." }
```

---

## 4) 관리자 민원 상태 변경

**PATCH** `/api/admin/complaints/{id}/status`

**Request Body**

```json
{ "status": "COMPLETED" }
```

**200 Response (예시)**

```json
{
    "id": 1,
    "title": "쓰레기",
    "content": "도로에 쓰레기가 치워지지 않아요",
    "address": "해미면",
    "category": "ENVIRONMENT_CLEANING",
    "status": "COMPLETED",
    "imageUrls": [
        "http://example.com/test2.jpg"
    ],
    "userName": "홍길동",
    "phoneNumber": "01012345678",
    "createdAt": "2025-08-12T19:52:11.018746",
    "updatedAt": "2025-08-13T17:45:42.503379500",
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null
}
```

---

## 5) 일간 리포트

**GET** `/api/admin/report/daily?day=2025-08-13`

**200 Response**

```json
{ "totalCount": 0, "completedCount": 0, "processingCount": 0, "pendingCount": 0 }
```

---

## 6) 월간 리포트

**GET** `/api/admin/report/monthly?yearMonth=2025-08`

**200 Response**

```json
{ "totalCount": 1, "completedCount": 0, "processingCount": 0, "pendingCount": 1 }
```

---

## 7) 카테고리별 미처리 민원 수

**GET** `/api/admin/complaints/categories`

**200 Response (예시)**

```json
[
    {
        "category": "ENVIRONMENT_CLEANING",
        "count": 1
    },
    {
        "category": "FACILITY_DAMAGE",
        "count": 0
    },
    {
        "category": "TRAFFIC_PARKING",
        "count": 0
    },
    {
        "category": "SAFETY_RISK",
        "count": 0
    },
    {
        "category": "LIVING_INCONVENIENCE",
        "count": 0
    },
    {
        "category": "OTHERS_ADMIN",
        "count": 0
    }
]
```

---

## 8) 카테고리별 미처리 글 목록

**GET** `/api/admin/complaints/category?category=ENVIRONMENT_CLEANING`

**200 Response (요약)**

```json
{
    "complaints": [
        {
            "id": 1,
            "title": "신고합니다",
            "content": "도로에 쓰레기가 치워지지 않아요",
            "address": "해미면",
            "category": "ENVIRONMENT_CLEANING",
            "status": "PENDING",
            "imageUrls": [],
            "userName": "홍길동",
            "phoneNumber": "01012345678",
            "createdAt": "2025-08-12T19:52:11.018746",
            "updatedAt": "2025-08-13T17:41:56.748109",
            "rejectionReason": "ALREADY_RESOLVED",
            "rejectionDetail": null
        }
    ],
    "totalCount": 1
}
```

---

## 9) 카테고리별 미처리 글 목록 (페이징)

**GET** `/api/admin/complaints/by-category?category=TRAFFIC_PARKING&page=0&size=10`

**200 Response (예시)**

```json
{
    "content": [],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 10,
        "sort": {
            "empty": false,
            "sorted": true,
            "unsorted": false
        },
        "offset": 0,
        "paged": true,
        "unpaged": false
    },
    "last": true,
    "totalPages": 0,
    "totalElements": 0,
    "size": 10,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "first": true,
    "numberOfElements": 0,
    "empty": true
}
```

---

## 10) 관리자용 민원 상세 조회

**GET** `/api/admin/complaints/{id}`

**200 Response (예시)**

```json
{
    "id": 1,
    "title": "신고합니다",
    "content": "도로에 쓰레기가 치워지지 않아요",
    "category": "ENVIRONMENT_CLEANING",
    "status": "PENDING",
    "userName": "홍길동",
    "phoneNumberMasked": "010-****-5678",
    "address": "해미면",
    "imageUrls": [],
    "createdAt": "2025-08-12T19:52:11.018746",
    "comments": [
        {
            "id": 1,
            "author": "관리자",
            "content": "현장 확인 예정입니다.",
            "createdAt": "2025-08-13T17:41:00.942611"
        }
    ],
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null
}
```

---

## 11) 관리자 댓글 등록

**POST** `/api/admin/complaints/{id}/comments`

**Request Body**

```json
{ "content": "현장 확인 예정입니다." }
```

**200 Response**

```json
{
    "id": 1,
    "author": "관리자",
    "content": "현장 확인 예정입니다.",
    "createdAt": "2025-08-13T17:41:00.942611200"
}
```
---
