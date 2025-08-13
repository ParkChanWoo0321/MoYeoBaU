# 간편민원서비스 **AI** API 명세서
---
- [USER API 명세서](USER_API.md)
- [ADMIN API 명세서](ADMIN_API.md)
---
> Base URL: `http://localhost:8080`

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
  "summary": "도로 쓰레기 치우세요."
}
```
