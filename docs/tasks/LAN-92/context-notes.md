# LAN-92 사용자 발화 제출 컨텍스트 노트

## 2026-07-09

- 작업 브랜치는 `feat/LAN-92`이며, 기준은 LAN-91 세션 시작/중도 종료 구현이다.
- 이번 이슈는 사용자 발화 제출, 다음 AI 메시지 저장, 종료 메시지 저장, 세션 완료 처리를 다룬다.
- `ScenarioQuestion` 테이블은 선행 작업으로 추가되어 있으므로, LAN-92는 해당 테이블을 조회해 다음 고정 질문을 결정한다.
- 사용자 입력 타입은 현재 코드에 있는 `TEXT`, `VOICE`, `GENERATED`를 유지한다. 명세의 `MIXED`는 이번 구현에 반영하지 않는다.
- 최신 정책상 종료 메시지도 `nextMessage`에 담아 내려준다. 따라서 마지막 턴에서 `nextMessage = null`이라는 초기 명세 문구는 수정 대상이다.
- AI 호출은 외부 의존성이므로 테스트에서는 가짜 클라이언트를 주입하고, 실제 구현은 `AiConversationClient` 인터페이스 뒤에 둔다.
- 메시지별 피드백 생성 요청은 별도 이슈 범위이므로 LAN-92에서는 호출하지 않는다.
- SayNow BE 로컬 `develop`과 `origin/develop`은 같은 커밋을 가리키고 있었다. SayNow는 다음 질문이 있으면 next-question을 호출하고, 다음 질문이 없으면 closing-message를 저장하는 흐름이다.
- Landit은 고정 질문 체계이므로 AI next-message 응답이 `COMPLETED`를 반환해도 다음 질문이 남아 있으면 세션을 계속 진행한다.
- 첫 TDD 사이클은 사용자 발화 제출 성공 케이스로 진행했다. 구현 전 `com.landit.landitbe.session.infrastructure.ai` 패키지 부재로 컴파일 실패를 확인했고, 구현 후 해당 단일 테스트가 통과했다.
- USER first 세션은 시작 시 히스토리가 없으므로 첫 사용자 메시지 제출 시 `session_history`를 생성한다. 이때 다음 고정 질문은 displayOrder 1번을 조회한다.
- AI first 세션은 기존 시작 메시지를 히스토리에 갖고 있으므로 사용자 답변 뒤 displayOrder 2번 질문을 다음 질문으로 조회한다.
- 다음 질문이 없으면 `MAX_TURNS_REACHED`로 closing-message를 호출하고, 종료 메시지도 `nextMessage`로 저장해 반환한다.
- AI 호출 실패 시 사용자 메시지만 남지 않도록 저장은 짧은 트랜잭션에서 처리하되, AI 서버 호출은 DB 트랜잭션과 세션 row lock 밖에서 수행한다.
- `./gradlew test --tests 'com.landit.landitbe.session.ScenarioSessionApiIntegrationTests.submit*Message*'`와 `./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`가 통과했다.
- 전체 회귀 검증으로 `./gradlew test`를 실행했고 통과했다.
- 리뷰 점검에서 saynow-be처럼 외부 AI 호출이 트랜잭션 밖에서 일어나는지 테스트로 고정하기로 했다.
- 트랜잭션 경계 수정 후 `./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests --rerun-tasks`와 `./gradlew test --rerun-tasks`가 통과했다.
- 리뷰 코멘트 반영으로 AI 요청 조립과 응답 검증은 `SessionMessageAiGenerator`로 분리하고, `SessionMessageSubmitUseCase`는 세션 상태 검증과 히스토리 저장 흐름에 집중하도록 정리했다.
- AI 서버의 next-message, closing-message path는 현재 명세상 고정값이므로 환경 변수에서 제거하고 원격 클라이언트 내부 상수로 관리한다.
- 리뷰 코멘트 반영으로 `SessionMessageAiGenerator.generate()`는 next/closing 분기 흐름만 보이게 하고, next-message 요청과 응답 검증은 `generateNextMessage()`로 분리했다.
- 정책 확인 결과 세션 종료는 모든 고정 질문을 소진했을 때만 발생해야 하므로, `goalCompletionStatus=COMPLETED`만으로 closing-message를 호출하지 않도록 수정했다.
- 리뷰 코멘트 반영으로 `AiNextMessageRequest` 조립은 `toNextMessageRequest()`로 분리해 next-message 호출부의 역할을 명확히 했다.
- 리뷰 코멘트 반영으로 `SessionMessageSubmitUseCase`는 짧은 트랜잭션 두 개와 AI 호출 순서만 조율하고, 사용자 메시지 저장은 `SubmittedMessageRecorder`, AI 결과 저장은 `GeneratedMessageRecorder`로 분리했다.
- `SubmittedMessageRecorder`와 `GeneratedMessageRecorder`에 중복되던 소유 세션 잠금 조회, 진행 상태 검증, 에러 매핑은 `LearningSessionFinder`로 분리했다. 다만 세션 히스토리나 시나리오 세션 조회처럼 한 Recorder에서만 쓰이는 private 조회는 아직 공통 컴포넌트로 빼지 않는다.
- AI 서버 호출 계약은 FE-BE API DTO가 아니라 application이 외부 AI Provider에 요구하는 포트 계약이므로 `session.application.port`로 분리했다. `infrastructure.ai`에는 원격/로컬 구현체와 설정 바인딩만 남기고, `SessionMessageAiGenerator`가 infrastructure 패키지에 직접 의존하지 않도록 정리했다.
