
---
- [USER API 명세서](USER_API.md)
- [ADMIN API 명세서](ADMIN_API.md)
---
> Base URL: `http://localhost:8080`

---

## AI 요약 생성

**POST** `/api/admin/complaints/{id}/ai-summary`

**설명**: 지정된 민원 ID의 내용을 AI가 요약하여 반환합니다.

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

**비고**

* 프론트엔드는 응답의 `summary`를 요약 표시 영역에 바로 반영하면 됩니다.
* AI 요약 내용은 원문과 다를 수 있으니, 원문과 함께 제공하는 것이 권장됩니다.
* 응답 속도는 AI 처리 시간에 따라 다소 지연될 수 있습니다.


