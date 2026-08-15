# LAN-218 편지함 사용자 기능 구현 계획

## 범위

- 사용자 피드백 등록과 보낸 편지 조회.
- 게시된 공지·업데이트와 사용자별 답장 조회.
- 상세 조회 시 자동 읽음 처리와 안 읽은 편지 개수 조회.
- 기존 NPS API는 동작을 유지하고 `DEPRECATED` 주석만 추가.

이번 이슈에서 어드민 작성·일괄 처리 API, 이미지 업로드, 푸시 알림, 사용자 수정·삭제는 구현하지 않는다.

## API

```text
POST /api/v1/mailbox/feedbacks
GET  /api/v1/mailbox/sent?cursor={opaque}&size=20
GET  /api/v1/mailbox/sent/{feedbackId}
GET  /api/v1/mailbox/received?cursor={opaque}&size=20
GET  /api/v1/mailbox/received/{letterId}
GET  /api/v1/mailbox/unread-count
```

- 목록은 기본 20건, 최대 100건의 `Base64 URL-safe` 커서 페이지네이션을 사용한다.
- 보낸 편지는 `(createdAt, feedbackId)` 최신순이다.
- 받은 편지는 `pinned DESC, sentAt DESC, letterId DESC` 순이다.
- 미리보기는 전체 문자열을 반환하고 2줄 말줄임은 클라이언트가 처리한다.
- 접근할 수 없는 상세는 존재 여부를 노출하지 않고 `RESOURCE_NOT_FOUND`로 응답한다.

## 데이터 모델

- `mailbox_letter`: 공지·업데이트·답장 본문과 게시 상태를 저장한다.
- `mailbox_feedback`: 사용자 피드백 원문, 유형, 처리 상태를 저장한다.
- `mailbox_letter_recipient`: 사용자별 답장 전달과 읽음 상태를 저장한다.
- `mailbox_letter_read`: 전역 공지·업데이트의 사용자별 읽음 상태를 저장한다.

enum 값은 문자열로 저장한다.

- `MailboxLetterType`: `NOTICE`, `UPDATE`, `REPLY`.
- `MailboxPublicationStatus`: `DRAFT`, `PUBLISHED`, `UNPUBLISHED`.
- `UserFeedbackType`: `BUG_REPORT`, `FEATURE_REQUEST`, `QUESTION`, `CHEER`.
- `UserFeedbackStatus`: `PENDING`, `COMPLETED`.

공지·업데이트 본문은 `JSON` 블록, 답장은 일반 텍스트로 저장한다. 답장은 `mailbox_letter_recipient`에 지정된 사용자만 조회할 수 있다.

## 구현 순서

1. Flyway 스키마, JPA Entity와 Repository를 추가한다.
2. 피드백 등록과 보낸 편지 목록·상세 API를 추가한다.
3. 받은 편지 목록·상세, 자동 읽음, 안 읽은 편지 개수 API를 추가한다.
4. 기존 NPS 회귀 테스트와 전체 Gradle 검증을 수행한다.

## 검증

- [x] 편지함 4개 테이블과 기존 `nps_response` 테이블 공존 확인.
- [x] 피드백 검증·인증·소유권·커서 페이지네이션 확인.
- [x] 전역 게시물 공개 범위와 사용자별 답장 격리 확인.
- [x] 고정 우선 정렬, 자동 읽음 멱등성, unread count 확인.
- [x] 편지함 OpenAPI와 기존 NPS API 회귀 확인.
- [x] `./gradlew check --rerun-tasks` 통과.
