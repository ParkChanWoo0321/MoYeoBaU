🌐 민원 시스템 API 명세서
Base URL: http://localhost:8080

1. 개인 (사용자) 기능
1.1 대시보드 (전체 요약/통계)
📌 대시보드 통계 조회
GET /api/complaints/dashboard

설명: 전체 민원 수, 처리율, 카테고리별 비율/현황 등 반환

응답 예시

json
복사
편집
{
  "totalCount": 34,
  "completedRate": 61.8,
  "categoryCounts": {
    "ENVIRONMENT": 12,
    "ROAD": 10,
    "FACILITY": 7,
    "OTHER": 5
  },
  "categoryRates": {
    "ENVIRONMENT": 35.3,
    "ROAD": 29.4,
    "FACILITY": 20.6,
    "OTHER": 14.7
  }
}
1.2 민원 목록 & 단건 조회
📌 전체 민원 목록 조회
GET /api/complaints

설명: 전체 민원 목록(익명)

📌 내 민원 목록 (본인확인)
GET /api/complaints/my?userName=이름&phoneNumber=전화번호

설명: 이름+번호로 내 민원만 조회

📌 내 민원 상세 조회 (본인확인)
GET /api/complaints/my/{id}?userName=이름&phoneNumber=전화번호

설명: 이름+번호로 특정 민원 단건 상세 조회

1.3 민원 처리 상태별 목록
📌 민원 상태별 목록 조회
GET /api/complaints/status?status=COMPLETED

Query Param

status: PENDING, IN_PROGRESS, COMPLETED, RECEIVED

설명: 상태별로 전체 민원 목록 조회

1.4 민원 접수 (등록)
📌 민원 등록
POST /api/complaints

Body (JSON)

json
복사
편집
{
  "userName": "홍길동",
  "phoneNumber": "01012345678",
  "content": "도로에 쓰레기가 많아요",
  "address": "서산시 해미면",
  "latitude": 36.7845,
  "longitude": 126.4532,
  "category": "ENVIRONMENT",
  "imageUrl": "http://example.com/photo.jpg"
}
설명: 이름/번호/내용/주소/사진 등 입력, 익명 접수

1.5 내 민원 수정/삭제 (본인확인)
📌 내 민원 수정
PATCH /api/complaints/my/{id}

Body (JSON): 등록과 동일

설명: 본인확인 후 민원 내용/사진 등 수정

📌 내 민원 삭제
DELETE /api/complaints/my/{id}?userName=이름&phoneNumber=전화번호

설명: 본인확인 후 해당 민원 삭제

2. 관리자 기능
모든 관리자 API는 Spring Security 인증 필요
(ID, PW: application.properties에 설정한 계정으로 로그인)

2.1 관리자 대시보드
📌 관리자 전체 민원 목록
GET /api/admin/complaints

설명: 전체 민원 목록 (관리자 권한)

📌 상태별 민원 목록
GET /api/admin/complaints/status?status=IN_PROGRESS

설명: 특정 상태의 민원 목록

📌 카테고리별 통계
GET /api/admin/complaints/stats

설명: 카테고리별 민원 건수 통계 (확장 가능)

2.2 관리자 민원 상세/수정/삭제
📌 관리자 민원 수정
PATCH /api/admin/complaints/{id}

Body (JSON): 등록과 동일

설명: 관리자 권한으로 어떤 민원이든 수정

📌 관리자 민원 삭제
DELETE /api/admin/complaints/{id}

설명: 관리자 권한으로 민원 삭제

📌 민원 상태변경 (관리자)
PATCH /api/admin/complaints/{id}/status

Body (JSON)

json
복사
편집
{ "status": "IN_PROGRESS" }
설명: 민원 상태 (미처리/처리중/완료 등) 변경

2.3 리포트, 지역별 통계
📌 월간/일간/지역별 리포트
GET /api/admin/complaints/report?address=서산시해미면&from=2024-08-01&to=2024-08-31

설명: 기간+지역별 민원 리포트 반환
(address, from, to 파라미터 활용)

📌 관리자 대시보드 통계
GET /api/admin/complaints/dashboard

설명: 관리자 대시보드 통계 (확장 가능)

3. 예외 및 기타 응답 예시
모든 에러/예외 응답(통일)

json
복사
편집
{
  "success": false,
  "message": "알 수 없는 오류가 발생했습니다: ..."
}
(성공시 응답 body 구조는 API별로 상단 참조)

4. Enum 값
status:

PENDING (접수)

IN_PROGRESS (처리중)

COMPLETED (처리완료)

RECEIVED (접수확인)

category:

ENVIRONMENT, ROAD, FACILITY, OTHER 등

5. 보안 및 인증
/api/admin/* API는 Basic Auth 필요
(application.properties에서 지정한 admin 계정 사용)

일반 사용자는 로그인 없이 이용,
수정/삭제/조회시 본인정보(userName, phoneNumber) 필요

6. 기타
사진 업로드는 현재 imageUrl(링크) 형태로 받음 (파일업로드 미구현)

AI 요약, 지도(위도/경도 기반 검색) 등 확장기능은 추후 구현 예정

