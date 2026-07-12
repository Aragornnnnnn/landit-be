# LAN-128 시나리오 고정 질문 저장 일원화 계획

## 목표

AI가 말하는 모든 고정 질문을 `scenario_question`에서 순서대로 조회한다. 기존 세션 API와 BE-AI 계약은 유지한다.

## 범위

- AI 시작 시나리오는 `display_order=1` 질문을 세션 시작 메시지로 사용한다.
- 이후 질문 순서는 AI 시작이면 `submittedTurnNumber + 1`, 사용자 시작이면 `submittedTurnNumber`로 조회한다.
- 사용자 시작 안내는 기존 `user_opening_instruction`을 유지한다.
- `ai_opening_message`, `ai_opening_message_translation`은 애플리케이션의 질문 원본으로 사용하지 않는다.
- 기존 20개 시나리오의 질문 데이터 삽입과 Flyway 데이터 마이그레이션은 제외한다.
- 기존 컬럼 제거도 이번 작업에서 하지 않는다. 질문 구조가 안정화된 뒤 별도 마이그레이션으로 처리한다.

## 작업 순서

### 1. 진행 순서 테스트

- AI 시작 시 질문 1번을 시작 메시지로 반환하고 2번, 3번을 이어서 조회하는 통합 테스트를 작성한다.
- 사용자 시작 시 행동 지시 후 질문 1번, 2번, 3번을 조회하는 통합 테스트를 작성한다.
- 마지막 질문 응답 후 종료 메시지가 생성되는지 검증한다.

### 2. 질문 조회 일원화

- `ScenarioSessionStartQueryRepository`가 활성 질문 1번의 언어별 문구를 조회하도록 변경한다.
- `ScenarioListQueryRepository`도 질문 1번을 기존 `aiOpeningMessage` 응답 필드에 매핑한다.
- `SubmittedMessageRecorder`에서 시작 화자에 따라 다음 질문 순서를 계산한다.
- 기존 API DTO와 OpenAPI 명세는 변경하지 않는다.

### 3. 검증과 문서

- 질문 데이터가 없는 AI 시작 시나리오는 설정 누락으로 실패하는지 확인한다.
- 데이터 규칙을 문서에 반영하고 스키마가 바뀌지 않는 ERD·DBML은 수정하지 않는다.
- 아래 명령을 실행한다.

```bash
./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests
./gradlew test --tests com.landit.landitbe.content.ScenarioListApiIntegrationTests
./gradlew test
git diff --check feat/LAN-94...HEAD
```

## 배포 전제

LAN-128 배포 전에 운영 DB의 활성 시나리오에 질문 데이터가 등록되어 있어야 한다. 질문 데이터는 별도 SQL로 삽입한다.
