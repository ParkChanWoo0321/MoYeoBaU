# 간편민원서비스 유저 API 명세서

---

- [ADMIN API 명세서](ADMIN_API.md)
- [AI API 명세서](AI_API.md)

> Base URL: `http://localhost:8080`
> Content-Type: 기본 `application/json`; 사진 업로드만 `multipart/form-data`

---

## 지역 목록 조회 (민원 접수 창 내 지역 선택)

**GET** `/api/regions/seosan`

**Response 200**

```json
[
  "해미면",
  "고북면",
  "인지면",
  "팔봉면",
  "부석면",
  "지곡면",
  "운산면",
  "성연면",
  "음암면",
  "부춘동",
  "동문동",
  "수석동",
  "석남동"
]
```

---

## 사진 업로드

**POST** `/api/attachments/upload`

* Body: `form-data`

  * Key: `file` (File) — 여러 파일 선택 가능 (화면에 "2 files" 표시)

**Response 200**

```json
{
  "id": 1,
  "fileName": "4a3725f0-b703-4757-8bdd-3b6e02658dff.jpg",
  "filePath": "C:\\Users\\...\\uploads\\4a3725f0-b703-4757-8bdd-3b6e02658dff.jpg",
  "fileSize": 1260,
  "url": "/uploads/4a3725f0-b703-4757-8bdd-3b6e02658dff.jpg",
  "uploadedAt": "2025-08-13T19:50:49.7238534",
  "complaint": null
}
```

---

## 민원 접수하기

**POST** `/api/complaints`

**Request Body**

```json
{
  "title": "신고합니다",
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "해미면",
  "category": "ENVIRONMENT_CLEANING",
  "imageUrls": "http://example.com/image.jpg"
}
```

**Response 201**

```json
{
    "id": 5,
    "title": "신고합니다",
    "content": "도로에 쓰레기가 치워지지 않아요",
    "address": "해미면",
    "category": "ENVIRONMENT_CLEANING",
    "status": "PENDING",
    "imageUrls": [],
    "userName": "홍길동",
    "phoneNumber": "01012345678",
    "createdAt": "2025-08-13T19:42:45.796355800",
    "updatedAt": "2025-08-13T19:42:45.796355800",
    "rejectionReason": null,
    "rejectionDetail": null
}
```

---

## 전체 민원 목록 조회

**GET** `/api/complaints`

**Response 200**

```json
{
    "complaints": [
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
            "updatedAt": "2025-08-13T17:45:42.503380",
            "rejectionReason": "ALREADY_RESOLVED",
            "rejectionDetail": null
        }
    ],
    "totalCount": 1
}
```

---

## 대시보드(전체 요약)

**GET** `/api/complaints/dashboard`

**Response 200**

```json
{
    "totalCount": 0,
    "totalCountDelta": 0.0,
    "completedRate": 0.0,
    "completedRateDelta": 0.0,
    "averageResolutionDays": 0.0,
    "averageResolutionDelta": 0.0,
    "categoryCounts": {
        "FACILITY_DAMAGE": 0,
        "LIVING_INCONVENIENCE": 0,
        "ENVIRONMENT_CLEANING": 0,
        "TRAFFIC_PARKING": 0,
        "OTHERS_ADMIN": 0,
        "SAFETY_RISK": 0
    },
    "categoryRates": {
        "FACILITY_DAMAGE": 0.0,
        "LIVING_INCONVENIENCE": 0.0,
        "ENVIRONMENT_CLEANING": 0.0,
        "TRAFFIC_PARKING": 0.0,
        "OTHERS_ADMIN": 0.0,
        "SAFETY_RISK": 0.0
    },
    "latestFive": [],
    "regionStats": [
        {
            "region": "해미면",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "고북면",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "인지면",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "팔봉면",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "부석면",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "지곡면",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "운산면",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "성연면",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "음암면",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "부춘동",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "동문동",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "수석동",
            "count": 0,
            "rate": 0.0
        },
        {
            "region": "석남동",
            "count": 0,
            "rate": 0.0
        }
    ]
}
```

---

## 내 민원 목록 조회

**GET** `/api/complaints/my?userName=홍길동&phoneNumber=01012345678`

**Response 200**

```json
{
    "complaints": [
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
            "updatedAt": "2025-08-13T17:45:42.503380",
            "rejectionReason": "ALREADY_RESOLVED",
            "rejectionDetail": null
        }
    ],
    "totalCount": 1
}
```

---

## 내 민원 단건 조회

**GET** `/api/complaints/my/1?userName=홍길동&phoneNumber=01012345678`

**Response 200**

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
    "updatedAt": "2025-08-13T17:45:42.503380",
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null
}
```

---

## 내 민원 수정

**PATCH** `/api/complaints/my/1`

**Request Body**

```json
{
  "title": "쓰레기",
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "content": "도로에 쓰레기가 치워지지 않아요",
  "address": "해미면",
  "category": "ENVIRONMENT_CLEANING",
  "imageUrls": "http://example.com/test2.jpg"
}
```

**Response 200**

```json
{
    "id": 1,
    "title": "쓰레기",
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
    "updatedAt": "2025-08-13T17:45:15.539135800",
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null
}
```
---

## 내 민원 삭제

**DELETE** `/api/complaints/my/4?userName=홍길동&phoneNumber=01012345678`

**Response 200**

```json
{
  "success": true,
  "message": "민원이 정상적으로 삭제되었습니다."
}
```

---

## 다른 사람 글 보기 (공용)

**GET** `/api/complaints/1`

**Response 200**

```json
{
    "id": 1,
    "title": "쓰레기",
    "content": "도로에 쓰레기가 치워지지 않아요",
    "category": "ENVIRONMENT_CLEANING",
    "status": "COMPLETED",
    "userName": null,
    "phoneNumberMasked": null,
    "address": "해미면",
    "imageUrls": [
        "http://example.com/test2.jpg"
    ],
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
