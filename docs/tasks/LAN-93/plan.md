# LAN-93 Plan

## 목표

AI First와 USER First 시나리오의 사용자 메시지를 동일한 메시지별 피드백 API로 요청하고, 정상 접수된 경우 FE에 `PREPARING` 상태를 반환한다.

## 구현 계획

1. AI 피드백 요청 계약을 `evaluationContext`와 `userMessage` 구조로 변경한다.
   - 검증 기준은 원격 클라이언트 직렬화 테스트로 확인한다.
2. 시나리오 메시지 컨텍스트에 `firstSpeaker`와 `userOpeningInstruction`을 포함한다.
   - 검증 기준은 JPA 컨텍스트 조회와 통합 테스트로 확인한다.
3. AI First와 USER First에 맞는 평가 컨텍스트를 구성한다.
   - AI First는 직전 AI 메시지를 `AI_MESSAGE`로 전달한다.
   - USER First 첫 발화는 시작 안내를 `SCENARIO_OPENING_INSTRUCTION`으로 전달한다.
4. 두 시나리오 모두 피드백 접수 성공 시 `PREPARING`을 반환한다.
   - 검증 기준은 메시지 제출 API 통합 테스트로 확인한다.
5. 관련 문서와 테스트를 갱신하고 전체 테스트를 실행한다.
   - 최종 검증 명령은 `./gradlew test`다.

## 범위 제외

- 메시지별 피드백의 BE DB 저장.
- AI 요청 자동 재시도.
- 최종 세션 피드백 생성 및 저장.
- AI 캐시의 다중 인스턴스 공유와 TTL 정책 변경.
- 평가 유형별 점수 가중치 도입.
