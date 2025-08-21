# 간편민원서비스 유저 API 명세서

---

- [ADMIN API 명세서](ADMIN_API.md)
- [AI API 명세서](AI_API.md)
---

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

  * Key: `files` (File) — 여러 파일 선택 가능

**Response 200**

```json
[
    "/uploads/af5b0376-9bc4-49a7-93e1-2f9f2c6a3721.png"
]
```

---

## 민원 접수하기

**POST** `/api/complaints`

**Request Body**

```json
{
  "title":"공원시설의 문제",
  "userName": "김멋사",
  "phoneNumber": "01012345678",
  "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
  "address": "해미면",
  "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"]
  "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"]
}
```

**Response 201**

```json
{
    "id": 4,
    "title": "공원시설의 문제",
    "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
    "address": "해미면",
    "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
    "status": "PENDING",
    "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
    "userName": "김멋사",
    "phoneNumber": "010-1234-5678",
    "createdAt": "2025-08-15T22:07:43.997826900",
    "updatedAt": "2025-08-15T22:07:43.997826900",
    "rejectionReason": null,
    "rejectionDetail": null,
    "comments": null,
    "commentCount": null
    "summaryLocation": "",
    "summaryPhenomenon": "",
    "summaryProblem": "",
    "summaryRisk": "",
    "summaryRequest": ""
}
```

---

## 전체 민원 목록 조회

**GET** `/api/complaints`

**Response 200**

```json
[
    {
        "id": 1,
        "title": "신고합니다",
        "content": "도로에 쓰레기가 치워지지 않아요",
        "address": "해미면",
        "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
        "status": "PENDING",
        "imageUrls": [],
        "userName": "홍길동",
        "phoneNumber": "010-1234-5678",
        "createdAt": "2025-08-15T18:20:32.413387",
        "updatedAt": "2025-08-15T21:07:20.237313",
        "rejectionReason": "ALREADY_RESOLVED",
        "rejectionDetail": null,
        "comments": null,
        "commentCount": null
        "summaryLocation": "",
        "summaryPhenomenon": "",
        "summaryProblem": "",
        "summaryRisk": "",
        "summaryRequest": ""
    },
    {
        "id": 2,
        "title": "공원시설의 문제",
        "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
        "address": "해미면",
        "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
        "status": "PENDING",
        "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
        "userName": "김멋사",
        "phoneNumber": "010-1234-5678",
        "createdAt": "2025-08-15T18:48:26.550027",
        "updatedAt": "2025-08-15T18:48:26.550027",
        "rejectionReason": null,
        "rejectionDetail": null,
        "comments": null,
        "commentCount": null
        "summaryLocation": "",
        "summaryPhenomenon": "",
        "summaryProblem": "",
        "summaryRisk": "",
        "summaryRequest": ""
    },
    {
        "id": 3,
        "title": "공원시설의 문제",
        "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
        "address": "해미면",
        "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
        "status": "PENDING",
        "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
        "userName": "김멋사",
        "phoneNumber": "010-1234-5678",
        "createdAt": "2025-08-15T22:05:05.305184",
        "updatedAt": "2025-08-15T22:05:05.305184",
        "rejectionReason": null,
        "rejectionDetail": null,
        "comments": null,
        "commentCount": null
        "summaryLocation": "",
        "summaryPhenomenon": "",
        "summaryProblem": "",
        "summaryRisk": "",
        "summaryRequest": ""
    },
    {
        "id": 4,
        "title": "공원시설의 문제",
        "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
        "address": "해미면",
        "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
        "status": "PENDING",
        "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
        "userName": "김멋사",
        "phoneNumber": "010-1234-5678",
        "createdAt": "2025-08-15T22:07:43.997827",
        "updatedAt": "2025-08-15T22:07:43.997827",
        "rejectionReason": null,
        "rejectionDetail": null,
        "comments": null,
        "commentCount": null
        "summaryLocation": "",
        "summaryPhenomenon": "",
        "summaryProblem": "",
        "summaryRisk": "",
        "summaryRequest": ""
    }
]
```

---


---

## 내 민원 목록 조회

**GET** `/api/complaints/my?userName=김멋사&phoneNumber=01012345678`

**Response 200**

```json
{
    "complaints": [
        {
            "id": 2,
            "title": "공원시설의 문제",
            "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
            "address": "해미면",
            "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
            "status": "PENDING",
            "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
            "userName": "김멋사",
            "phoneNumber": "010-1234-5678",
            "createdAt": "2025-08-15T18:48:26.550027",
            "updatedAt": "2025-08-15T18:48:26.550027",
            "rejectionReason": null,
            "rejectionDetail": null,
            "comments": null,
            "commentCount": null
            "summaryLocation": "",
            "summaryPhenomenon": "",
            "summaryProblem": "",
            "summaryRisk": "",
            "summaryRequest": ""
        },
        {
            "id": 3,
            "title": "공원시설의 문제",
            "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
            "address": "해미면",
            "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
            "status": "PENDING",
            "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
            "userName": "김멋사",
            "phoneNumber": "010-1234-5678",
            "createdAt": "2025-08-15T22:05:05.305184",
            "updatedAt": "2025-08-15T22:05:05.305184",
            "rejectionReason": null,
            "rejectionDetail": null,
            "comments": null,
            "commentCount": null
            "summaryLocation": "",
            "summaryPhenomenon": "",
            "summaryProblem": "",
            "summaryRisk": "",
            "summaryRequest": ""
        },
        {
            "id": 4,
            "title": "공원시설의 문제",
            "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
            "address": "해미면",
            "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
            "status": "PENDING",
           "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
            "userName": "김멋사",
            "phoneNumber": "010-1234-5678",
            "createdAt": "2025-08-15T22:07:43.997827",
            "updatedAt": "2025-08-15T22:07:43.997827",
            "rejectionReason": null,
            "rejectionDetail": null,
            "comments": null,
            "commentCount": null
            "summaryLocation": "",
            "summaryPhenomenon": "",
            "summaryProblem": "",
            "summaryRisk": "",
            "summaryRequest": ""
        }
    ],
    "totalCount": 3
}
```

---

## 내 민원 확인

**GET** `/api/complaints/my/{id}?userName=홍길동&phoneNumber=01012345678`

**Response 200**

```json
{
    "id": 1,
    "title": "신고합니다",
    "content": "도로에 쓰레기가 치워지지 않아요",
    "address": "해미면",
    "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
    "status": "PENDING",
    "imageUrls": [],
    "userName": "홍길동",
    "phoneNumber": "010-1234-5678",
    "createdAt": "2025-08-15T18:20:32.413387",
    "updatedAt": "2025-08-15T21:07:20.237313",
    "rejectionReason": "ALREADY_RESOLVED",
    "rejectionDetail": null,
    "comments": null,
    "commentCount": null
    "summaryLocation": "",
    "summaryPhenomenon": "",
    "summaryProblem": "",
    "summaryRisk": "",
    "summaryRequest": ""
}
```

---

## 내 민원 삭제

**DELETE** `/api/complaints/my/{id}?userName=홍길동&phoneNumber=01012345678`

**Response 200**

```json
{
  "success": true,
  "message": "민원이 정상적으로 삭제되었습니다."
}
```

---

## 다른사람 글 보기

**GET** `/api/complaints/{id}`

**Response 200**

```json
{
    "id": 2,
    "title": "공원시설의 문제",
    "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
    "address": "해미면",
    "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
    "status": "PENDING",
    ""imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
    "userName": "김멋사",
    "phoneNumber": "010-1234-5678",
    "createdAt": "2025-08-15T18:48:26.550027",
    "updatedAt": "2025-08-15T18:48:26.550027",
    "rejectionReason": null,
    "rejectionDetail": null,
    "comments": null,
    "commentCount": null
    "summaryLocation": "",
    "summaryPhenomenon": "",
    "summaryProblem": "",
    "summaryRisk": "",
    "summaryRequest": ""
}
```

---

## 대시보드 파이차트

**GET** `/api/complaints/piechart`

**Response 200**

```json
{
    "total": 3,
    "slices": [
        {
            "name": "해미면",
            "value": 3,
            "percent": 100.0
        }
    ]
}
```

---

## 대시보드 top5

**GET** `/api/complaints/region-top5`

**Response 200**

```json
[
    {
        "region": "해미면",
        "count": 3,
        "percent": 100.0,
        "deltaPercent": 300.0,
        "up": true
    },
    {
        "region": "고북면",
        "count": 0,
        "percent": 0.0,
        "deltaPercent": 0.0,
        "up": false
    },
    {
        "region": "동문동",
        "count": 0,
        "percent": 0.0,
        "deltaPercent": 0.0,
        "up": false
    },
    {
        "region": "부석면",
        "count": 0,
        "percent": 0.0,
        "deltaPercent": 0.0,
        "up": false
    },
    {
        "region": "부춘동",
        "count": 0,
        "percent": 0.0,
        "deltaPercent": 0.0,
        "up": false
    }
]
```

---

## 카테고리별 민원현황

**GET** `/api/complaints/categorystat`

**Response 200**

```json
[
    {
        "category": "FACILITY_DAMAGE",
        "valuePercent": 300.0,
        "up": true
    },
    {
        "category": "ENVIRONMENT_CLEANING",
        "valuePercent": 0.0,
        "up": false
    },
    {
        "category": "LIVING_INCONVENIENCE",
        "valuePercent": 0.0,
        "up": false
    },
    {
        "category": "OTHERS_ADMIN",
        "valuePercent": 0.0,
        "up": false
    },
    {
        "category": "SAFETY_RISK",
        "valuePercent": 0.0,
        "up": false
    },
    {
        "category": "TRAFFIC_PARKING",
        "valuePercent": 0.0,
        "up": false
    }
]
```

---

## 카테고리별 글 목록

**GET** `/api/complaints/categorylist?category=FACILITY_DAMAGE&status=ALL&days=30`

**Response 200**

```json
[
    {
        "id": 4,
        "title": "공원시설의 문제",
        "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
        "address": "해미면",
        "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
        "status": "PENDING",
        "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
        "userName": "김멋사",
        "phoneNumber": "010-1234-5678",
        "createdAt": "2025-08-15T22:07:43.997827",
        "updatedAt": "2025-08-15T22:07:43.997827",
        "rejectionReason": null,
        "rejectionDetail": null,
        "comments": null,
        "commentCount": null
        "summaryLocation": "",
        "summaryPhenomenon": "",
        "summaryProblem": "",
        "summaryRisk": "",
        "summaryRequest": ""
    },
    {
        "id": 3,
        "title": "공원시설의 문제",
        "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
        "address": "해미면",
        "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
        "status": "PENDING",
        "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
        "userName": "김멋사",
        "phoneNumber": "010-1234-5678",
        "createdAt": "2025-08-15T22:05:05.305184",
        "updatedAt": "2025-08-15T22:05:05.305184",
        "rejectionReason": null,
        "rejectionDetail": null,
        "comments": null,
        "commentCount": null
        "summaryLocation": "",
        "summaryPhenomenon": "",
        "summaryProblem": "",
        "summaryRisk": "",
        "summaryRequest": ""
    },
    {
        "id": 2,
        "title": "공원시설의 문제",
        "content": "서산 중앙공원 산책로를 이용하다가 바닥이 심하게 파손된 구간을 발견했습니다. 위치는 중앙공원 내 체육시설을 지나 연못 쪽으로 가는 산책로 중간쯤입니다. 보도블록이 여러 개 들떠 있고, 한 부분은 아예 깨져서 구멍처럼 파여 있어요. 최근 비가 와서 물이 고여있는데, 바닥이 울퉁불퉁해서 지나다니기 불편하고, 특히 밤에는 어두워서 발을 헛디뎌 넘어질까 봐 매우 위험합니다. 아이들이 뛰어다니다 다칠까 봐 걱정도 됩니다.시민들이 안전하게 산책로를 이용할 수 있도록 빠른 시일 내에 보수 부탁드립니다.",
        "address": "해미면",
        "categories": ["FACILITY_DAMAGE", "SAFETY_RISK", "ENVIRONMENT_CLEANING"],
        "status": "PENDING",
        "imageUrls": ["http://localhost:8080/uploads/8647fb9e-0668-43c1-9ed9-377f47edd24d.png"],
        "userName": "김멋사",
        "phoneNumber": "010-1234-5678",
        "createdAt": "2025-08-15T18:48:26.550027",
        "updatedAt": "2025-08-15T18:48:26.550027",
        "rejectionReason": null,
        "rejectionDetail": null,
        "comments": null,
        "commentCount": null
        "summaryLocation": "",
        "summaryPhenomenon": "",
        "summaryProblem": "",
        "summaryRisk": "",
        "summaryRequest": ""
    }
]
```

---

## 민원 처리율

**GET** `/api/complaints/resolution-rate`

**Response 200**

```json
{
    "ratePercent": 0.0,
    "deltaPercent": 0.0,
    "up": false
}
```

---

## 평균 처리 시간

**GET** `/api/complaints/avg-handle-time`

**Response 200**

```json
{
    "days": 0.0,
    "deltaDays": 0.0,
    "up": false
}
```
