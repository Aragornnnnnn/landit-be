# LAN-568 프리톡 표현 3001~4000 발음 평가 자산 적재 계획

## 목표

- 새 프리톡 표현 1,000개(id 3001~4000)의 발음 평가 자산을 만든다
- 자산은 `expression_pronunciation_asset` 테이블에 Flyway 마이그레이션으로 넣는다
- 규모는 1,000 × 3억양 = 3,000행이다
- 같은 PR에서 발음 자산 runbook을 Flyway 적재 방식에 맞게 갱신한다

## 범위 밖

- 표현 INSERT(`writing_expression` 1,000행)는 다른 개발자가 한다
  - 임베딩·이미지 URL도 그 개발자가 채운다
- 이 작업은 그 마이그레이션이 id 3001~4000을 먼저 넣는다는 전제로 FK를 건다

## 입력

- 표현 원본은 `insert_writing_expression_1000.sql`(2026-09-24 생성, 11개 카테고리)이다
  - 파일에는 id 컬럼이 없다
  - 파일 순서대로 3001부터 매긴다고 가정한다
  - 대응표는 작업 폴더의 `id_mapping.tsv`에 있다
  - **이 대응이 표현 INSERT 쪽 id와 다르면 문장·표현 음성 키가 틀린다**
- 발음 대상 문장은 `representative_sentence_text`이고, 표현은 `target_expression_text`이다
- 작업 폴더는 `eng_expressions2/발음자산_3001_4000/`이다

## 확인한 사실 (2026-09-25)

- dev의 `writing_expression`은 max(id) 3000이다
  - 3001 이상인 행은 0개다
  - 시퀀스 last_value는 3000이다
  - 자산의 최대 표현 id도 3000이다
  - Flyway 버전은 125다
- prod는 조회하지 못했다(권한 분류기 차단). 선녀가 직접 확인해야 한다
- S3 `content/expression-pronunciation-audio/`에는 숫자 폴더가 3000까지만 있다
- 원본 SQL에서 표현 텍스트 중복은 0개다. 패턴형(`~`·괄호·한글·`+`) 표현은 25개다
  - 표현 음성 NULL이 75행 나온다
- 기준 데이터와 TTS 소스는 생성을 마쳤다
  - 자산 25,461개 = 문장 3,000 + 표현 2,925 + 단어 19,536
  - 억양 대조는 346개다
  - `source_sha256`은 `48d1789ee1fc6067015e746f95029151e4d6b524502789720d8b8ad65128d150`이다
- 단어 합성 단위는 (억양, 해시) 기준 4,989개다
  - 공용 풀에 이미 3,114개가 있다(억양마다 1,038개)
  - 새로 만들 것은 1,875개다(억양마다 625개)
- 새로 합성할 음성은 총 7,800개다(문장 3,000 + 표현 2,925 + 단어 1,875)

## 구현 순서

1. [x] 기준 데이터 3억양 생성, `build_tts_source`, `validate-source`
2. [x] id는 3001부터(선녀 확인). dev max(id) 3000 확인. **prod max(id)는 미확인**(권한 분류기 차단)
3. [x] TTS 생성
   - landit-iac `feat/LAN-561` 코드를 쓴다
   - `--reuse-s3-bucket --boto3 --workers 4`로 돌린다
   - `verify`, `verify-accent`까지 진행한다
4. [x] QA
   - `expression_pronunciation_qa.py check --resynth`
   - 남은 불합격은 선녀가 청취한다
5. [x] S3 게시
   - build-manifest → verify → upload → build-be-manifest → upload-reference
   - 키 4개를 이 문서에 기록한다
6. [x] `build_pronunciation_asset_sql.py`로 UPSERT SQL을 생성한다
7. [x] 마이그레이션 `db/postgresql/V128__insert_free_talk_expression_pronunciation_assets_3001_4000.sql`을 작성한다
   - 번호는 표현 INSERT 마이그레이션과 V126(LAN-560)보다 크게 정한다
   - 적재 전 검사: 1,000개 표현·예문 글자 대조
   - 적재 후 검사:
     - 3,000행, 억양마다 1,000행
     - 문장 URL NULL 0
     - 표현 URL NULL 75
   - 단어 URL 검사: 전부 `word/{억양}/{해시}.mp3`이고 억양이 행과 같아야 한다
8. [x] 계약 테스트를 추가한다(`Lan471PronunciationAssetMigrationTests`와 같은 방식)
9. [x] 검증
   - dev 롤백 드라이런: 표현 마이그레이션과 자산 마이그레이션을 한 트랜잭션에서 실행한다
   - `./gradlew check`
10. [ ] PR 배포 순서: #224(V126) → 표현 INSERT PR → 이 PR

## 검증 결과

- 2026-09-25 generate 결과:
  - 단위 10,914개 = 합성 7,800 + 공용 풀 재사용 3,114
  - 실패 0
  - `verify` 10,914개 통과
- verify-accent는 5회 돌리고 prune했다:
  - 실패 수: 151 → 13 → 5 → 6 → 2
  - 억양 대조: 346개 → 194개(EN_US 147 · EN_GB 47 · EN_AU 0)
  - 대부분 GB 음성이 t를 미국식 flap(d)으로 읽은 경우다
  - 뒤 차수 실패에는 이전에 통과한 항목이 섞여 있다. LLM 판정의 흔들림이다
  - prune 전 원본은 `reference_before_prune1/`에 있고, 문제 목록은 `logs/accent_problems_{1..5}.txt`에 있다
- Whisper QA 대상은 새로 합성한 7,800개(`new_unit_ids.txt`)뿐이다
  - 공용 풀에서 가져온 단어는 제외했다
  - 여기서 재합성하면 그 단어를 쓰는 모든 표현의 소리가 바뀌기 때문이다
- 2026-09-25 23:5x에 QA를 선녀 요청으로 중단했다
  - 보고서 기준 3,740/7,800 검사: 합격 3,619, 불합격 121(재합성 후)
  - 중단 후 state와 디스크 파일의 sha 불일치는 0이다
  - **재개**: 같은 check 명령을 다시 실행한다. 합격이고 sha가 같은 단위는 건너뛴다
- `build_pronunciation_asset_sql.py`는 id가 있는 단일 INSERT만 읽는다
  - 그래서 `expressions_3001_4000_for_asset_sql.sql`(id·표현·문장 대조표)을 따로 만들었다
- 2026-09-27 QA 재개·완료:
  - 7,800개 = 첫 시도 합격 5,872 + 재합성 후 합격 1,744 + 불합격 184(AU 154 · GB 21 · US 9)
- Gemini 2차 판정(adjudicate):
  - 정상 43 · 불량 140 · 미판정 1
  - 처음에는 단어 74개가 미판정으로 나왔다. adjudicate가 단위 id(`word/{억양}/{해시}`)를 찾지 못하던 버그 때문이다
  - 이 버그는 landit-iac `feat/LAN-561`에서 고쳤다(커밋 안 함)
- 선녀 청취:
  - AU 표현 50개 중 불량은 3036 "tone up" 1개뿐이었다
  - 3036은 후보 4개도 전부 "turn up"으로 들려 현행 유지로 결정했다
  - 미국·영국·잘림 33개는 전부 정상이었다
  - → 184개 전부 합격 처리했다(보고서 `lastReason`에 `listened-ok` 기록)
  - 결론: Whisper small.en은 AU를 크게 오청한다
- 마지막 verify-accent 1회: 6개 실패, prune 후 종료했다. 최종 억양 대조는 188개다
- S3 게시(2026-09-27):
  - 새 키 7,801개 업로드(문장 3,000 · 표현 2,925 · 단어 1,875 · 작업 매니페스트 1)
  - 덮어쓰기 0 · 충돌 0 · 검증 10,915
  - be manifest: `content/expression-pronunciation-audio/manifests/be-f5985101c6e5b28655a64edf75a55f1ded6b7fba4c51f3466f7c4ff45d0d57c6.json` (3,000행)
  - reference EN_US: `content/expression-pronunciation-audio/reference/EN_US-92f2854441fef0262dd770a6301027d7ba14ca78f89bb461309a277922294bad.json`
  - reference EN_GB: `content/expression-pronunciation-audio/reference/EN_GB-3a4d1e3fbb90311016d814545b5316f899c7989a0955c9b600c3c570e3221261.json`
  - reference EN_AU: `content/expression-pronunciation-audio/reference/EN_AU-82d832431a0e1144f2cea744c5891fed52f0703a3cc7de5b5143b68e5789e81f.json`
- V128 dev 롤백 드라이런(표현 픽스처와 V128을 한 트랜잭션에서 실행):
  - 통과, 220초
  - 억양당 1,000행, 표현 음성 NULL 25씩, 단어 19,536개
  - 롤백 후 dev의 3001 이상 행은 0개다
- 음 버전 검사:
  - 문장 한 글자 변경 → 적재 전 대조가 id 3001을 지목하며 중단
  - 단어 URL 하나를 옛 형식으로 변경 → 단어 URL 검사가 중단
- 계약 테스트 `Lan568PronunciationAssetMigrationTests` 5개 통과
- `./gradlew check`(JDK 21) 통과: 테스트 1,794개(건너뜀 12, 실패 0), Spotless·Checkstyle 통과

## 표현 INSERT 담당자에게 전달할 것

원본 `insert_writing_expression_1000.sql`은 그대로는 dev에서 실패한다.

- `owner_user_profile_id` 컬럼은 V51에서 삭제됐다. 컬럼과 값(NULL)을 빼야 한다
- `representative_sentence_words`·`representative_sentence_word_choices`는 `varchar[]`다. `'[...]'::jsonb`로는 들어가지 않는다
- `id`를 3001부터 **파일 순서대로** 명시해야 한다. 음성 S3 키와 V128 적재 전 대조가 이 대응을 전제한다
- 문장·표현 글자를 바꾸면 V128이 배포 때 중단된다. 바꿔야 하면 알려 달라(해당 표현 음성 재생성)
- BEGIN/COMMIT은 빼야 한다(Flyway가 트랜잭션을 관리한다)
- 마이그레이션 번호는 V127로 가정했다. 다르면 V128 파일 이름과 계약 테스트 경로를 바꾼다

