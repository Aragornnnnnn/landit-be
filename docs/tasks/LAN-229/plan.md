# LAN-229 추가 예문 조회 API 단어 칩 배열 추가 계획

**Goal:** 추가 예문 작문이 타이핑에서 단어 칩 순서 맞추기로 바뀐 기획에 맞춰, 추가 예문 조회 API의 `writingSentence`에 `writingSentenceWords`(정답 순서 단어 배열)와 `writingSentenceWordChoices`(정답+오답 3개를 섞은 선택지 배열)를 추가한다.

**Architecture:** `practice_examples_payload`(JSONB) 예문 항목에 `sentenceWords`/`sentenceWordChoices` 키를 필수 계약으로 추가한다. 파싱 결과는 `ParsedPracticeSentence`(응답 항목 + 단어 배열 캐리어)로 나르고, 단어 배열은 랜덤으로 뽑힌 `writingSentence`에만 노출한다. `practiceSentence` 목록 계약은 변경하지 않는다.

---

### Task 1: 응답 DTO와 파싱 확장

- [x] `WritingSentenceResponse`에 `writingSentenceWords`/`writingSentenceWordChoices` 추가.
- [x] `ParsedPracticeSentence` 캐리어 record 추가 (payload 키: `sentenceWords`/`sentenceWordChoices`).
- [x] `ExpressionQueryService`에 단어 배열 필수 검증 추가 — 키 누락/비배열/빈 배열/blank 원소 예문은 기존 필수 키 규칙과 동일하게 제외 + warn 로그.

### Task 2: 테스트

- [x] 단위: 뽑힌 예문의 payload 배열이 순서 그대로 `writingSentence`에 매핑되는지, 불량 단어 배열 예문이 제외되는지 검증.
- [x] 통합: 시딩 payload에 배열 추가, 응답 `writingSentenceWords`/`Choices` 검증.

### Task 3: 데이터 백필 (보류 → 콘텐츠 시트 수령 후)

- [ ] `db/postgresql/V25__add_practice_sentence_word_arrays.sql` — 예문별 정답/오답 단어를 payload에 주입 (V19 스타일 손 작성, V22 스타일 jsonb 갱신).
- [ ] 오답 단어 3개는 콘텐츠 시트로 제공받아 작성한다. 서버 파생(문장 분해 + 대표 예문 오답 재사용) 대신 payload 확장안을 채택한 결정 기록.

## 배포 주의

단어 배열 키는 **필수**라, 백필 전에 배포되면 기존 예문이 전부 제외되어 추가 예문 API가 404를 반환한다. **백필 마이그레이션(또는 수동 데이터 교정)과 같은 릴리스로 배포해야 한다.**

## 검증 결과

- `ExpressionQueryServiceTest`, `ExpressionPracticeApiIntegrationTests` 통과.
- `./gradlew check` 통과.
