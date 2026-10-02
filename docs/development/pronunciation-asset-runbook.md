# 발음 평가 자산 운영 절차 (Runbook)

발음 평가 기능(LAN-342)의 자산인 원어민 TTS 음성과 발음 기준 데이터를 만들고 DB에 넣는 절차다.
writing_expression 표현을 새로 추가했거나, 문장을 수정했거나, 음성을 교체할 때 이 문서를 따라 하면 된다.

LAN-471(V95)부터 DB 적재는 **Flyway 마이그레이션**으로 한다. 어드민 임포트 API는 예비 수단으로만
남아 있다(맨 아래 참고). API로 넣던 시절에는 환경마다 따로 호출해야 해서 prod가 한동안 비어 있었고,
prod 호출은 nginx 60초 타임아웃(504)에 걸렸다. Flyway로 넣으면 배포할 때 dev·prod가 똑같이 채워진다.

## 전체 구조 한 장 요약

```
[landit-ai]   기준 데이터 JSON 생성 (발음 표기·음절·강세·억양 대조) + TTS 주문서(tts_source.json) 변환
[landit-iac]  mp3 생성 → 파일 검증·억양 검증 → Whisper QA·재합성 → S3 업로드
              → BE 매니페스트 1개·기준 데이터 3개 게시 (S3 키 4개 출력)
[SQL 생성기]   게시 파일 4개 + 표현 INSERT SQL → expression_pronunciation_asset UPSERT SQL
[landit-be]   db/postgresql/V{N}__...sql 마이그레이션 → dev 롤백 드라이런 → PR → 배포 때 적재
[커버리지 API] 전수 대조로 빠진 표현이 없는지 확인
```

생성·게시는 사람이 노트북 터미널에서 실행한다(자동 실행 없음).

- 자산 테이블: `expression_pronunciation_asset`, (표현, 억양)당 1행. 표현 1~3000 × 3억양
- 판정 기준 음성과 앱 "원어민 발음 듣기" 음성은 같은 파일이다. 유저가 고른 AI 튜터의 억양이 기준이다
- TTS 파일 이름은 (모델·보이스·텍스트·포맷) 해시라서 **문장이 바뀌면 그 표현의 음성은 다시 만들어야** 한다

## 확정 설정 (바꾸면 전량 재생성이므로 주의)

| locale | 모델 | 보이스 |
| --- | --- | --- |
| EN_US | deepgram/aura-2 | aura-2-thalia-en (미국 여성) |
| EN_GB | deepgram/aura-2 | aura-2-pandora-en (영국 여성) |
| EN_AU | deepgram/aura-2 | aura-2-theia-en (호주 여성) |

- TTS는 OpenRouter를 거친다
- 버킷은 `landit-content-982529430654`, CDN은 `https://d19azau1un4t7r.cloudfront.net`이다
- S3 키 규칙:
  - 문장·표현: `content/expression-pronunciation-audio/{표현id}/{억양}/{sentence|expression}/{해시}.mp3`
  - 단어: `content/expression-pronunciation-audio/word/{억양}/{해시}.mp3` (LAN-475/560/561 공용 풀)
    - 같은 억양의 같은 단어는 표현이 달라도 파일 하나를 함께 쓴다
    - V126이 기존 행의 단어 URL을 이 형식으로 옮겼다
    - 새 배치도 반드시 이 형식이어야 한다
- 표현 음성은 입력 끝에 마침표를 붙여 합성한다(잡음 꼬리 방지)
  - 해시는 원문으로 계산하므로 키는 바뀌지 않는다
  - 물음표·느낌표 등 이미 부호로 끝나는 표현은 건드리지 않는다
- 버킷에 버저닝이 없어 **덮어쓰기와 삭제는 되돌릴 수 없다**
- BE는 이 경로의 읽기 권한만 가진다 (Task Role, 테라폼 관리)

## 사전 준비

- landit-iac `main`에 LAN-561(공용 단어 풀 파이프라인)이 들어가 있어야 한다
  - 그 전 파이프라인은 단어를 옛 형식 `{표현id}/{억양}/word/{순서}/{해시}.mp3`로 만든다
  - V126 이후 DB와 형식이 어긋나고, 이미 있는 단어도 전부 다시 합성한다
- `export SSL_CERT_FILE="$(python3 -m certifi)"`를 설정한다
  - python.org 파이썬 3.12는 루트 인증서가 없다
  - 이대로 두면 OpenRouter 호출이 로그 없이 멈춘 것처럼 보인다
- `OPENROUTER_API_KEY`는 `landit-ai/.env`에 있다. AWS는 기본 프로필(계정 982529430654)을 쓴다
- 작업 폴더는 **영구 경로**에 새로 만든다(예: `eng_expressions2/발음자산_{시작}_{끝}/`)
  - 세션 스크래치 폴더는 하루 지나면 비워진다
  - 옛 작업 폴더의 `state.json`(스키마 1)은 거부되므로 재사용하지 않는다

## 절차 A — 새 표현 배치 (Flyway 적재)

**순서: 대상 확정 → 기준 데이터 → TTS → QA → 게시 → SQL 생성 → 마이그레이션 → 드라이런 → 배포 → 확인.**

1. **대상 확정**
   - 표현 INSERT 마이그레이션(또는 그 원본 SQL)의 문장이 최종본인지 확인한다
     - 음성을 만든 뒤 문장이 바뀌면 음성과 기준 데이터가 모두 틀린 채로 들어간다
   - 기존 표현이 빠진 경우에는 `coverage`의 `referenceMissing`·`audioMissing`이 대상 목록이다
2. **재료 준비** — 대상 표현을 JSON으로 만든다(`expressions.json`)
   ```json
   [{"expressionId": 1, "expressionText": "...", "sentenceText": "..."}]
   ```
3. **기준 데이터 생성** — landit-ai 레포에서 실행한다
   - 발음 사전 기반이라 AI를 호출하지 않는다
   - 두 번째 명령의 `--ids 164,177`로 일부 표현만 고를 수 있다
   ```bash
   .venv/bin/python scripts/generate_pronunciation_reference.py \
       --input expressions.json --locale EN_US --out-dir out/final   # EN_GB, EN_AU도 같은 방식
   .venv/bin/python scripts/build_tts_source.py \
       --expressions expressions.json --reference-dir out/final --out tts_source.json
   ```
4. **TTS 생성** — landit-iac 레포에서 실행한다
   - **`--reuse-s3-bucket`은 항상 준다**
     - 공용 자리에 이미 있는 단어는 합성하지 않고 내려받는다
     - 예: LAN-471 배치 기준으로 단어 16,167개 중 새로 만들 것은 984개다
     - 옛 지침 "신규 id면 빼라"는 표현별 경로 시절 이야기라 지금은 틀리다
   - 출력의 `synthesized=`·`reused_from_s3=`로 재사용이 됐는지 확인한다
   - 진행 상황은 `work/mp3/*.mp3` 개수로 본다
   ```bash
   python3 -m scripts.expression_pronunciation_audio validate-source --source tts_source.json
   python3 -m scripts.expression_pronunciation_audio generate --source tts_source.json --work-dir work \
       --reuse-s3-bucket landit-content-982529430654 --boto3 --workers 4
   python3 -m scripts.expression_pronunciation_audio verify        --source tts_source.json --work-dir work
   python3 -m scripts.expression_pronunciation_audio verify-accent --source tts_source.json --work-dir work
   ```
   - `verify-accent`가 실패하면:
     - 일시적 실패: 해당 mp3를 지우고 `generate`를 다시 돌린다
     - 계통적 실패: landit-ai `prune_accent_contrasts.py`로 대조를 제거하고 3번의 `build_tts_source`부터 다시 한다
       (보통 2~3회에 수렴한다)
   - **프루닝을 했으면 이후 단계는 반드시 최종 reference·tts_source 기준으로 진행한다**
5. **QA** — Whisper 전사 대조와 무음 검사를 하고, 불합격은 재합성한다
   - 같은 명령을 다시 돌리면 이어서 한다(합격이고 sha가 같은 자산은 건너뛴다)
   ```bash
   caffeinate -i .venv-qa/bin/python -u scripts/expression_pronunciation_qa.py check \
       --source tts_source.json --work-dir work --report work/qa_report.json \
       --resynth --max-resynth 6 --workers 4 --backend mlx --transcribe-timeout 90
   ```
   - 재합성 뒤에도 남은 불합격은 사람이 듣고 판정한다(`sample`로 청취 페이지를 만든다)
   - 단독 단어·AU 억양은 Whisper 오청이 많다. 불합격이라고 곧바로 불량으로 보지 않는다
   - `adjudicate`(Gemini 2차 전사)는 Whisper 불합격을 구제할 때만 쓴다
     - **기대 텍스트를 프롬프트에 넣으면 그대로 따라 적는 환각이 나므로** 받아 적게만 한다
6. **S3 게시** — 모든 명령은 dry-run이 기본이다. 출력을 확인한 뒤 `--execute`를 붙인다
   ```bash
   python3 -m scripts.expression_pronunciation_audio build-manifest --source tts_source.json --work-dir work --output work/manifest.json
   python3 -m scripts.expression_pronunciation_audio verify         --manifest work/manifest.json --work-dir work
   python3 -m scripts.expression_pronunciation_audio upload         --manifest work/manifest.json --work-dir work --bucket landit-content-982529430654 --boto3 --execute
   python3 -m scripts.expression_pronunciation_audio build-be-manifest --manifest work/manifest.json --source tts_source.json --bucket landit-content-982529430654 --execute
   python3 -m scripts.expression_pronunciation_audio upload-reference  --reference-dir out/final --tts-manifest-key {upload가 출력한 매니페스트 키} --bucket landit-content-982529430654 --execute
   ```
   - 끝나면 터미널에 **`be_manifest_key=...` 1줄과 `reference_key=...` 3줄**이 출력된다
   - 이 키 4개는 마이그레이션 머리 주석에 근거로 적는다
7. **SQL 생성** — 게시된 파일 4개를 내려받아 `build_pronunciation_asset_sql.py`로 UPSERT SQL을 만든다
   - 생성기는 BE 임포트와 같은 검증을 한다
     - 문장 일치
     - order 연속·중복
     - 단어 수
     - 패턴형이 아닌데 표현 음성이 없는지
   ```bash
   python3 build_pronunciation_asset_sql.py \
       --expressions-sql {표현 INSERT SQL} \
       --be-manifest published/be-manifest.json --reference-dir published/ \
       --be-manifest-key {be_manifest_key} \
       --reference-keys {EN_US_KEY} {EN_GB_KEY} {EN_AU_KEY} \
       --source-sha256 {tts_source의 source_sha256} \
       --out V{N}__insert_..._pronunciation_assets.sql
   ```
8. **마이그레이션 작성**
   - 파일은 `src/main/resources/db/postgresql/V{N}__....sql`에 둔다
     - postgresql 벤더 전용이라 H2 테스트에서는 실행되지 않는다
     - 그래서 계약 테스트를 따로 둔다
   - 번호는 **표현 INSERT 마이그레이션보다 크게**(FK), 그리고 V126보다 크게 정한다
     - 벤더 디렉터리도 버전 번호를 공유한다
     - 머지 직전에 develop의 최대 번호를 다시 확인한다
   - 생성기가 넣어 주는 검사는 "행 수 = 문장 URL 있는 행 수" 하나뿐이다
   - V95처럼 아래 검사를 직접 보강한다
     - **적재 전**: 대상 표현의 표현·예문 글자를 음성을 만든 원문과 전부 대조한다
       - 하나라도 다르면 `RAISE EXCEPTION`으로 중단한다
       - 임포트 API의 "문장이 DB와 다릅니다" 검증을 옮긴 것이다
     - **적재 후**:
       - 총 행 수와 억양별 행 수
       - 문장 URL NULL 0
       - 표현 URL NULL 개수 = 패턴형 표현 수 × 3
     - **단어 URL 형식**: `words[*].audioUrl`이 전부 `word/{억양}/{해시}.mp3` 형식이고 URL의 억양이 행의 억양과 같은지
   - 계약 테스트를 추가한다. 예: `Lan471PronunciationAssetMigrationTests`
     - SQL 문자열로 표현×억양 대응, URL 형식, 검사 블록 존재를 확인한다
9. **dev 롤백 드라이런** — 실제 DB에서 한 트랜잭션으로 돌리고 되돌린다
   - 표현 INSERT 마이그레이션이 아직 dev에 없으면 그 파일도 같은 트랜잭션에서 먼저 `\i`한다
   ```bash
   PGPASSWORD="$(aws ssm get-parameter --name /landit/develop/DB_PASSWORD --with-decryption --query Parameter.Value --output text)" \
     psql "$DEV_DB_URI" -v ON_ERROR_STOP=1 -c 'BEGIN' -f {표현 INSERT 마이그레이션} -f {자산 마이그레이션} -c 'ROLLBACK'
   ```
   - `$DEV_DB_URI`는 Supabase Session pooler URI다
   - Supabase 대시보드에서 비밀번호를 리셋하지 않는다(dev BE가 SSM 값으로 접속 중이다)
   - 검사가 일부러 틀린 입력에서 중단되는지도 한 번 확인한다(예: 문장 한 글자 변경)
10. **PR·배포** — 표현 INSERT PR을 먼저 머지·배포하고, 자산 PR을 그 뒤에 배포한다
    - 마이그레이션은 앱 기동이 아니라 배포 workflow의 앞 단계(`flyway-migration.yml` → `./gradlew migrateDatabase`)에서 돈다
      - 검사가 실패하면 이 단계가 실패해서 **그 환경의 배포 전체가 막힌다**
      - 서버는 이전 버전 그대로 돈다
      - 그래서 9번 드라이런을 건너뛰지 않는다
    - 배포는 수동(`workflow_dispatch`)이라 머지만으로는 적용되지 않는다
      - 표현 PR과 자산 PR이 **둘 다 develop에 들어간 뒤에** 배포하면 한 번에 번호 순서대로 적용된다
      - 자산 PR만 들어간 상태에서 누가 배포하면 적재 전 검사에서 막힌다
11. **전수 확인**
    ```
    GET /api/v1/admin/expressions/pronunciation-assets/coverage
    ```
    - 모든 억양에서 `referenceMissing: []`, `audioMissing: []`이면 완료다
    - prod Swagger는 꺼져 있어 curl로 호출한다(어드민 토큰 필요)

## 절차 B — 표현 몇 개만 추가하거나 다시 만들 때

1. `coverage`에 뜬 표현 ID나 수정한 표현 ID가 대상이다
2. 절차 A의 2~11을 대상 표현만으로 진행한다
   - `build_tts_source.py --ids`로 대상만 고른다
   - `--reuse-s3-bucket`이 있으면 이미 게시된 음성은 다시 만들지 않는다
3. 마이그레이션은 `ON CONFLICT ... DO UPDATE`라서 기존 행을 새 값으로 갈아끼운다

## 절차 C — 문장을 수정했을 때

문장이 바뀐 표현은 기준 데이터와 TTS가 모두 낡은 것이 된다.

1. 새 문장으로 절차 A의 2~7을 진행한다
2. 문장 수정 마이그레이션과 자산 마이그레이션을 **연속 번호로 같이 배포**한다
   - 사이가 벌어지면 그동안 낡은 자산(옛 문장 음성)이 서빙된다
   - 자산 마이그레이션의 적재 전 대조가 새 문장을 기준으로 하므로, 순서가 뒤집히면 배포가 중단된다

## 절차 D — 음성 파일만 교체할 때 (같은 키)

QA로 불량을 찾아 같은 텍스트로 다시 합성한 경우다. 키가 텍스트 해시라서 **DB는 바꾸지 않는다.**

1. `upload --replace --boto3 --execute`로 같은 키를 덮어쓴다(되돌릴 수 없다)
2. 파일이 immutable 캐시라서 CloudFront 무효화가 필요하다(upload 출력의 안내 명령 사용)
3. 단어는 공용 자리라서, 하나를 교체하면 **그 단어를 쓰는 모든 표현**에 반영된다

## 자주 나오는 실패와 대처

| 증상 | 원인 | 대처 |
| --- | --- | --- |
| 배포 때 마이그레이션 `RAISE EXCEPTION`(문장 대조) | 표현 SQL과 음성의 문장 버전이 다름 | 최종 문장으로 절차 A 2~7 재생성, 또는 표현 PR 배포 순서 확인 |
| 배포 때 FK 위반 | 표현 INSERT보다 자산 마이그레이션이 먼저 적용됨 | 번호·배포 순서 확인 (표현 → 자산) |
| 단어 URL이 `{표현id}/.../word/{순서}/...` 형식 | LAN-561 이전 파이프라인으로 생성 | 최신 iac `main`으로 다시 생성·게시 |
| 생성기 "reference 키 집합이 SQL 표현×3과 다르다" | 표현 SQL과 게시 파일의 대상 표현이 다름 | 같은 `expressions.json`에서 만든 게시 파일인지 확인 |
| `generate`가 로그 없이 멈춤 | SSL 인증서 없음 | `SSL_CERT_FILE` 설정 후 재실행 |
| `state.json` 스키마 거부 | 옛 작업 폴더 재사용 | 새 작업 폴더에서 시작 |
| `verify-accent`가 "대조 충돌"로 중단 | 한 단어 클립에 서로 다른 억양 대조가 붙음 | landit-ai에서 대조를 정리한 뒤 재생성 |
| 앱에서 발음 파트가 안 보임 | 그 표현의 자산이 없음 (learning-start가 URL을 null로 내림) | `coverage`로 확인 후 절차 B |
| 발음 평가 API 404 PRONUNCIATION_DATA_NOT_FOUND | 자산 없음 또는 음성 URL 비어 있음 | `coverage`로 확인 후 절차 B |

## 예비 수단 — 어드민 임포트 API

Flyway 적재가 기본이다. API는 긴급하게 한 환경만 고쳐야 할 때만 쓴다. 이렇게 넣은 행은
다른 환경에 자동으로 따라가지 않는다.

```
POST /api/v1/admin/expressions/pronunciation-assets/import-reference-from-s3?manifestKey={reference_key}  # 억양별 3회
POST /api/v1/admin/expressions/pronunciation-assets/import-tts-from-s3?manifestKey={be_manifest_key}      # 1회
```

- 기준 데이터 임포트는 해당 자산의 음성 URL을 초기화한다
  - 두 호출은 **반드시 붙여서** 실행한다
  - 두 호출 사이에는 그 표현의 발음 평가가 404를 낸다
- prod에서는 큰 배치 호출이 nginx 60초 타임아웃으로 504를 낸다
  - 서버 처리는 계속되므로 `coverage`로 결과를 확인한다
- `build-be-manifest`·`upload-reference`로 게시한 파일 키만 받는다
  - 작업 매니페스트 키를 넣으면 모양이 달라 파싱에 실패한다

## 참고

- 설계·구현 배경: `docs/tasks/LAN-342/` (노션 이슈 LAN-342, AI 서버는 LAN-373)
- Flyway 적재 선례: `db/postgresql/V95__insert_free_talk_expression_pronunciation_assets.sql` (LAN-471)
- 단어 공용 풀: landit-iac `docs/handoffs/lan-475-shared-word-pool.md`, BE `db/postgresql/V126__share_expression_pronunciation_word_audio_urls.sql`
- 관련 코드:
  - 임포트·커버리지: `feature/content/expression/pronunciation/admin/service/ExpressionPronunciationAssetService.java`
  - S3 읽기: `feature/content/expression/pronunciation/client/S3PronunciationManifestReader.java`
- 파이프라인 코드:
  - landit-iac `scripts/expression_pronunciation_audio.py`
  - landit-iac `scripts/expression_pronunciation_qa.py`
  - landit-ai `scripts/generate_pronunciation_reference.py`, `build_tts_source.py`, `prune_accent_contrasts.py`
