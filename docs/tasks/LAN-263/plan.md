# LAN-263: 시나리오 콘텐츠 전면 개편 + 신규 20개 추가 (20일 → 40일)

## 개요

- 기존 20개 시나리오 질문 60건 전면 재작성 (자연스러운 회화체, 질문 독립성, 화용 함정 유지)
- 신규 20개 시나리오(id 21~40) + 질문 60건(id 61~120) 추가, 신규 카테고리 '쇼핑'(id 4) 신설
- 캐릭터 통일: Marco(tts 2) / Chloe(tts 1) / 제3자 Teddy(tts 3, LAN-261의 V38로 등록)
- `scenario.display_order`를 **카테고리별 순번 → 전체(글로벌) 40일 Day 번호**로 전환
- 백엔드 코드: 일일 진행 순서를 시나리오 ID순 → display_order순으로 변경
  (`ScenarioSequenceQueryRepository.findScenarioIdsInDisplayOrder`)

Day ↔ 시나리오 ↔ 첫 질문 ↔ tts 매핑은 [sql/scenario_q1_mapping.md](sql/scenario_q1_mapping.md) 참고.

## 반영 방식 (LAN-271에서 Flyway 마이그레이션으로 전환)

콘텐츠 반영은 수동 SQL이 아니라 **Flyway 마이그레이션**으로 적용한다 (LAN-271).
CI의 flyway-migration workflow가 `db/migration` + `db/postgresql`을 버전 순서대로 실행하므로
별도의 수동 실행·순서 관리가 필요 없다. `db/postgresql`은 운영 전용이라 H2 테스트 DB에는
콘텐츠가 들어가지 않는다.

| 버전 | 파일 | 내용 |
| --- | --- | --- |
| V39 | `db/postgresql/V39__rewrite_scenario_questions.sql` | 기존 질문 60건 재작성 |
| V40 | `db/postgresql/V40__apply_scenario_global_display_order.sql` | ai_role 정리 + 글로벌 Day 번호 부여 |
| V41 | `db/postgresql/V41__insert_new_scenarios.sql` | '쇼핑' 카테고리 + 신규 20개 + 질문/해석 + setval |
| V42 | `db/postgresql/V42__update_scenario_language_variants.sql` | 캐릭터명 치환 + tts 재배정 + 신규 variant 20건 + setval (Teddy id≠3이면 즉시 실패하는 가드 포함) |
| V43 | `db/migration/V43__enforce_global_scenario_display_order.sql` | `uk_scenario_category_order` 삭제 + `UNIQUE(display_order)` 전역 강제 |

- 버전 순서가 의존성을 강제한다: V40(빈자리 생성) → V41(신규 INSERT, 유니크 충돌 방지),
  V38(Teddy) → V42(tts FK), V39~V42(데이터 완성) → V43(전역 unique)
- 머지 순서: **LAN-263(코드) → LAN-271(마이그레이션)**. 구코드는 id순 진행이라
  데이터가 먼저 반영돼도 유저 영향이 없고, 새 코드는 데이터 반영 전에도 (do, id)
  tie-breaker로 결정적으로 동작한다.

## 남은 작업 (TODO)

- [ ] 신규 20개 썸네일 제작 (현재 `thumbnail_url` NULL) 후 UPDATE
- [ ] 기존 시나리오 1·6·8 썸네일에 구 캐릭터 외형이 박혀 있으면 교체
- [ ] 신규 20개 Q1 inner_thought 작성 (현재 NULL) 후 variant UPDATE
