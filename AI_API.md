# 간편민원서비스 **AI** API 명세서
---
- [USER API 명세서](USER_API.md)
- [ADMIN API 명세서](ADMIN_API.md)
---

> Base URL: `http://localhost:8080`
> 공통 헤더(모든 관리자 요청): `PASSWORD: hanseo`

---

## AI 요약

**POST** `/api/admin/complaints/{id}/ai-summary`

**설명**: 지정된 민원 ID의 내용과 사진을 받아 AI가 요약하여 반환합니다.

**Request Body**

```json
{}
```

※ 요청 바디 없이 호출 가능하며, 서버에 저장된 민원 내용을 AI가 분석 후 요약합니다.

**200 Response (예시)**

```json
{
  "location": "서산 중앙고우언 내,체육시설 지나 연못 쪽으로 가는 산책로 중간",
  "phenomenon": "보도블록 여러 곳이 들떠 있고 일부는 깨져 구멍이 생김 (바닥 심각 파손)",
  "problem": "최근 우천으로 물이 고이고 바닥이 울퉁불퉁하여 보행 불편 및 낙상 위험 발생",
  "risk": "야간 조명 미비 시 보행자(특히 어린이) 안전사고 우려",
  "request": "시민 안전을 위한 긴급 보수 및 임시 안전조치 요청"
}
```
