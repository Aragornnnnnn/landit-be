# Writing 표현 사용 영역 분리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Writing 표현을 `SCENARIO`와 `FREE_TALK` 사용 영역으로 명확히 구분하고 프리톡 추천에서 공용 프리톡 표현만 조회한다.

**Architecture:** `writing_expression.expression_source`를 콘텐츠 사용 영역의 단일 기준으로 추가한다. 기존 `expression_type`은 의미 분류로 유지하고, `scenario_id`와 `owner_user_profile_id` 조합은 DB 제약 조건으로 `expression_source`와 일치시킨다.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, PostgreSQL/H2, Flyway, JUnit 5, Mockito

## Global Constraints

- 기존 시나리오 표현은 `SCENARIO`로 백필한다.
- 기존 프리톡 개인 생성 표현은 `FREE_TALK`으로 백필한다.
- 공용 프리톡 표현은 `FREE_TALK`, `scenario_id=null`, `owner_user_profile_id=null`로 저장할 수 있다.
- 프리톡 추천 후보는 공용 `FREE_TALK` 활성 표현 전체이며 100개 제한을 두지 않는다.
- AI가 선택한 기존 표현은 저장 전에 동일한 공용 프리톡 후보 조건으로 다시 검증한다.
- 240개 표현 데이터 자체는 제공된 파일이 없으므로 이번 변경에 포함하지 않는다.

---

### Task 1: 표현 사용 영역 스키마와 도메인 추가

**Files:**
- Create: `src/main/resources/db/migration/V36__add_writing_expression_source.sql`
- Create: `src/main/java/com/landit/landitbe/feature/content/domain/WritingExpressionSource.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/domain/WritingExpression.java`
- Modify: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`
- Modify: `src/test/java/com/landit/landitbe/feature/content/service/ExpressionQueryServiceTest.java`

**Interfaces:**
- Produces: `WritingExpressionSource.SCENARIO`, `WritingExpressionSource.FREE_TALK`
- Produces: `WritingExpression#getExpressionSource()`

- [x] **Step 1: 마이그레이션과 생성 표현 저장값의 실패 테스트를 작성한다.**

```java
assertColumnExists("writing_expression", "expression_source");
assertTableConstraintExists("writing_expression", "chk_writing_expression_source");
assertThat(savedExpression.getExpressionSource()).isEqualTo(WritingExpressionSource.FREE_TALK);
```

- [x] **Step 2: 테스트가 컬럼과 enum 부재로 실패하는지 확인한다.**

Run: `./gradlew test --tests '*DatabaseSchemaIntegrationTests' --tests '*ExpressionQueryServiceTest' --no-daemon --console=plain`

- [x] **Step 3: V36 마이그레이션과 enum, 엔티티 매핑을 최소 구현한다.**

```sql
ALTER TABLE writing_expression
    ADD COLUMN expression_source VARCHAR(20) DEFAULT 'SCENARIO';
UPDATE writing_expression
SET expression_source = CASE WHEN scenario_id IS NULL THEN 'FREE_TALK' ELSE 'SCENARIO' END;
ALTER TABLE writing_expression ALTER COLUMN expression_source SET NOT NULL;
```

```java
public enum WritingExpressionSource {
  SCENARIO,
  FREE_TALK
}
```

- [x] **Step 4: 대상 테스트가 통과하는지 확인한다.**

Run: `./gradlew test --tests '*DatabaseSchemaIntegrationTests' --tests '*ExpressionQueryServiceTest' --no-daemon --console=plain`

- [x] **Step 5: 논리 변경을 커밋한다.**

```bash
git commit -m "feat: Writing 표현의 시나리오와 프리톡 사용 영역 분리"
```

### Task 2: 프리톡 공용 표현 추천 조회 전환

**Files:**
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/WritingExpressionRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ExpressionQueryService.java`
- Modify: `src/test/java/com/landit/landitbe/feature/content/service/ExpressionQueryServiceTest.java`

**Interfaces:**
- Consumes: `WritingExpressionSource.FREE_TALK`
- Produces: `findPublicExpressionCandidates(...)`

- [x] **Step 1: 프리톡 공용 표현만 제한 없이 요청하는 실패 테스트를 작성한다.**

```java
verify(writingExpressionRepository)
    .findPublicExpressionCandidates(
        WritingExpressionSource.FREE_TALK, Locale.EN, Locale.KR, ActiveStatus.ACTIVE);
```

- [x] **Step 2: 새 repository 메서드 부재로 컴파일이 실패하는지 확인한다.**

Run: `./gradlew test --tests '*ExpressionQueryServiceTest' --no-daemon --console=plain`

- [x] **Step 3: repository 조회 조건과 서비스를 최소 수정한다.**

```java
List<WritingExpression>
    findPublicExpressionCandidates(
        WritingExpressionSource expressionSource,
        Locale targetLocale,
        Locale baseLocale,
        ActiveStatus status);
```

- [x] **Step 4: 대상 테스트와 전체 검증을 실행한다.**

Run: `./gradlew test --tests '*ExpressionQueryServiceTest' --no-daemon --console=plain`

Run: `./gradlew check --rerun-tasks --no-daemon --console=plain`

- [x] **Step 5: 논리 변경을 커밋한다.**

```bash
git commit -m "feat: 프리톡 공용 표현만 추천 후보로 조회"
```

## 검증 결과

- RED: `WritingExpressionSource`와 공용 프리톡 검증 메서드 부재로 대상 테스트가 실패했다.
- GREEN: `ExpressionQueryServiceTest`가 통과했다.
- 전체 검증: `./gradlew check --rerun-tasks --no-daemon --console=plain`이 통과했다.
