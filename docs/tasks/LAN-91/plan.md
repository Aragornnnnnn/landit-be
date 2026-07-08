# LAN-91 시나리오 세션 시작/종료 구현 계획

## 목표

`POST /api/v1/scenarios/{scenarioId}/sessions`로 시나리오 세션을 시작하고, `PATCH /api/v1/sessions/{sessionId}/end`로 진행 중인 세션을 사용자 중도 종료 처리한다.

## 구현 방향

- SayNow BE의 세션 시작 흐름을 참고해 사용자, 시나리오, 잠금 상태를 검증한 뒤 세션과 진행도를 생성한다.
- Landit은 AI first와 USER first가 공존하므로 `firstSpeaker`에 따라 AI 시작 메시지를 저장하거나 사용자 시작 안내만 반환한다.
- 다음 AI 메시지 생성 연동은 범위 밖으로 두고, 첫 AI 메시지는 `scenario_language_variant`의 시작 데이터만 사용한다.
- 진행도 응답은 명세 수정에 맞춰 `totalQuestionCount`와 `completed`만 내려준다.

## 작업 순서

1. LAN-91 통합 테스트를 먼저 작성하고 실패를 확인한다.
2. 세션 시작에 필요한 저장소, DTO, 서비스, 컨트롤러를 추가한다.
3. 세션 종료에 필요한 소유자 검증과 상태 전이를 구현한다.
4. Swagger annotation과 공통 오류 코드를 보강한다.
5. LAN-91 관련 테스트와 전체 테스트를 실행한다.
