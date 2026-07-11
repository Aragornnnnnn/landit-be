# LAN-93 Checklist

## AI 메시지별 피드백 연동

- [x] `message-feedback` 요청 DTO와 응답 모델을 추가한다.
- [x] `AiConversationClient`에 메시지별 피드백 요청 메서드를 추가한다.
- [x] 로컬·원격 AI 클라이언트 구현을 추가한다.
- [x] 사용자 메시지 직전 AI 메시지 컨텍스트를 구성한다.
- [x] 다음 메시지 생성 후 메시지별 피드백을 순차 요청한다.
- [x] 응답의 `sessionId`, `messageId`, `feedbackStatus`를 검증한다.
- [x] 정상 접수 시 FE 응답에 `PREPARING`을 반환한다.
- [x] USER first 첫 사용자 메시지는 피드백 요청을 생략하고 상태를 `null`로 반환한다.
- [x] AI 호출 실패 시 사용자 메시지를 제거하고 `503`을 반환한다.
- [x] AI 응답 검증 실패 시 사용자 메시지를 제거하고 `502`를 반환한다.
- [x] 다음 메시지 생성 실패와 피드백 요청 실패를 내부 로그에서 구분한다.

## 테스트 및 검증

- [x] AI first 메시지의 피드백 요청 DTO 구성을 검증한다.
- [x] USER first 첫 메시지에서 피드백 요청이 생략되는지 검증한다.
- [x] 피드백 요청 성공 시 `PREPARING` 응답을 검증한다.
- [x] 피드백 요청 실패 시 사용자 메시지가 제거되는지 검증한다.
- [x] 피드백 응답 식별자 또는 상태가 잘못된 경우 `502`를 검증한다.
- [x] `./gradlew test`를 실행한다.

## USER First 피드백 계약 확장

- [ ] 요청 DTO를 `evaluationContext`와 `userMessage` 구조로 변경한다.
- [ ] 평가 컨텍스트 타입에 `AI_MESSAGE`, `SCENARIO_OPENING_INSTRUCTION`을 추가한다.
- [ ] 시나리오 메시지 컨텍스트에서 `firstSpeaker`와 `userOpeningInstruction`을 조회한다.
- [ ] AI First는 직전 AI 메시지를 평가 컨텍스트로 구성한다.
- [ ] USER First 첫 발화는 시나리오 시작 안내를 평가 컨텍스트로 구성한다.
- [ ] USER First 첫 발화도 메시지별 피드백을 요청하고 `PREPARING`을 반환한다.
- [ ] `messageSequence`를 세션 전체 메시지 순번으로 전달한다.
- [ ] 원격 AI 요청 직렬화 테스트를 새 계약에 맞게 변경한다.
- [ ] AI First와 USER First 통합 테스트를 갱신한다.
- [ ] `./gradlew test`를 다시 실행한다.
