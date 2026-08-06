# LAN-263: 시나리오 콘텐츠 전면 개편 + 신규 20개 추가 (20일 → 40일)

## 개요

- 기존 20개 시나리오 질문 60건 전면 재작성 (자연스러운 회화체, 질문 독립성, 화용 함정 유지)
- 신규 20개 시나리오(id 21~40) + 질문 60건(id 61~120) 추가, 신규 카테고리 '쇼핑'(id 4) 신설
- 캐릭터 통일: Marco(tts 2) / Chloe(tts 1) / 제3자 Teddy(tts 3, LAN-261의 V38로 등록)
- `scenario.display_order`를 **카테고리별 순번 → 전체(글로벌) 40일 Day 번호**로 전환
- 백엔드 코드: 일일 진행 순서를 시나리오 ID순 → display_order순으로 변경
  (`ScenarioSequenceQueryRepository.findScenarioIdsInDisplayOrder`)

Day ↔ 시나리오 ↔ 첫 질문 매핑은 [sql/scenario_q1_mapping.md](sql/scenario_q1_mapping.md) 참고.

## 반영 순서 (순서 위반 시 제약 충돌로 실패)

| 순서 | 작업 | 의존성 |
| --- | --- | --- |
| 1 | [sql/update_scenario_questions.sql](sql/update_scenario_questions.sql) — 기존 질문 60건 UPDATE | 없음 |
| 2 | [sql/update_scenarios.sql](sql/update_scenarios.sql) — ai_role + 글로벌 Day 번호 부여 | **3보다 먼저.** 기존 20개가 카테고리별 옛 번호(1~8)를 가진 상태에서 신규가 들어오면 `uk_scenario_category_order` (category_id, display_order) 충돌 (예: 신규 21번 Day 6 vs 기존 id 6의 do 6) |
| 3 | [sql/insert_new_scenarios.sql](sql/insert_new_scenarios.sql) — '쇼핑' 카테고리 + scenario 21~40 + 질문/해석 | **4(코드 배포) 없이도 가능하나 2 이후 필수.** 5의 D)섹션이 scenario 21~40을 FK 참조하므로 5보다 먼저 |
| 4 | **LAN-263 코드 배포** — V38 Flyway(Teddy 보이스, LAN-261)가 함께 자동 적용 | 구코드는 id순 진행이라 1~3 데이터 변경의 유저 영향 없음(무중단). 배포 후 Teddy id 확인: `SELECT id FROM tts_voice WHERE model = 'deepgram/aura-2'` → 3이 아니면 5번 파일의 `tts_voice_id = 3`을 실제 id로 치환 |
| 5 | [sql/update_scenario_language_variant.sql](sql/update_scenario_language_variant.sql) — 이름 치환(A·B) + tts 재배정(C) + 신규 variant 20건 INSERT(D) | **3·4 이후 필수.** D)가 scenario 21~40 참조, C)·D)가 Teddy tts_voice FK 참조. C)는 TTS 담당자 작업과 중복 여부 확인 후 실행 |
| 6 | 시퀀스 재조정 | id 명시 INSERT를 썼으므로 `scenario`, `scenario_question`, `scenario_question_language_variant`, `scenario_language_variant`, `category`, `category_language_variant` 시퀀스를 MAX(id)로 setval |

코드 배포를 맨 앞으로 당겨도 동작은 하지만, 배포~2번 완료 사이에 진행 순서가
카테고리 교차 순서(display_order, id)로 일시 왜곡되므로 위 순서를 권장한다.

## 남은 작업 (TODO)

- [ ] 신규 20개 썸네일 제작 (현재 `thumbnail_url` NULL) 후 UPDATE
- [ ] 기존 시나리오 1·6·8 썸네일에 구 캐릭터 외형이 박혀 있으면 교체
- [ ] 신규 20개 Q1 inner_thought 작성 (현재 NULL) 후 variant UPDATE
- [ ] 데이터 반영 후 `scenario.display_order` 전역 UNIQUE 제약 마이그레이션 (선택 —
      데이터가 카테고리별 중복 번호인 상태에서 먼저 나가면 마이그레이션 실패하므로 반드시 반영 후)
