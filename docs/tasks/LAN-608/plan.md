# LAN-608 개인 편지 발송 목록·상세 조회

## 범위와 계약

- 기존 직접 편지 발송 API에 관리자용 조회 API 2개를 추가한다. 발송·푸시 동작과 DB 스키마는 변경하지 않는다.
- 관리자 권한을 요구한다. 비인증은 401, 일반 사용자는 수신자 본인이어도 403이다.
- 발송된 `DIRECT`만 노출한다. 공지·업데이트·문의 답장은 제외한다.

### 목록

`GET /api/v1/admin/mailbox/direct-letters?page=0&size=20`

- `page`는 0 이상, `size`는 1~100이다. 기본값은 각각 0과 20이며 잘못된 범위는 400 `VALIDATION_FAILED`다.
- 응답 `data`: `items`, `page`, `size`, `totalElements`, `totalPages`.
- `items` 항목: `letterId`, `title`, `sentAt`, `recipientCount`.
- `sentAt DESC, letterId DESC`로 정렬한다. 100명에게 함께 발송해도 목록에는 1건이다.
- 수신자 수는 현재 활성 여부와 관계없이 저장된 수신 이력을 집계한다. 본문은 목록에 반환하지 않는다.
- 빈 목록과 마지막 페이지 이후 요청은 200과 빈 `items`를 반환한다.

### 상세

`GET /api/v1/admin/mailbox/direct-letters/{letterId}`

- 응답 `data`: `letterId`, `title`, `bodyText`, `sentAt`, `recipientCount`, `recipients`.
- `recipients` 항목: `userProfileId`, `readAt`. 사용자 ID 오름차순이며 발송 후 탈퇴한 사용자도 포함한다.
- `readAt`은 최초 읽음 시각이며 미열람이면 명시적인 `null`이다. 관리자 조회는 읽음 상태를 변경하지 않는다.
- 수신자 이름·이메일·발송자 정보는 이번 응답에 포함하지 않는다.
- 없는 ID 또는 발송된 직접 편지가 아닌 ID는 404 `RESOURCE_NOT_FOUND`다.

## 구현과 검증

- 목록은 수신자 수를 집계한 페이지 쿼리로 조회해 수신자 수에 따른 중복과 발송 건별 추가 쿼리를 피한다.
- 상세는 편지와 수신 이력을 읽기 전용 트랜잭션에서 조회한다.
- OpenAPI에 페이지·정렬·권한·오류·읽음 계약을 반영한다.
- 2026-10-02 관리자 편지함 통합 테스트 39개 통과. 신규 4개 테스트에서 발송 건별 페이지·정렬·집계, 탈퇴 수신자 보존, 최초 읽음·미열람 유지, 관리자 권한, 타 유형·없는 ID의 404, 페이지 범위·기본값을 검증했다. 기존 OpenAPI 테스트도 조회 계약으로 확장했다.
- 최종 `./gradlew spotlessApply check --console=plain` 통과. Spotless·Checkstyle·아키텍처 검사 포함, JUnit 1,905건 중 성공 1,893건, 조건부 제외 12건, 실패·오류 0건이다.
- 첫 전체 검사에서 새 코드의 Checkstyle 오류 7건을 수정한 뒤 전체 검사를 다시 통과했다. 테스트 동작 실패는 없었다.
- `git diff --check` 통과. API·쿼리 검증은 로컬 H2 기반이며 운영 PostgreSQL이나 배포 상태를 검증한 결과는 아니다.
- FE 화면 구현, 앱의 `DIRECT` 표시 지원, 운영 배포와 실기기 검증은 이 작업에 포함하지 않는다.
