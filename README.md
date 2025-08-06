🚩 간편 민원 관리 시스템 API 명세 (README용)
📝 프로젝트 개요
목적: 충남 서산시 해미면 등 지역의 생활민원을 쉽고 빠르게 등록/관리

대상: 로그인 없는 익명 민원 등록(개인), 관리자는 별도 인증 후 처리

주요 특징:

익명 민원 등록(본인확인: 이름+번호)

카테고리/위치별 통계 및 시각화, 리포트 발행

관리자 암호(아이디/비번)로 전체 관리

📚 주요 기능 요약
👤 개인(사용자)
대시보드(전체요약)

전체 민원수

전체 민원목록

민원 내용/처리상태 확인(익명)

민원 처리율/카테고리별 비율/카테고리별 현황 시각화

민원 접수

이름/폰번호 입력(본인확인용), 동네 선택, 사진 등록(링크), 내용 작성

접수 완료

민원 등록 결과 반환

내역 보기(본인확인)

이름/번호 입력 → 내가 접수한 민원 확인/수정/삭제 가능

🛠️ 관리자
관리자 인증(아이디/비번)

지정된 한 계정으로만 접근(스프링 시큐리티 Basic Auth)

대시보드(전체요약)

전체 민원 목록/통계, 카테고리/지역별 현황, 처리율 등

우선순위별 지역 목록화

지역/카테고리별 민원 리스트

카테고리별 민원 상세

월간/일간/지역별 리포트 발행

주소·기간 기준 필터링

민원 상태 변경

미처리/처리중/처리완료 변경

민원 수정/삭제

관리자 권한으로 모든 민원 내용/상태 관리

AI 요약 내용 (확장 예정)

📑 API 상세 명세
1. 대시보드/통계
메서드	URL	설명/권한
GET	/api/complaints/dashboard	대시보드 통계 (개인)
GET	/api/admin/complaints/dashboard	대시보드 통계 (관리자)
GET	/api/complaints	전체 민원 목록 (개인)
GET	/api/admin/complaints	전체 민원 목록 (관리자)

2. 민원 목록/상세/처리상태
메서드	URL	설명/권한
GET	/api/complaints/my?userName=&phoneNumber=	내 민원 목록(본인확인)
GET	/api/complaints/my/{id}?userName=&phoneNumber=	내 민원 단건(본인확인)
GET	/api/complaints/status?status=	상태별 민원 목록 (개인)
GET	/api/admin/complaints/status?status=	상태별 민원 목록 (관리자)
GET	/api/admin/complaints/stats	카테고리별/지역별 통계 (관리자)
GET	/api/admin/complaints/report?address=&from=&to=	월간/일간/지역별 리포트 (관리자)

3. 민원 등록/수정/삭제
메서드	URL	설명/권한
POST	/api/complaints	민원 등록(익명)
PATCH	/api/complaints/my/{id}	내 민원 수정(본인확인)
DELETE	/api/complaints/my/{id}?userName=&phoneNumber=	내 민원 삭제(본인확인)
PATCH	/api/admin/complaints/{id}	관리자 민원 수정
DELETE	/api/admin/complaints/{id}	관리자 민원 삭제
PATCH	/api/admin/complaints/{id}/status	관리자 상태 변경

4. 요청/응답 예시
📌 민원 등록 요청 예시 (POST)
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
📌 민원 상태변경(관리자) 예시 (PATCH)
json
복사
편집
{ "status": "IN_PROGRESS" }
📌 응답(공통 성공/실패)
json
복사
편집
// 성공
{
  "success": true,
  "data": { ... }
}
// 실패
{
  "success": false,
  "message": "에러 메시지"
}
🔒 관리자 인증
/api/admin/* 엔드포인트는 HTTP Basic Auth(ID/PW: application.properties 등록)

(예시)

pgsql
복사
편집
spring.security.user.name=likelion
spring.security.user.password=hanseo
✅ 테스트 체크리스트
 대시보드, 통계 (개인/관리자)

 전체/내 민원 목록, 상세, 본인확인

 민원 등록(위치, 사진 포함)

 본인 민원 수정/삭제 (본인확인)

 관리자의 전체 민원 목록/상세/수정/삭제/상태변경

 카테고리/지역/기간별 통계/리포트

 예외 응답 (필수값 누락, 권한없음, 본인아님 등)

 모든 API Postman 등으로 실전 테스트 완료

📝 기타/확장
사진 업로드는 현재 imageUrl(링크)만 지원

AI 요약 기능(민원 내용 요약)은 추후 구현 예정

Frontend 연동/지도 기능 등은 후속 작업으로 진행
