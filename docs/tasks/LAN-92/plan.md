# LAN-92 사용자 발화 제출 구현 계획

## 목표

- `POST /api/v1/sessions/{sessionId}/messages` API로 사용자 메시지를 저장하고, AI 다음 메시지 또는 종료 메시지를 생성해 저장한 뒤 응답한다.

## 구현 방향

- 세션 시작과 종료 UseCase는 그대로 두고, 메시지 제출은 `SessionMessageSubmitUseCase`로 분리한다.
- AI 호출은 외부 의존성이므로 `AiConversationClient` 인터페이스 뒤에 둔다.
- 통합 테스트는 MockMvc, JdbcTemplate, 테스트용 AI 클라이언트 빈으로 API와 DB 변경을 실제로 검증한다.

## 주요 정책

- 사용자 입력 타입은 현재 코드의 `TEXT`, `VOICE`, `GENERATED` enum을 유지한다.
- 사용자 메시지는 일반적으로 `VOICE`로 들어오지만, API는 현재 enum 값을 기준으로 검증한다.
- 다음 고정 질문이 있으면 AI `next-message`를 호출한다.
- AI `next-message`가 `COMPLETED`를 반환해도 다음 고정 질문이 남아 있으면 세션을 계속 진행한다.
- 다음 고정 질문이 없으면 최대 턴 도달로 보고 `closing-message`를 호출한다.
- 종료 메시지도 FE 응답의 `nextMessage`에 담아 반환한다.
- 메시지별 피드백 생성 요청은 이번 이슈 범위에서 제외하고, 제출 메시지의 `feedbackProcessingStatus`는 `PREPARING`으로 응답한다.

## 작업 순서

1. 작업 문서와 체크리스트를 생성한다.
2. 사용자 발화 제출 성공 통합 테스트를 먼저 추가하고 실패를 확인한다.
3. API DTO, Controller, UseCase, Repository 조회 메서드를 최소 구현한다.
4. AI next-message 호출과 다음 AI 메시지 저장을 구현한다.
5. 최대 턴 종료 메시지 흐름과 목표 완료 응답 시 계속 진행하는 흐름을 테스트로 고정한다.
6. 권한, 완료 세션, 빈 메시지, AI 실패 오류를 테스트로 고정한다.
7. 전체 테스트와 diff 검사를 실행한다.
