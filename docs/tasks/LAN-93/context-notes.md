# LAN-93 Context Notes

## 확정된 정책

- 작업 브랜치는 `feat/LAN-100`을 기준으로 생성한 `feat/LAN-93`이다.
- 사용자 메시지를 먼저 저장해 `messageId`를 확보한 뒤 AI 요청을 수행한다.
- AI 요청은 다음 메시지 또는 종료 메시지 생성 후 메시지별 피드백 요청 순서로 실행한다.
- 메시지별 피드백 요청을 BE에서 자동 재시도하지 않는다.
- AI 호출 실패는 FE에 `503 AI_GENERATION_FAILED`로 반환한다.
- AI 응답 형식 또는 식별자 검증 실패는 FE에 `502 AI_RESPONSE_INVALID`로 반환한다.
- 두 AI 호출의 공개 에러 코드는 구분하지 않고 내부 로그의 workflow로 구분한다.
- 오류 발생 시 먼저 저장한 사용자 메시지를 제거하고 다음 AI 메시지는 저장하지 않는다.
- FE는 명확한 `502` 또는 `503` 응답을 받았을 때 메시지 제출 API 전체를 재시도한다.
- AI First와 USER First 모두 기존 `/api/v1/conversation/message-feedback`을 사용한다.
- 요청은 최상위 `evaluationContext`와 `userMessage`로 평가 기준과 평가 대상을 구분한다.
- 평가 컨텍스트 타입은 `AI_MESSAGE`, `SCENARIO_OPENING_INSTRUCTION`을 사용한다.
- AI First는 직전 AI 메시지를 평가 컨텍스트로 전달한다.
- USER First 첫 사용자 메시지는 시나리오 시작 안내를 평가 컨텍스트로 전달한다.
- USER First 첫 사용자 메시지도 피드백 요청이 정상 접수되면 `PREPARING`을 반환한다.
- USER First 시나리오에 시작 안내가 없으면 재시도로 해결되지 않는 콘텐츠 설정 오류이므로 `500 INTERNAL_SERVER_ERROR`를 반환한다.
- `messageSequence`는 턴 내부 순번이 아니라 세션 전체 메시지 순번이다.
- `SCENARIO_OPENING_INSTRUCTION`은 첫 턴에만 사용하되 타입 판별은 시나리오의 `firstSpeaker`를 기준으로 한다.
- AI 메시지의 `translatedContent`는 기준 locale 번역문이며, 시작 안내는 자체가 기준 locale 문구이므로 번역을 `null`로 전달한다.
- 메시지별 피드백 요청이 정상 접수된 경우에만 `PREPARING`을 FE에 반환한다.
- AI와 FE의 피드백 상태 값은 `PREPARING`, `COMPLETED`, `FAILED`를 사용한다.
- LAN-93에서는 메시지별 피드백을 BE DB에 저장하지 않는다.
- AI 서버는 메시지별 피드백을 캐시에 저장하고, 최종 피드백 저장은 별도 이슈에서 처리한다.

## 변경 전 구현 결과

- `SessionMessageFeedbackRequester`가 직전 AI 메시지와 제출 사용자 메시지로 피드백 요청을 구성한다.
- 직전 메시지가 없는 USER first 첫 발화는 피드백 요청 없이 `null` 상태를 반환한다.
- `AiScenarioContextMapper`로 다음 메시지, 종료 메시지, 피드백 요청이 같은 시나리오 컨텍스트 조립 로직을 사용한다.
- 원격 AI 클라이언트는 `/api/v1/conversation/message-feedback`의 202 응답을 처리한다.
- 통합 테스트에서 요청 본문, USER first 생략, 실패 보상 삭제, 응답 상태, 메시지 ID, 세션 ID 검증을 확인했다.
- 종료 메시지 생성 경로도 메시지별 피드백 요청과 `PREPARING` 응답을 반환하는지 검증했다.

## USER First 계약 전환 결과

- 기존 `messageContext` 요청 DTO를 `evaluationContext`와 최상위 `userMessage` 구조로 교체했다.
- `AiMessageFeedbackEvaluationContextType`에 `AI_MESSAGE`, `SCENARIO_OPENING_INSTRUCTION`을 추가했다.
- 시나리오 메시지 컨텍스트 조회에 `firstSpeaker`와 `userOpeningInstruction`을 추가했다.
- USER First 첫 발화의 피드백 생략 분기를 제거하고 시작 안내 기반 피드백 요청으로 변경했다.
- AI First와 USER First의 요청 본문, 세션 전체 메시지 순번, FE 상태를 통합 테스트로 검증했다.

## 참고 구현

- `saynow-be`의 `origin/develop`은 다음 질문 또는 종료 메시지를 생성한 뒤 턴별 피드백을 요청한다.
- saynow-be는 AI 호출을 트랜잭션 밖에서 수행하고 모든 응답이 유효할 때 결과를 저장한다.
- Landit은 사용자 메시지가 별도 엔티티이므로 AI 피드백 요청 전에 사용자 메시지를 먼저 저장하고 실패 시 보상 삭제한다.
