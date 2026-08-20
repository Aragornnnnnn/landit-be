# LAN-232: 프리톡 대화 → 벡터 유사도 기반 표현 추천

## 개요

프리톡 완료 후 표현 추천의 후보 선정을 "활성 공용 표현 전체(818건)를 추천 LLM에 전달"에서
"대화 임베딩과 가까운 소수 후보(최대 30건)만 전달"로 교체한다. LAN-291(V51·V52)이 적재한
`writing_expression.embedding vector(1536)`을 검색에 사용한다.

- 추천 LLM 입력의 후보 목록이 818건(~32k 토큰) → 최대 30건(~1.2k 토큰)으로 축소
- 후보 풀이 커져도 세션당 비용이 고정됨
- 프론트 계약(API 요청/응답, 상태 전이)은 변경 없음

## 파이프라인

| 단계 | 주체 | 내용 |
| --- | --- | --- |
| 1 | BE | 프리톡 완료 → 세션 잠금·PREPARING 선점, 대화 로드 (기존) |
| 2 | BE→AI | `POST /api/v1/free-talk/conversation-embeddings` — 사용자 발화 중 핵심 1~4개 추출 + 임베딩 (신규, [ai-contract.md](ai-contract.md)) |
| 3 | BE→DB | 벡터별 pgvector 코사인 거리 검색, 학습 완료 표현 제외, 상위 30건 |
| 4 | BE | 표현별 최소 거리 병합 → 임계값 필터 → 통과 0건이면 최상위 1건 유지 |
| 5 | BE→AI | 기존 `expression-recommendations` — 추린 후보만 전달, LLM이 1~3건 확정 |
| 6 | BE | ID 검증 → `free_talk_session_expression` 저장 → READY (기존) |

- 핵심 선정 로직: `feature/session/service/ExpressionCandidateSelector`
- 검색: `feature/content/repository/ExpressionEmbeddingSearchRepository`
  - `PgVector...` 구현 (운영, DB가 `<=>` 계산) / `InMemory...` 구현 (H2 테스트·로컬, 자바 코사인)
  - H2에는 vector 타입이 없어 실행 분리가 필요하며, AI 클라이언트의 remote/local 스위치와 같은 패턴
- 학습 완료 표현 제외: `user_writing_expression_completion` NOT EXISTS (완료만 제외,
  추천됐지만 미학습인 표현은 재추천 허용)

## 설정 (`landit.expression-search`)

| 프로퍼티 | 환경변수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `mode` | `LANDIT_EXPRESSION_SEARCH_MODE` | pgvector | H2 테스트만 `in-memory`로 재정의한다 |
| `max-candidates` | `LANDIT_EXPRESSION_SEARCH_MAX_CANDIDATES` | 30 | 추천 LLM에 전달할 최대 후보 수 |
| `distance-threshold` | `LANDIT_EXPRESSION_SEARCH_DISTANCE_THRESHOLD` | 0.6 | 후보 인정 최대 코사인 거리. **실측 근거 없는 시작점** — 배포 후 추천 품질을 보고 조정 |

## 배포 체크리스트

1. **AI 서버의 `conversation-embeddings`(LAN-303)와 함께 배포** ([ai-contract.md](ai-contract.md) 전달)
   — 동시 배포 가능. BE가 먼저 뜨는 겹침 구간에 프리톡을 완료한 유저의 표현 생성만
   일시 FAILED가 되며, AI 배포 완료 후 재시도로 복구된다. 겹침 영향조차 없애려면
   AI 먼저 배포 (AI 선배포는 무해 — 새 엔드포인트가 대기할 뿐)
2. ~~dev/prod 환경변수에 `LANDIT_EXPRESSION_SEARCH_MODE=pgvector` 추가 (SSM/태스크 정의)~~
   — 환경변수 누락으로 dev에서 in-memory 구현이 동작해 후보 검색에 26초가 걸렸다.
   기본값을 `pgvector`로 뒤집어 설정 없이도 운영 경로가 선택되도록 변경했으므로 별도 조치가 필요 없다.
3. dev 배포 후 pgvector 실쿼리 1회 확인 (H2에서는 `<=>` 실행 검증 불가):
   프리톡 1회 완주 → 세션 상세에서 READY + 추천 표현 확인
4. 실패 시 원복: `LANDIT_EXPRESSION_SEARCH_MODE=in-memory`를 명시하면 되돌릴 수 있지만
   운영 규모에는 부적합 — 코드 롤백이 원칙

## 실패 정책

- 검색 후보 0건(임베딩 데이터 없음 등) → FAILED (재시도 유도). 데이터 사고를 숨기지 않기 위함
- 유저가 공용 표현을 전부 학습 완료한 경우도 0건 → FAILED가 되는데, 단기에는 도달 불가능한
  규모(818건)라 수용. 장기 대응(완료 제외를 풀고 복습으로 재추천하는 fallback)은 기획 확인 후
  별도 이슈

## 남은 작업 (별도 트래킹)

- [ ] AI 서버 `conversation-embeddings` 구현 (AI팀, 계약 전달됨)
- [ ] `distance-threshold`·`max-candidates` 실측 튜닝 (평가셋: 유저 발화 → 정답 표현 쌍)
- [ ] 전량 학습 유저 대응 — 복습 재추천 fallback (기획 확인 필요)
- [ ] 후보 풀 수천 건 이상 확장 시 HNSW 인덱스 도입 (818건은 seq scan으로 충분)
