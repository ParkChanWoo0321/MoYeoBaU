# 간편민원서비스 **관리자용** API 명세서

---

- [AI API 명세서](AI_API.md)
- [USER API 명세서](USER_API.md)
  
---

> Base URL: `http://localhost:8080`
> 공통 헤더(모든 관리자 요청): `PASSWORD: hanseo`

---

## 민원 반려하기

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
    "createdAt": "2025-08-15T18:20:32.413387",
    "updatedAt": "2025-08-15T18:20:32.413387",
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null,
    "comments": null,
    "commentCount": null
}
```

---

## 관리자 민원 수정

**PATCH** `/api/admin/complaints/{id}`

**Request Body**

```json
{
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "해미면",
  "category": "ENVIRONMENT_CLEANING",
  "imageUrl": "http://example.com/test2.jpg"
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
    "status": "REJECTED",
    "imageUrls": [],
    "userName": "홍길동",
    "phoneNumber": "01012345678",
    "createdAt": "2025-08-15T18:20:32.413387",
    "updatedAt": "2025-08-15T21:05:55.218355",
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null,
    "comments": null,
    "commentCount": null
}
```

---

## 관리자 민원 삭제

**DELETE** `/api/admin/complaints/{id}`

**200 Response**

```json
{ "success": true, "message": "민원이 정상적으로 삭제되었습니다." }
```

---

## 관리자 민원 상태 변경

**PATCH** `/api/admin/complaints/{id}/status`

**Request Body**

```json
{ "status": "COMPLETED" }
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
    "imageUrls": [],
    "userName": "홍길동",
    "phoneNumber": "01012345678",
    "createdAt": "2025-08-15T18:20:32.413387",
    "updatedAt": "2025-08-15T21:07:20.237313300",
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null,
    "comments": null,
    "commentCount": null
}
```

---

## 일간 리포트

**GET** `/api/admin/report/daily?day=2025-08-13`

**200 Response**

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

**GET** `/api/admin/report/monthly?yearMonth=2025-08`

**200 Response**

```json
{
    "totalCount": 2,
    "completedCount": 0,
    "processingCount": 0,
    "pendingCount": 2
}
```

---

## 카테고리별 미처리 민원 수

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

## 카테고리별 미처리 글 목록

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
            "createdAt": "2025-08-15T18:20:32.413387",
            "updatedAt": "2025-08-15T21:07:20.237313",
            "rejectionReason": "ALREADY_RESOLVED",
            "rejectionDetail": null,
            "comments": null,
            "commentCount": null
        }
    ],
    "totalCount": 1
}
```

---

## 관리자용 민원 상세 조회

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

## 관리자 댓글 등록

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

## 접수된 미처리 민원

**GET** `/api/admin/complaints/pending`

**200 Response (예시)**

```json
{
    "complaints": [
        {
            "id": 2,
            "title": "공원시설의 문제",
            "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
            "address": "해미면",
            "category": "FACILITY_DAMAGE",
            "status": "PENDING",
            "imageUrls": [
                "/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"
            ],
            "userName": "김멋사",
            "phoneNumber": "01012345678",
            "createdAt": "2025-08-15T18:48:26.550027",
            "updatedAt": "2025-08-15T18:48:26.550027",
            "rejectionReason": null,
            "rejectionDetail": null,
            "comments": null,
            "commentCount": null
        },
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
            "createdAt": "2025-08-15T18:20:32.413387",
            "updatedAt": "2025-08-15T21:07:20.237313",
            "rejectionReason": "ALREADY_RESOLVED",
            "rejectionDetail": null,
            "comments": null,
            "commentCount": null
        }
    ],
    "totalCount": 2
}
```

---

## 긴급/다발민원

**GET** `/api/admin/complaints/emergency`

**200 Response (예시)**

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
            "createdAt": "2025-08-15T18:20:32.413387",
            "updatedAt": "2025-08-15T21:07:20.237313",
            "rejectionReason": "ALREADY_RESOLVED",
            "rejectionDetail": null,
            "comments": null,
            "commentCount": null
        }
    ],
    "totalCount": 1
}
```
