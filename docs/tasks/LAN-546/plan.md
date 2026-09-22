# LAN-546 특정 사용자에게 편지 발송과 문의 이미지 첨부

## 범위와 계약

- 관리자가 사용자 ID 1~100개와 제목·일반 텍스트 본문을 지정해 즉시 발송한다.
- 사용자 선택에 따라 이번 작업은 편지함 발송만 제공하고 푸시 알림은 보내지 않는다.
- `POST /api/v1/admin/mailbox/direct-letters`: `userProfileIds`, `title`(최대 200자), `bodyText`를 받고 `letterId`, `recipientCount`, `sentAt`을 반환한다.
- 중복 ID는 400 `INVALID_REQUEST`, 빈 목록·100명 초과·null/비양수 ID·빈 제목/본문은 400 `VALIDATION_FAILED`, 존재하지 않거나 탈퇴한 수신자는 404 `RESOURCE_NOT_FOUND`다. 한 명이라도 유효하지 않으면 전체 발송을 취소한다.
- `DIRECT` 유형을 추가한다. 본문은 `bodyText`, 미리보기는 본문을 사용하고, 피드백 연결 없이 수신자별 행을 저장한다.
- 받은 편지 목록·상세·안 읽은 개수는 수신자에게만 공개하고 최초 읽음 시각을 보존한다. 직접 편지의 `contentBlocks`, `feedbackType`, `quotedFeedbackContent`는 null이다.
- 공지·업데이트 관리 API는 해당 두 유형만 허용한다. 직접 편지를 공지로 바꾸거나 전역으로 공개할 수 없게 한다.
- 편지·수신자·감사 로그는 같은 트랜잭션으로 저장한다. 활성 수신자 프로필을 ID 순서로 잠가 탈퇴와의 상태 변경을 직렬화한다.
- 직접 발송은 `LANDIT_MAILBOX_DIRECT_LETTER_ENABLED` 기본값 false로 차단하며 API는 503을 반환한다. 앱의 `MailboxLetterType`·배지 매핑·일반 텍스트 상세 렌더링 지원을 배포하고 확인한 뒤 true로 활성화한다. 공지·업데이트·문의 답장은 이 설정의 영향을 받지 않는다.
- 앱은 `DIRECT`를 일반 텍스트 편지로 렌더링하고, 어드민은 기존 사용자 목록의 ID로 발송 API를 연동해야 한다. 이 저장소의 작업 범위는 BE다.

## 추가 범위: 사용자 문의 이미지

- 사용자가 같은 이슈에서 프론트 → BE → S3 방식의 이미지 첨부 구현을 요청했다.
- 기존 `POST /api/v1/mailbox/feedbacks`의 JSON 계약을 유지한다. 이미지 첨부 요청은 같은 경로에 `multipart/form-data`로 보낸다. `feedback` 파트는 `application/json`의 `{ "type": "QUESTION", "content": "문의 본문" }`, 반복되는 `images` 파트는 이미지 파일이다.
- PNG/JPEG 최대 3장, 장당 5 MiB, 합계 10 MiB, 이미지당 2천만 픽셀로 제한한다. 실제 이미지 디코딩 결과와 MIME을 대조하며 빈 파일·손상·위조 형식·초과 크기는 저장 전에 거부한다. 기존 전체 multipart 요청 상한 11MB와 오디오 파일 상한 10MB를 유지한다.
- 업로드는 DB 트랜잭션 밖에서 수행한다. 문의와 첨부 연결은 별도 트랜잭션으로 확정하고, 업로드 또는 DB commit 실패 시 해당 요청이 업로드를 시도한 객체를 모두 삭제한다. 삭제 자체가 실패하면 객체 키를 로그에 남긴다. 프로세스 강제 종료나 삭제 실패로 남는 미연결 파일의 자동 정리 작업은 이번 구현에 포함하지 않는다.
- 사용자·관리자 문의 상세 응답에 `attachments` 배열을 추가한다. 각 항목은 `attachmentId`, `contentType`, `fileSize`, `downloadUrl`이다. 기존 문의는 빈 배열이다.
- `GET /api/v1/mailbox/feedbacks/{feedbackId}/attachments/{attachmentId}`는 작성자·관리자만 접근할 수 있으며 다른 사용자는 404다. `downloadUrl`은 이 API의 상대 경로이며 프론트가 Bearer 토큰으로 조회한 blob을 표시해야 한다. 공개 이미지 URL이나 S3 키를 반환하지 않는다. 응답은 이미지 바이트와 `Cache-Control: private, no-store`다.
- `S3_BUCKET_NAME`으로 전달되는 환경별 비공개 애플리케이션 버킷을 사용한다. 로컬 IaC `modules/app-platform/main.tf`에서 Public Access Block, API task의 GetObject/PutObject/DeleteObject 권한과 환경 변수 주입을 확인했다. 공개 콘텐츠 버킷 설정은 사용하지 않는다. 운영에 적용된 정책이나 실제 S3 입출력은 아직 검증하지 않았다.
- 이 작업의 미배포 migration에 문의 첨부 테이블도 포함한다. PR 생성 직전 #211의 V118과 충돌을 발견해 최종 번호를 V119로 변경했다. 기존 편지 기능 데이터와 순서 제약을 함께 검증한다.
- [x] 업로드·실패 보상·비공개 조회 API와 상세 응답 추가.
- [x] 첨부 형식·제한·권한·부분 실패와 DB 롤백 테스트.
- [x] 전체 check 및 PostgreSQL 제약 재검증.

프론트 요청 예시:

```bash
curl -X POST "$API_BASE/api/v1/mailbox/feedbacks" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F 'feedback={"type":"QUESTION","content":"화면 오류 문의입니다."};type=application/json' \
  -F 'images=@screen.png;type=image/png'
```

- 전체 `Content-Type`과 multipart boundary는 HTTP 클라이언트가 설정하게 한다. `feedback` 파트의 `application/json`은 반드시 지정한다. 본문 검증 실패·파트 누락은 400, 지원하지 않는 파트 MIME은 415다.
- 이미지 다운로드는 인증 헤더를 붙인 별도 요청으로 처리한다. 웹에서는 응답 blob의 object URL을 표시하고 사용 후 해제한다. `downloadUrl`을 인증 없이 `<img src>`에 바로 넣는 방식은 지원하지 않는다.

이미지 첨부 검증 기록:

- 최종 `./gradlew spotlessApply check` 성공. Spotless·Checkstyle·전체 테스트를 실행했고 JUnit XML 집계 1,371개, 실패·오류 0개, skip 9개다.
- 통합 테스트 7개: JSON·multipart 호환, PNG/JPEG 순서와 상세 응답, 작성자·관리자 조회, 다른 사용자·비인증 차단, 잘못된 파일·본문·파트 형식, 업로드 부분 실패 및 삭제 실패, commit 준비 단계 예외의 DB 롤백·객체 정리, OpenAPI의 두 요청 형식을 확인했다.
- 이미지 검증 단위 테스트 3개: 장당 5 MiB·합계 10 MiB와 3장 경계 허용, 2천만 픽셀 초과 조기 거부, 보고된 크기를 신뢰하지 않는 실제 스트림 상한을 확인했다.
- S3 adapter 단위 테스트 4개: 비공개 버킷·ACL 미지정·바이트 보존, 제한된 Range 조회·삭제 대상, SDK 오류 상세 비노출, 설정 누락 시 호출 차단을 확인한다. S3는 mock이므로 실제 AWS 권한과 저장 성공을 증명하지 않는다.
- PostgreSQL 15.18에서 실제 V119에 포함된 첨부 테이블을 생성하고, 이미지 형식·크기·순서·객체 키 중복·외래 키 위반 8건의 거부와 문의 삭제 시 첨부 메타데이터 연쇄 삭제를 확인했다. 기존 직접 편지 검증도 함께 통과했다. 재검증은 전용 schema에서 수행한 뒤 해당 schema를 삭제했다.

## 구현과 검증

- [x] 저장 제약·수신자 모델·프로필 잠금 계약 확장.
- [x] 관리자 발송 API·감사 기록·OpenAPI 반영.
- [x] 사용자 조회·읽음·공개 범위 및 기존 관리 API 보호.
- [x] 통합 테스트와 `./gradlew check`. V118로 번호를 변경한 뒤 최종 전체 검사도 통과했다.
- [x] 별도 PostgreSQL 제약 검증. 임시 PostgreSQL 15.18에서 실제 V53·V54·V119와 검증 전용 V122 SQL을 실행했다. 기존 데이터 보존, 직접 편지 수신 정보, 제약 위반 거부와 읽음 상태 분리를 확인했다.

검사 기록:

- 새 직접 편지 통합 테스트 4개: 수신자별 비공개 조회와 최초 읽음 보존, 푸시 미발송, 잘못된 입력의 전체 거부, 관리자 권한과 전역 공개 방지, OpenAPI 계약을 확인했다.
- 기존 공지·문의 답장 테스트를 포함한 관리자 편지함 테스트 35개가 통과했다.
- 첫 전체 검사에서 새 테스트의 Checkstyle 오류 4건을 수정했다. 기존 `RemoteAiFreeTalkClientTest.normalOpeningWaitsBeyondMemoryTimeout`도 실패했다. 해당 테스트의 벽시계 소요 시간은 약 981초로 비정상적으로 길었으며, 코드 변경 없이 전체 재실행에서 통과했다.
- 전체 재실행: Spotless·Checkstyle·테스트 성공. JUnit XML 집계 1,357개, 실패·오류 0개, skip 9개.
- 2026-09-22 PostgreSQL 검증: [재실행용 SQL](verify-mailbox-postgres.sql)을 `psql -X -f`로 실행했다. V53·V54 적용 후 공지·업데이트·답장·문의·수신자·읽음 데이터를 넣고 PostgreSQL용 V119를 적용해 전후 데이터가 동일함을 비교했다.
- 직접 편지와 피드백 없는 수신자 2명 저장, 유형·본문·수신자 중복·외래 키 위반 10건 거부, 반복 읽음의 무변경 및 다른 수신자의 안 읽은 상태 보존을 확인했다. 잘못된 유형은 유형·본문 제약을 동시에 위반하므로 테스트가 특정 제약의 보고 순서를 강제하지 않도록 보완했다.
- 검증은 전용 schema와 사용자 PK만 가진 외부 의존 테이블에서 수행한 SQL 검증이다. PostgreSQL에서 애플리케이션 전체 Flyway 이력이나 JPA/API 경로를 검증한 것은 아니다. V119는 제약을 NOT VALID로 추가하고 커밋하며, V122는 별도 트랜잭션으로 기존 행을 검증한다. 검증 전 제약의 쓰기 보호, `convalidated`의 false→true 전환, 검증 시 `ShareUpdateExclusiveLock` 보유와 `AccessExclusiveLock` 부재도 확인했다.
- 트랜잭션 분리를 검증하기 위해 각 단계를 커밋하고, 마지막에 전용 schema를 삭제했다.

## 마이그레이션

- 2026-09-22 리뷰 수정에서 PostgreSQL용 V119와 별도 V122를 실제 재실행했다. 위 검증 기록은 이 재실행 기준이며 V118은 다른 PR #211의 번호다. H2용 V119는 기존 즉시 검증 SQL을 유지한다.
- 시작 기준 `origin/develop`은 `5346aae7f`, 공용 migration은 V111까지다.
- PR 생성 직전 SSH로 갱신한 `origin/develop`은 `5346aae7f`로 동일하다. 공용·PostgreSQL·H2 runtime migration과 열린 PR #202~#211의 변경 파일을 재확인했다. #204=V112, #205=V113, #206=V114, #207=V115, #208=V116, #210=V117, #211=V118이므로 이 작업은 최종 V119를 사용한다. #202·#203·#209에는 migration 변경이 없다.
- 2026-09-22 재확인: develop은 V111, 열린 PR #213은 V120까지 사용한다. #208 리뷰 수정은 V121을 사용하므로 이 PR의 검증은 V122를 사용한다. `db/migration`·`db/postgresql`·`db/h2` 런타임 전체에서 번호를 확인했다.
- 병합·배포 직전 열린 PR과 대상 DB의 Flyway 이력을 재확인한다. V122 적용 전에 V112~V121을 모두 포함하거나 이미 적용해야 한다. 기존 `outOfOrder=false` 순서를 유지하고, Flyway `group` 기본값 false를 유지해 V119와 V122를 같은 트랜잭션으로 묶지 않는다. 미배포 V119의 PostgreSQL SQL을 수정했으므로 이전 PR 버전을 적용한 개발 DB는 체크섬 차이를 별도로 처리해야 한다. 운영 이력은 조회하거나 변경하지 않았다.
- 운영 DB 적용과 프론트엔드·실기기 검증은 이번 로컬 구현 검증에 포함되지 않는다.

## 리뷰 수정

- 프로필의 활성 수신자 조회는 `ActiveUserProfileIds` record를 반환한다. 내부 목록은 복사해 불변으로 전달하며 기존 필터링과 프로필 잠금은 유지한다.
- DIRECT 지원이 없는 현재 프런트와의 호환을 위해 기본 비활성 발송 설정과 503 OpenAPI 응답을 추가했다. 활성 상태의 기존 API 통합 테스트와 비활성 상태의 부수 효과 차단 테스트를 함께 실행한다.

- 2026-09-22 리뷰 수정 후 `./gradlew spotlessApply check --offline --no-daemon --console=plain` 통과. Spotless·Checkstyle 포함, JUnit 1,372개, 실패·오류 0개, 환경 조건 생략 9개. `git diff --check` 통과. 운영 배포와 실기기 검증은 수행하지 않았다.
