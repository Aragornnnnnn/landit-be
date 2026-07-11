# LAN-94 Plan

## 목표

완료된 시나리오 세션의 최종 피드백을 생성하거나 기존 결과를 조회한다. 신규 저장에서만 세션 히스토리와 시나리오 진행도를 확정한다.

## 구현 범위

1. USER First 시작 안내를 `scenario_session.user_opening_instruction_snapshot`에 저장한다.
2. BE가 모든 사용자 메시지 ID와 평가 컨텍스트를 구성해 AI `session-feedback`을 호출한다.
3. AI 응답을 검증하고 summary/message feedback을 저장한다.
4. 최초 저장에서만 히스토리 종료 정보와 진행도를 갱신한다.
5. 기존 결과는 AI 재호출이나 상태 변경 없이 반환한다.

## 구조

- `SessionFeedbackUseCase`는 기존 결과 조회, AI 호출, 응답 조립을 담당한다.
- `SessionFeedbackContextLoader`는 읽기 트랜잭션에서 세션과 평가 컨텍스트를 immutable record로 만든다.
- `SessionFeedbackRecorder`는 저장 트랜잭션과 세션 row lock 안에서 신규 결과를 저장한다.
- AI 호출은 두 DB 트랜잭션 사이에서 실행한다.

## 핵심 규칙

- `COMPLETED` 상태의 `SCENARIO` 세션만 처리한다.
- USER First 첫 발화는 스냅샷 시작 안내를, 그 외 발화는 직전 AI 메시지를 평가 기준으로 사용한다.
- `expectedMessageIds`는 세션 전체 메시지 순서의 모든 USER 메시지 ID다.
- AI 응답의 session ID, 메시지 ID 순서, 점수·별점 구간, 피드백 필수 필드를 검증한다.
- AI `MESSAGE_FEEDBACK_NOT_READY`는 FE `FEEDBACK_NOT_READY`로 변환한다.
- 동시 요청의 AI 호출 1회는 보장하지 않지만, DB 저장과 진행도 부작용은 세션 lock과 기존 summary 재조회로 한 번만 반영한다.

## 검증

- API 통합 테스트는 AI First 신규 생성·순차 재조회, USER First 시작 안내, 미완료 세션, 잘못된 AI 결과를 확인한다.
- 원격 AI 클라이언트 테스트는 요청 직렬화와 409·502·503 변환을 확인한다.
- `UserScenarioProgressTest`는 최초 클리어와 최고 성과 갱신을 확인한다.
- 최종 명령은 `./gradlew test`와 `git diff --check feat/LAN-93...HEAD`다.
