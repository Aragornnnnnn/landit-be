# LAN-555 시나리오 히스토리

## 확정 범위

- `GET /api/v1/scenarios/{scenarioId}/history`에서 로그인 사용자의 완료한 모든 회차를 조회한다.
- 완료 시각 내림차순, 동률이면 세션 ID 내림차순으로 반환한다. 진행 중·중도 종료 회차는 제외한다.
- 회차의 세션 ID, 시작·완료 시각, 대화 순서대로 정렬된 AI/사용자 메시지, 번역, 속마음과 처리 상태를 제공한다.
- 저장된 요약·상세 피드백을 반환하며 기존 상세 피드백 잠금 정책을 적용한다. AI 생성·재시도는 하지 않는다.
- 기록이 없거나 존재하지 않는 시나리오 ID이면 `sessions: []`, 저장된 완료 피드백이 없으면 `feedback: null`이다.
- 사용자 ID는 인증 토큰에서 가져오고 별도 사용자 ID 입력은 받지 않는다.

## 구현과 검증

- 완료 회차·메시지·피드백을 일괄 조회하고 저장 당시 메시지와 시작 안내로 평가 문맥을 복원한다.
- OpenAPI에 정렬·빈 결과·피드백 부재·인증 계약을 기록한다.
- 통합 테스트로 소유권, 시나리오·완료 상태 필터, 회차·메시지 정렬, 속마음, 피드백 연결, 무료 잠금, AI 미호출을 검증한다.
- `./gradlew check` 완료 후 관련 변경만 커밋한다.

## FE 응답 사용

- `data.scenarioId`: 요청한 시나리오 ID.
- `data.sessions[]`: 전체 완료 회차. `sessionId`, `startedAt`, `endedAt`, `messages`, `feedback`을 포함한다.
- `messages[]`: `messageId`, `messageSequence`, `turnNumber`, `role`, `content`, `translatedContent`, `innerThought`, `innerThoughtType`, `innerThoughtProcessingStatus`.
- `role`은 `AI` 또는 `USER`이며 속마음은 저장된 사용자 메시지에 연결된다. 대상이 아닌 메시지의 속마음 상태는 null이다.
- `feedback`은 기존 최종 피드백 응답과 같은 구조다. `nativeScore`, `starRating`, `highlightMessage`, `summaryMessage`, `messageFeedbacks`, `detailFeedbackLocked`를 제공한다.
- 상세 피드백의 `messageId`로 대화 메시지와 연결할 수 있다. 평가 문맥은 당시 AI 메시지 또는 사용자 선톡 시작 안내에서 복원하며, 과거 원본이 없으면 `evaluationContext`는 null이다.
- 회차·메시지·요약·상세 피드백은 일괄 조회하며, 상세 공개 여부는 회차마다 기존 정책으로 판단한다. 스키마 변경은 없다.

## 검증 결과

- `./gradlew spotlessApply check` 통과. Spotless·Checkstyle·업무 경계 검사를 포함한다.
- 전체 테스트 1,773건 중 1,761건 통과, 12건 건너뜀, 실패·오류 0건.
- 새 히스토리 API 통합 테스트 7건 모두 통과. 인증, 빈 기록, 회차·대화 정렬, 피드백 복원·잠금, AI 미호출, OpenAPI 스키마를 확인했다.
- `git diff --check` 통과.
- 로컬 H2 기반 검증이며, 운영 PostgreSQL·배포·FE 화면 연동은 확인하지 않았다.
