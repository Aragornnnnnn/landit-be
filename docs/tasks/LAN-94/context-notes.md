# LAN-94 Context Notes

## 결정

- 최종 피드백은 `POST /api/v1/sessions/{sessionId}/feedback`으로 생성하거나 조회한다.
- BE-AI `POST /api/v1/conversation/session-feedback` 계약은 `sessionId`, `scenario`, `expectedMessageIds`를 유지한다.
- FE 응답의 `evaluationContext`는 BE가 저장 메시지와 세션 스냅샷으로 구성한다.
- USER First 첫 발화에는 세션 시작 시 저장한 시작 안내 스냅샷을 사용한다.
- AI 호출은 트랜잭션 밖에서 실행한다. 저장 트랜잭션은 learning session lock과 summary 재조회로 DB 부작용만 멱등하게 처리한다.
- 별점은 AI 응답값이 아니라 유효한 `nativeScore` 구간으로 BE가 계산한다. AI 별점이 다르면 경고 로그를 남기고 BE 계산값을 저장한다.
- AI 캐시의 단일 인스턴스·3시간 TTL·AI 성공 후 BE 저장 실패 복구는 이번 범위 밖이다.

## 결과 확정 정책

- 메시지 제출 완료는 `learning_session`만 완료한다.
- 최종 피드백 신규 저장 때 `session_history`의 종료 시각, duration, USER 메시지 수를 확정한다.
- 같은 시점에 `user_scenario_progress`를 `CLEARED`로 바꾸고 완료 횟수, 최초 클리어 시각, 최근 플레이 시각, 최고 성과를 갱신한다.
- 기존 summary가 있으면 AI를 호출하거나 위 값을 다시 바꾸지 않는다.

## 검증 기록

- 중복된 Loader·Recorder 통합 테스트와 단순 팩토리 테스트를 제거하고 API·원격 클라이언트·진행도 테스트만 유지했다.
- 중복 상태 검증과 반복된 USER 메시지 ID 조회를 공통화해 동작 변경 없이 구현량을 줄였다.
- 최종 피드백의 조회·저장·검증 흐름에는 메서드 역할과 트랜잭션 의도를 설명하는 주석을 추가했다.
- H2 Flyway V15 적용과 전체 `./gradlew test`를 확인했다.
- `git diff --check feat/LAN-93...HEAD`를 확인했다.
- AI 별점 불일치에도 `nativeScore=90` 기준 `3.0`을 저장하고 반환하는 회귀 테스트를 추가했다.
- 리뷰 가독성을 위해 최종 피드백 컨텍스트 조회의 `var`를 명시 타입으로 바꾸고, 원격 AI 호출의 기본 오류 코드를 `defaultErrorCode`로 명명했다.
- PostgreSQL 실행 환경이 없어 실제 V15 적용은 아직 확인하지 못했다.
