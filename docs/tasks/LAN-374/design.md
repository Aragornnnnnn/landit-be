<!-- LAN-374 어드민 피드백 상세 조회 API의 승인된 계약을 정의한다. -->
# LAN-374 어드민 피드백 상세 조회 설계

## 목표

어드민이 `feedbackId`로 사용자 피드백 상세와 연결된 가장 최근 답장을 한 번에 조회할 수 있게 한다.

## API 계약

- `GET /api/v1/admin/mailbox/feedbacks/{feedbackId}`를 추가한다.
- 응답은 기존 어드민 피드백 목록 항목의 피드백·사용자 정보와 nullable `reply`를 포함한다.
- `reply`는 `letterId`, `title`, `bodyText`, `sentAt`을 포함한다.
- 답장이 없으면 `reply`는 `null`이다.
- 여러 답장이 있으면 `publishedAt DESC, id DESC` 기준의 가장 최근 답장을 반환한다.
- 존재하지 않는 `feedbackId`는 `RESOURCE_NOT_FOUND`로 응답한다.
- 어드민 피드백 검색 응답에는 `replyLetterId`를 추가하지 않는다. 상세 화면은 목록의 `feedbackId`로 진입한다.

## 답장 연결 정책

- 대표 피드백은 자신의 `feedbackId`를 canonical ID로 사용한다.
- 비대표 피드백 상세는 canonical `resolvedByFeedbackId`와 요청 `feedbackId` 양쪽에 연결된 답장을 후보로 사용한다.
- 두 후보에 연결된 답장 중 `publishedAt DESC, id DESC` 기준의 최신 한 건을 반환한다.
- 기존 답장 작성 경로가 완료된 비대표 피드백에 추가 답장을 요청 `feedbackId`로 직접 연결할 수 있기 때문에 두 후보를 함께 조회한다.
- 답장 조회에는 피드백의 `userProfileId`도 사용해 사용자별 연결을 제한한다.
- `mailbox_letter_recipient.representative_feedback_id`와 `mailbox_letter.id`를 통해 답장을 조회한다.

## 제외 범위

- 답장 작성·수정·삭제 API 변경.
- 전체 답장 이력 조회.
- 기존 피드백 검색 조건·응답 변경.
- DB 스키마 변경.
- 기존 답장 작성 동작 변경.

## 완료 기준

- 미답변 피드백 상세에서 `reply: null`을 반환한다.
- 답변된 대표 피드백 상세에서 가장 최근 답장을 반환한다.
- 비대표 피드백 상세에서도 canonical ID와 요청 ID 양쪽에 연결된 답장 중 가장 최근 답장을 반환한다.
- 존재하지 않는 피드백은 `RESOURCE_NOT_FOUND`를 반환한다.
- OpenAPI 계약과 통합 테스트가 반영되고 `./gradlew check`가 통과한다.
