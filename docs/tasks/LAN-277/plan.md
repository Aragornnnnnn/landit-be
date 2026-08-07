# LAN-277: 표현 학습 오답 선택지 2차 교정 (누락분 추가 + 오답 재설계)

## 개요

LAN-276(V46) 1차 교정 이후 재검수에서 확정된 개정판 반영:

- **1차 미검출 불량 18건 추가** (expr 84, 89, 93, 95, 100, 102, 105, 110, 118, 120,
  121, 130, 139, 140, 146, 147, 150, 159) — 같은 유형의 비실존 단어
  (throwed/threws, exactlyed/exactlys, prefered/prefering, Sorrys/Sorrying,
  mistakens/mistakened, grewing/grews, Moneyed/Moneys, Swinged 등)
- **기존 교정분 48건 중 40건 재수정** — 대표 문제 오답 재설계(rep=redesigned 27건) 및
  선택지 재배치(spread)
- 배열 단위 실변경: 대표문장 40/80, 예문 160/320 → 선택지 배열 합계 200/400 (50%)

V46은 이미 머지·적용되어 수정 불가(Flyway 체크섬) → 개정판 66건 전체를 새 마이그레이션으로
재적용한다. 이 중 8건은 V46과 동일 값 재적용이라 무해.

## 반영 방식

`db/postgresql/V47__refine_writing_expression_word_choices.sql` — Flyway 마이그레이션.

- UPDATE 66건 (신규 표현 80건 중 82.5%)
- 오답 선택지만 교체 — V44 원본 대비 필드 단위 diff로 정답 문장·단어 배열·예문 텍스트·
  이미지 URL 불변 확인 완료
- 원본 SQL의 `representative_sentence_word_choices = '...'::jsonb`는 컬럼 타입(varchar array)과
  불일치라 40건을 `ARRAY[...]` 문법으로 교정해 변환 (1차와 동일한 이슈)
- 교정 후 전량 재스캔: 불량 패턴(폐쇄류+접미사 / e-탈락 위반 / 어간 조각) 잔여 0건
- 고정 매핑은 `Lan277ContentMigrationTests`가 검증

## 버전 순서

- 이 PR이 **V47** 사용. #85(LAN-265)는 **V48로 리네임** 필요 (안내 예정)
- V46(LAN-276)보다 뒤 순번이라 1차 교정 위에 2차가 덮어쓰는 순서가 보장됨

## 남은 작업 (재발 방지 — 별도 트래킹)

- [ ] 오답 생성 로직 수정: 열린 품사에서만 생성, 실제 굴절형만 사용, 사전 필터 적용
      — 콘텐츠 생성 파이프라인 측 작업
