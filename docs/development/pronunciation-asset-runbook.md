# 발음 평가 자산 운영 절차 (Runbook)

발음 평가 기능(LAN-342)의 자산 — 원어민 TTS 음성과 발음 기준 데이터 — 을 만들고 DB에 넣는 절차다.
writing_expression 표현을 새로 추가했거나, 문장을 수정했거나, 음성을 교체할 때 이 문서를 따라 하면 된다.

## 전체 구조 한 장 요약

```
[AI 파이프라인(landit-ai)]  기준 데이터 JSON 생성 (발음 표기·음절·강세·억양 대조) + TTS 주문서 변환
[TTS 배치(landit-iac)]      mp3 생성·검증 (표현·문장·단어 × 3억양) → S3 업로드
                            → BE 임포트용 파일 2종(BE 매니페스트·기준 데이터)도 S3에 게시
[BE 어드민 API]             두 파일을 S3에서 읽어 expression_pronunciation_asset 테이블에 저장
[커버리지 API]              전수 대조로 빠진 표현이 없는지 확인
```

모든 생성·게시 단계는 사람이 노트북 터미널에서 실행한다 (자동 실행 없음). BE 임포트용 파일은
반드시 landit-iac의 `build-be-manifest`·`upload-reference` 명령으로 게시해야 한다 — 이 명령들이
BE 파서가 기대하는 JSON 모양(확정 계약)으로 변환·검증해서 올린다.

- 자산 테이블: `expression_pronunciation_asset` — (표현, 억양)당 1행. 981개 표현 × 3억양 = 2,943행
- 판정 기준 음성 = 앱 "원어민 발음 듣기" 음성 = 같은 파일. 유저가 고른 AI 튜터의 억양이 기준
- TTS 파일명은 (모델·보이스·텍스트) 해시라, **문장이 바뀌면 그 표현의 음성은 재생성**해야 한다

## 확정 설정 (바꾸면 전량 재생성이므로 주의)

| locale | 모델 | 보이스 |
| --- | --- | --- |
| EN_US | deepgram/aura-2 | aura-2-thalia-en (미국 여성) |
| EN_GB | deepgram/aura-2 | aura-2-pandora-en (영국 여성) |
| EN_AU | deepgram/aura-2 | aura-2-theia-en (호주 여성) |

- 포맷터: google-java-format과 무관. TTS는 OpenRouter 경유, 핑거프린트 파일명
- S3 경로: `landit-content-982529430654/content/expression-pronunciation-audio/...`
- BE는 이 경로의 읽기 권한만 가진다 (Task Role, 테라폼 관리)

## 절차 A — 최초 전량 구축 / 대량 재생성

**순서가 중요하다: 배포 → 재료 → 기준 데이터 → TTS → 게시 → 임포트 → 확인.**

1. **BE 배포 확인** — 대상 마이그레이션(V59·V60·V61·V62)이 적용된 상태여야 한다
2. **재료 준비** — DB에서 활성 표현을 JSON으로 export:
   `[{"expressionId": 1, "expressionText": "...", "sentenceText": "..."}]` (`expressions.json`)
3. **기준 데이터 생성** — landit-ai 레포에서 locale별 3회 실행 (발음 사전 기반, AI 호출 없음):
   ```
   .venv/bin/python scripts/generate_pronunciation_reference.py \
       --input expressions.json --locale EN_US --out-dir out/final   # EN_GB, EN_AU 동일
   .venv/bin/python scripts/build_tts_source.py \
       --expressions expressions.json --reference-dir out/final --out tts_source.json
   ```
4. **TTS 생성·검증** — landit-iac 레포에서 순서대로 (`OPENROUTER_API_KEY` 필요):
   ```
   python3 scripts/expression_pronunciation_audio.py validate-source --source tts_source.json
   python3 scripts/expression_pronunciation_audio.py generate      --source tts_source.json --work-dir work/ --reuse-s3-bucket landit-content-982529430654
   python3 scripts/expression_pronunciation_audio.py verify        --source tts_source.json --work-dir work/
   python3 scripts/expression_pronunciation_audio.py verify-accent --source tts_source.json --work-dir work/
   ```
   `verify-accent` 실패 시: 일시적이면 해당 mp3 삭제 후 `generate` 재실행, 계통적이면 landit-ai의
   `prune_accent_contrasts.py`로 대조 제거 후 3번(`build_tts_source`)부터 재실행 (보통 2~3회에 수렴).
   **프루닝을 돌았으면 이후 단계는 반드시 최종 reference·tts_source 기준으로 진행할 것**
5. **S3 게시** — landit-iac에서 mp3 업로드 후 BE 임포트용 파일 2종 게시
   (모두 dry-run이 기본, 출력 확인 후 `--execute`로 실행):
   ```
   python3 scripts/expression_pronunciation_audio.py build-manifest --source tts_source.json --work-dir work/ --output work/manifest.json
   python3 scripts/expression_pronunciation_audio.py verify         --manifest work/manifest.json --work-dir work/
   python3 scripts/expression_pronunciation_audio.py upload         --manifest work/manifest.json --work-dir work/ --bucket landit-content-982529430654 --execute
   python3 scripts/expression_pronunciation_audio.py build-be-manifest --manifest work/manifest.json --source tts_source.json --bucket landit-content-982529430654 --execute
   python3 scripts/expression_pronunciation_audio.py upload-reference  --reference-dir out/final --tts-manifest-key {upload가 출력한 매니페스트 키} --bucket landit-content-982529430654 --execute
   ```
   끝나면 터미널에 **`be_manifest_key=...` 1줄과 `reference_key=...` 3줄**이 출력된다.
   이 4개 키가 다음 단계에서 Swagger에 넣을 값이다 (복사해 둘 것)
6. **기준 데이터 임포트** — Swagger(어드민 토큰)에서 locale별로 3회 호출:
   ```
   POST /api/v1/admin/expressions/pronunciation-assets/import-reference-from-s3?manifestKey={reference_key}
   ```
   응답의 `failures`가 비어 있는지 확인. "문장이 DB와 다릅니다" 실패가 나오면 → 그 표현의 문장이
   기준 데이터 생성 이후 바뀐 것. 해당 표현만 2번부터 재생성
7. **TTS 임포트** — 1회 호출:
   ```
   POST /api/v1/admin/expressions/pronunciation-assets/import-tts-from-s3?manifestKey={be_manifest_key}
   ```
   "기준 데이터가 없습니다" 실패 = 6번을 건너뛴 것. "order가 맞지 않습니다" = 기준 데이터와 TTS가
   서로 다른 버전의 문장으로 만들어진 것 → 둘 다 같은 문장 기준으로 재생성
8. **전수 확인** — 1회 호출:
   ```
   GET /api/v1/admin/expressions/pronunciation-assets/coverage
   ```
   모든 억양에서 `referenceMissing: []` 그리고 `audioMissing: []`이면 완료.
   빠진 ID가 있으면 그 표현만 다시 2~7 진행

## 절차 B — 표현 몇 개만 추가/수정했을 때 (증분)

1. `coverage` 호출 → `referenceMissing`/`audioMissing`에 뜬 표현 ID가 작업 대상
   (커버리지가 "뭘 만들어야 하는지"의 정답지다 — 사람이 기억할 필요 없음)
2. 그 표현들만 대상으로 절차 A의 2~5 진행. `build_tts_source.py`의 `--ids 164,177` 옵션으로
   대상 표현만 골라 돌릴 수 있고, 이미 S3에 있는 mp3는 재사용되므로 전량 재실행해도 안전하다
3. 절차 A의 6~8과 동일하게 임포트 → 커버리지 재확인.
   임포트는 upsert라 기존 행은 영향받지 않는다

## 절차 C — 문장을 수정했을 때 (V60 같은 데이터 마이그레이션)

문장이 바뀐 표현은 기준 데이터·TTS 모두 낡은 것이 된다:

1. 문장 수정 마이그레이션 배포
2. 해당 표현들의 기준 데이터·TTS를 새 문장으로 재생성 (절차 A의 2~5)
3. 절차 A의 6~8 — 임포트가 upsert로 갈아끼운다.
   재임포트 전까지 기존(낡은) 자산이 계속 서빙되므로, 문장 수정과 자산 재생성은 붙여서 진행할 것

## 자주 나오는 실패와 대처

| 증상 | 원인 | 대처 |
| --- | --- | --- |
| 임포트 404 (매니페스트) | S3 키 오타 또는 업로드 안 됨 | 배치 출력의 키를 그대로 복사했는지 확인 |
| 임포트 400 | 매니페스트 JSON 형식 오류 | 생성 배치 로그 확인, 파일 직접 열어 검증 |
| failures: "문장이 DB와 다릅니다" | 문장 수정 후 낡은 기준 데이터 | 절차 C |
| failures: "기준 데이터가 없습니다" | TTS를 먼저 임포트함 | 기준 데이터부터 (절차 A 6번) |
| 임포트 400 (파싱 실패) | BE용 파일을 `build-be-manifest`/`upload-reference`로 게시하지 않음 | 절차 A 5번의 두 명령으로 다시 게시 (작업 매니페스트 키를 넣으면 모양이 달라 파싱 실패) |
| failures: "order가 맞지 않습니다" | 기준 데이터와 TTS의 문장 버전 불일치 | 같은 문장 기준으로 둘 다 재생성 |
| 앱에서 발음 파트가 안 보임 | 그 표현의 자산 미구축 (learning-start가 URL을 null로 내림) | coverage로 확인 후 절차 B |
| 발음 평가 API 404 PRONUNCIATION_DATA_NOT_FOUND | 자산 없음 또는 TTS 미완성 | coverage로 확인 후 절차 B |

## 참고

- 설계·구현 배경: `docs/tasks/LAN-342/` (노션 이슈 LAN-342, AI 서버는 LAN-373)
- 관련 코드: `feature/content/service/ExpressionPronunciationAssetService.java` (임포트·커버리지),
  `feature/content/client/PronunciationManifestReader.java` (S3 읽기)
- 어드민 페이지에 임포트·커버리지 화면을 붙이는 작업은 후속 백로그 (그 전까지는 Swagger 사용)
