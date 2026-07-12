# LAN-128 시나리오 고정 질문 저장 일원화 계획

## 목표

AI가 말하는 질문 본문과 속마음을 `scenario_question` 계열에서 함께 관리하고, 질문을 `display_order=1,2,3`으로 연속 조회한다.

## 범위

- AI 시작 시나리오는 `display_order=1` 질문을 세션 시작 메시지로 사용한다.
- 이후 질문 순서는 AI 시작이면 `submittedTurnNumber + 1`, 사용자 시작이면 `submittedTurnNumber`로 조회한다.
- 사용자 시작 안내는 기존 `user_opening_instruction`을 유지한다.
- `scenario_question_language_variant`에 nullable `inner_thought`, `inner_thought_type`을 추가하고 질문 1번에만 저장한다.
- 두 속마음 컬럼은 모두 값이 있거나 모두 `null`이도록 DB 체크 제약을 둔다.
- `scenario_language_variant`의 `ai_opening_message`, 번역, 속마음, 속마음 타입 컬럼을 V18에서 제거한다.
- 기존 20개 시나리오의 질문 데이터 삽입과 Flyway 데이터 마이그레이션은 제외한다.
- 기존 API DTO와 BE-AI 계약은 변경하지 않는다.

## 작업 순서

### 1. 테스트

- V18 적용 후 새 속마음 컬럼과 체크 제약이 존재하고 기존 네 컬럼이 제거되는지 검증한다.
- AI 시작 메시지와 목록 미리보기가 질문 1번의 본문·번역·속마음을 사용하는지 검증한다.
- AI 시작은 질문 2, 3번, 사용자 시작은 질문 1, 2, 3번을 연속 조회하는지 검증한다.

### 2. 스키마와 조회 변경

- `V18__move_scenario_opening_content_to_question.sql`에서 질문 Variant에 속마음 컬럼과 체크 제약을 추가하고 기존 네 컬럼을 제거한다.
- `ScenarioQuestionLanguageVariant`에 새 필드를 매핑하고 `ScenarioLanguageVariant`의 기존 속마음 필드를 제거한다.
- 세션 시작과 목록 조회가 질문 1번 Variant의 본문·번역·속마음을 함께 조회하도록 변경한다.
- 테스트 데이터 생성 코드를 새 스키마에 맞춘다.

### 3. 검증

- `./gradlew test --no-daemon`으로 H2 마이그레이션과 전체 테스트를 검증한다.
- PostgreSQL 테스트 DB에 `./gradlew flywayMigrate`를 실행해 V18 적용을 확인한다.
- `git diff --check`를 실행한다.
- ERD와 DBML은 수정하지 않고 변경할 컬럼 정의만 사용자에게 전달한다.

## 배포 전제

사용자는 기존 속마음 데이터를 별도로 보관한다. V18 적용 후 운영 DB에 질문 1번의 속마음을 포함한 60개 질문 SQL을 등록한 뒤 LAN-128 애플리케이션을 배포한다.
