# LAN-276: 표현 학습 오답 선택지 비실존 단어 교정

## 개요

- 신규 시나리오(21~40)용 `writing_expression` 표현의 오답 선택지
  (`representative_sentence_word_choices`, `practice_examples_payload` 내 `sentenceWordChoices`)에
  영어에 존재하지 않는 단어가 포함됨 (예: nevers, mights, giveing, justs, Anyth)
- 문장 조립 퍼즐의 오답 타일로 유저에게 그대로 노출되는 데이터라 배포 전 교정 필요
- 불량 유형: ① 폐쇄류 품사(-s/-ing/-ed 부착 불가)에 접미사 부착 ② e-탈락 규칙 위반 철자
  ③ 어미를 잘라낸 어간 조각
- 원인: 오답 생성이 품사 확인·사전 검증 없이 문자열 수준 접미사 조작으로 동작

## 반영 방식

`db/postgresql/V46__fix_writing_expression_word_choices.sql` — Flyway 마이그레이션으로
배포 파이프라인에서 자동 적용 (운영 전용 위치라 H2 테스트 DB 무관).

- UPDATE 48건: 이슈 검출 46건 + 재스캔 추가분 2건(expr 127, 129)
- 불량 단어만 실존 혼동 단어로 1:1 교체 — 정답 문장·단어 배열·예문 텍스트·이미지 URL 미변경
  (V44 원본과 필드 단위 diff로 확인)
- 원본 SQL의 `representative_sentence_word_choices = '...'::jsonb`는 컬럼 타입(varchar array)과
  달라 실행이 실패하므로 `ARRAY[...]` 문법으로 교정해서 변환 (payload는 jsonb라 캐스트 유지)
- 고정 매핑은 `Lan276ContentMigrationTests`가 검증 (대상 id 48건, 비실존 단어 잔존 0,
  array 문법 회귀 방지)

## 버전 순서

이 PR이 #85(LAN-265, draft)보다 먼저 머지·배포되므로 V46을 사용한다.
**나중에 머지되는 #85는 V47로 리네임 필요** (PR 코멘트로 안내됨). Flyway 기본
설정(outOfOrder=false)에서는 낮은 번호가 늦게 들어오면 migrate가 실패하므로,
먼저 반영되는 쪽이 낮은 번호를 갖는 것이 원칙.

## 남은 작업 (재발 방지 — 별도 트래킹)

- [ ] 오답 생성 로직 수정: 열린 품사(동사·명사·형용사)에서만 생성, 실제 굴절형만 사용,
      생성 후 사전(워드리스트) 필터 적용 — 콘텐츠 생성 파이프라인 측 작업
- [ ] 교정/재생성 후 검증 스캔 재실행 (잔여 불량 0건 확인) — 이번 교정분은 스캔 완료
