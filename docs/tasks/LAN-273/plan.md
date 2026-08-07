# LAN-273: 신규 시나리오 inner_thought(속마음) 추가

## 개요

- `scenario_question_language_variant.inner_thought`는 AI 캐릭터가 첫 질문을 던지기 직전의
  속마음 텍스트 (짝 컬럼 `inner_thought_type` = GOOD/NORMAL/BAD)
- 기존 20개는 채워져 있으나 신규 20개(scenario 21~40)는 전부 NULL이라 경험 통일 필요
- 기존 데이터 규칙 준수: **AI가 먼저 말하는 시나리오의 1번 질문에만** 존재
  (유저가 먼저 말하는 시나리오는 속마음 노출 타이밍이 없음)

## 반영 방식

`db/postgresql/V45__add_new_scenario_inner_thoughts.sql` — Flyway 마이그레이션으로
develop/prod 배포 파이프라인에서 자동 적용 (운영 전용 위치라 H2 테스트 DB 무관).

- UPDATE 14건: 신규 AI 먼저 시나리오 14개의 Q1
  (q_id 64, 67, 73, 79, 82, 88, 91, 94, 97, 100, 103, 106, 109, 112)
- 신규 USER 먼저 6개(scenario 21, 24, 26, 29, 39, 40)는 제외 — NULL 유지
- 수정 컬럼은 `inner_thought`, `inner_thought_type`, `updated_at` 3개뿐 (질문 텍스트·해석 미변경)
- type 분포: GOOD 3(q79·94·106) / NORMAL 11
- 고정 매핑은 `Lan273ContentMigrationTests`가 검증 (대상 q_id 집합, type 값, 질문 텍스트 미변경)

V41(신규 질문 variant INSERT)보다 뒤 순번이므로 대상 행 존재가 버전 순서로 보장된다.
