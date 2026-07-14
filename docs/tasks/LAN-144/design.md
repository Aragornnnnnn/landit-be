# LAN-144 다음 메시지 조기 응답 설계

## 목표

일반 대화 턴에서 `landit-ai`가 생성한 최종 `aiMessage`가 준비되면 속마음과 메시지 피드백 완료를 기다리지 않고 프론트에 응답한다. 프론트는 제출된 사용자 메시지의 속마음 상태를 1초 간격으로 조회한다.

## 책임 경계

- `landit-ai`는 상태를 저장하지 않는 동기 API 두 개를 제공한다.
  - `POST /api/v1/conversation/next-message`는 `aiMessage`, `translatedMessage`, `goalCompletionStatus`를 반환한다.
  - `POST /api/v1/conversation/inner-thought`는 `innerThought`, `innerThoughtType`을 반환한다.
- `landit-be`는 일반 턴에서 다음 메시지, 속마음, 기존 메시지 피드백 요청을 동시에 시작한다.
- `landit-be`는 다음 메시지만 기다려 저장하고 응답한다. 속마음 완료·실패는 별도 트랜잭션으로 사용자 메시지에 반영한다.
- 프론트는 `landit-be`의 속마음 조회 API만 폴링한다.

## API 계약

사용자 메시지 제출 응답의 `submittedMessage`에는 `innerThoughtProcessingStatus`를 추가한다. 일반 턴의 최초 응답은 `PREPARING`이며 `innerThought`, `innerThoughtType`은 `null`이다. 종료 턴은 기존 `closing-message` 결과를 함께 저장하므로 최초 응답부터 `COMPLETED`와 속마음 값을 반환한다.

속마음 조회 API는 `GET /api/v1/sessions/{sessionId}/messages/{messageId}/inner-thought`로 제공한다. 응답은 `processingStatus`, `innerThought`, `innerThoughtType`을 포함하고, `PREPARING`과 `FAILED`에서는 내용 필드를 `null`로 반환한다.

`landit-ai`의 `inner-thought` 요청은 `sessionId`, `submittedMessageId`, `submittedTurnNumber`, `scenario`, 비어 있지 않은 `conversationHistory`를 받는다. 마지막 히스토리는 요청 식별자와 일치하는 `USER` 메시지여야 한다. 응답의 `sessionId`와 `messageId`는 AI 서버가 요청값으로 조립하고 OpenRouter에는 속마음 두 필드만 생성시킨다.

## 상태와 실패 처리

- 사용자 메시지를 저장할 때 속마음 상태를 `PREPARING`으로 기록한다.
- 속마음 저장 성공 시 `COMPLETED`, AI 호출 또는 응답 검증 실패 시 `FAILED`로 변경한다.
- `PREPARING`이 메시지 생성 시점부터 90초를 넘으면 조회 시 `FAILED`로 전환한다.
- 속마음과 메시지 피드백 실패는 이미 성공한 다음 메시지를 롤백하지 않는다.
- 다음 메시지 생성 또는 다음 메시지 저장 실패는 기존처럼 제출 사용자 메시지를 제거한다.
- AI 호출은 부수 효과 없이 재호출할 수 있지만 같은 문구를 보장하지 않는다. 중복 실행 방지와 최초 성공 결과 확정은 BE가 담당한다.

## 데이터 변경

`session_history_message`에 nullable `inner_thought_processing_status`를 추가하고 기존 사용자 메시지는 속마음 값의 존재 여부에 따라 `COMPLETED` 또는 `FAILED`로 보정한다. 신규 사용자 메시지만 `PREPARING`으로 시작하며 AI 메시지는 상태를 `null`로 유지한다. 기존 `ProcessingStatus` enum을 재사용한다.

## 범위 제외

- 종료 턴의 `closing-message` 계약은 변경하지 않는다.
- SSE, WebSocket, SQS, 신규 Worker는 추가하지 않는다.
- AI 서버에 폴링 상태나 결과 저장소를 추가하지 않는다.
- 자동 재시도는 추가하지 않는다.

## 검증

- AI 클라이언트의 분리된 요청·응답과 400, 502, 503 매핑을 검증한다.
- 다음 메시지가 속마음 완료를 기다리지 않고 반환되는지 검증한다.
- `PREPARING → COMPLETED/FAILED`와 90초 만료를 검증한다.
- 속마음 실패 후에도 다음 메시지가 유지되는지 검증한다.
- 종료 턴이 별도 속마음 API를 호출하지 않는지 검증한다.
- 전체 `./gradlew test`를 실행한다.
