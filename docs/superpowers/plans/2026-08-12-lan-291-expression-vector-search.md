# LAN-291 Expression Embedding Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 공용 프리톡 표현과 사전 생성 임베딩을 함께 적재하고 사용자 전용 NEW 표현 생성 경로를 제거한다. 벡터 유사도 검색은 후속 이슈에서 구현한다.

**Architecture:** 배포 전에 고정 형식 텍스트를 1,536차원으로 임베딩해 V52 SQL에 포함한다. 운영 중 AI·BE는 임베딩을 생성하지 않으며, 기존 추천 LLM은 활성 공용 표현 전체를 받아 기존 ID를 반환한다.

**Tech Stack:** Java 21, Spring Boot 4, JPA/JdbcTemplate, PostgreSQL pgvector, Flyway V51, H2 test migration.

## Global Constraints

- migration 번호는 열린 PR의 V50 이후인 V51이다.
- `openai/text-embedding-3-small`과 1,536차원을 사용한다.
- `target_expression_text`와 `usage_summary`는 설계 문서의 고정 라벨 형식으로 결합한다.
- 런타임 임베딩 API와 backfill은 만들지 않는다.

---

### Task 1: 스키마 정리와 pgvector 컬럼

**Files:**
- Create: `src/main/resources/db/postgresql/V51__prepare_writing_expression_embeddings.sql`
- Create: `src/main/resources/db/h2/V51__prepare_writing_expression_embeddings.sql`
- Modify: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`

**Interfaces:**
- Produces: PostgreSQL `writing_expression.embedding extensions.vector(1536)`과 owner 없는 표현 스키마.

- [x] 스키마 기대 조건을 테스트에 먼저 추가하고 실패를 확인한다.
- [x] PostgreSQL과 H2 V51 migration을 최소 구현한다.
- [x] `DatabaseSchemaIntegrationTests`를 다시 실행해 통과시킨다.
- [x] migration 변경을 커밋한다.

### Task 2: EXISTING-only 도메인 계약

**Files:**
- Modify: `WritingExpression.java`, `WritingExpressionRepository.java`, `ExpressionQueryService.java`.
- Modify: `FreeTalkExpressionGenerationService.java`와 AI DTO·클라이언트.
- Delete: NEW 전용 콘텐츠 DTO와 사용되지 않는 출처 enum.
- Test: 프리톡 서비스·클라이언트·통합 테스트.

**Interfaces:**
- Produces: 추천 결과의 `existingExpressionId()`와 기존 표현 세션 연결만 사용하는 계약.

- [x] NEW 응답이 허용되지 않고 사용자 전용 표현이 저장되지 않는 실패 테스트를 작성한다.
- [x] 테스트의 예상 실패를 확인한다.
- [x] NEW 생성 분기와 owner 접근 제어를 제거한다.
- [x] 관련 테스트를 통과시키고 커밋한다.

### Task 3: 사전 생성 임베딩 데이터

**Files:**
- Create: `src/main/resources/db/postgresql/V52__insert_free_talk_expressions_with_embeddings.sql`.
- Test: 818개 행, 벡터 개수와 차원, owner 컬럼 제거를 검증한다.

**Interfaces:**
- Consumes: 배포 전에 생성한 818개 임베딩.
- Produces: 표현 ID `164~981`과 임베딩을 함께 적재하고 identity sequence를 동기화하는 V52 migration.

- [x] 원본 SQL 818행과 입력 결합 규칙을 검증한다.
- [x] OpenRouter에서 1,536차원 임베딩 818개를 생성한다.
- [x] V52 INSERT에 임베딩과 표현 ID `164~981`을 함께 포함한다.
- [x] 런타임 임베딩 API와 backfill 코드가 없음을 확인한다.

### Task 4: 전체 검증

**Files:**
- Modify: `docs/superpowers/plans/2026-08-12-lan-291-expression-vector-search.md` 검증 기록.

**Interfaces:**
- Produces: LAN-291 BE 검증 증거.

- [ ] Spotless, 관련 테스트, 전체 `check`를 실행한다.
- [ ] PostgreSQL migration SQL과 열린 PR migration 번호를 다시 확인한다.
- [ ] `git diff --check`, `git status --short`를 확인한다.
- [ ] 검증 결과를 기록하고 커밋한다.
