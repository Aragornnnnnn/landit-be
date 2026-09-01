# LAN-426 Apple 사용자 이전 CLI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 운영자가 앱 이전 전후에 GitHub Actions를 수동 실행해 기존 Apple OAuth identity를 같은 사용자 프로필에 유지한 채 새 Team 식별자로 일괄 변환한다.

**Architecture:** 웹 애플리케이션과 분리된 Java CLI가 JDBC 저장소와 Apple REST 클라이언트를 조합해 `PREPARE`와 `COMPLETE`를 실행한다. 사용자별 상태를 DB에 저장하고 한 건씩 커밋해 부분 실패와 재실행을 지원하며, GitHub Actions가 운영 자격 증명을 SSM에서 주입한다.

**Tech Stack:** Java 21, Gradle JavaExec, JDBC, JDK HttpClient, Jackson, Flyway, H2/PostgreSQL, GitHub Actions, AWS SSM Parameter Store

**Spec:** `docs/tasks/LAN-426/design.md`

## Global Constraints

- 작업 브랜치는 최신 `main`에서 생성한 `hotfix/LAN-426`이다.
- 대상은 `provider = APPLE`, `status = ACTIVE`인 OAuth identity다.
- 관리자 API·UI와 로그인 중 자동 보정 fallback은 추가하지 않는다.
- `oauth_identity.user_profile_id`는 변경하지 않는다.
- Apple 식별자, 이메일, access token, client secret을 로그와 테스트 출력에 노출하지 않는다.
- Flyway 버전은 `V74`를 사용한다.
- 모든 새 Java 소스 첫 줄에 역할을 설명하는 한국어 주석을 작성하고 공개 타입·메서드에 Javadoc을 작성한다.
- 각 동작은 실패 테스트를 먼저 확인한 뒤 최소 구현으로 통과시킨다.

---

### Task 1: 이전 상태 스키마와 JDBC 저장소

**Files:**
- Create: `src/main/resources/db/migration/V74__add_apple_user_migration.sql`
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationPhase.java`
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationCandidate.java`
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationSummary.java`
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationRepository.java`
- Create: `src/test/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationRepositoryTest.java`

**Interfaces:**
- Produces: `AppleUserMigrationRepository(String dbUrl, String username, String password)`
- Produces: `int initializePending()`, `List<AppleUserMigrationCandidate> findCandidates(AppleUserMigrationPhase phase)`, `void markPrepared(long migrationId, String transferSub)`, `void markFailed(long migrationId, AppleUserMigrationPhase phase, String failureCode)`, `void complete(long migrationId, String providerUserId, String providerEmail)`, `AppleUserMigrationSummary summarize(AppleUserMigrationPhase phase)`
- Produces: `AppleUserMigrationCandidate(long migrationId, long oauthIdentityId, String providerUserId, String transferSub)`
- Produces: `AppleUserMigrationSummary(long targetCount, long successCount, long failureCount, long unresolvedCount)` and `boolean completed()`

- [x] **Step 1: Write failing repository integration tests**

Create an H2 schema containing `user_profile` and `oauth_identity`, apply `V74`, and use literal fixtures to verify:

```java
@Test
void initializePendingTargetsOnlyActiveAppleIdentities() {
  repository.initializePending();

  assertThat(repository.findCandidates(AppleUserMigrationPhase.PREPARE))
      .extracting(AppleUserMigrationCandidate::providerUserId)
      .containsExactly("old-apple-sub");
}

@Test
void completeKeepsUserProfileAndUpdatesTheSameIdentityAtomically() {
  long migrationId = preparedMigration("old-apple-sub", "transfer-sub");

  repository.complete(migrationId, "new-apple-sub", "new-private@privaterelay.appleid.com");

  assertThat(identityRow()).containsExactly(1L, "new-apple-sub", "new-private@privaterelay.appleid.com");
  assertThat(migrationStatus(migrationId)).isEqualTo("COMPLETED");
}
```

Also verify duplicate new `sub` and non-active/non-Apple identity leave the old identity unchanged, phase failures remain retry candidates, completed rows are skipped, and phase-aware counts are literal expected values.

- [x] **Step 2: Run the repository test and verify RED**

Run: `./gradlew test --tests '*AppleUserMigrationRepositoryTest'`

Expected: compilation fails because the migration repository types do not exist.

- [x] **Step 3: Add the V74 schema and minimal JDBC implementation**

Create `apple_user_migration` with a unique FK to `oauth_identity`, unique nullable `transfer_sub`, constrained statuses `PENDING`, `PREPARED`, `COMPLETED`, `PREPARE_FAILED`, `COMPLETE_FAILED`, sanitized `failure_code`, attempt count, and timestamps. Use conditional updates and a JDBC transaction in `complete` so the identity change and `COMPLETED` state commit together.

```java
public void complete(long migrationId, String providerUserId, String providerEmail) {
  try (Connection connection = openConnection()) {
    connection.setAutoCommit(false);
    verifyCompletionTarget(connection, migrationId, providerUserId);
    updateOauthIdentity(connection, migrationId, providerUserId, providerEmail);
    markCompleted(connection, migrationId);
    connection.commit();
  }
}
```

- [x] **Step 4: Run the repository test and verify GREEN**

Run: `./gradlew test --tests '*AppleUserMigrationRepositoryTest'`

Expected: PASS.

- [ ] **Step 5: Commit Task 1**

```bash
git add src/main/resources/db/migration/V74__add_apple_user_migration.sql src/main/java/com/landit/landitbe/feature/auth/migration src/test/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationRepositoryTest.java
git commit -m "feat: Apple 사용자 이전 상태 저장소 추가"
```

### Task 2: Apple 사용자 이전 HTTP 클라이언트

**Files:**
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationClient.java`
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationException.java`
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleRecipientUser.java`
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/HttpAppleUserMigrationClient.java`
- Create: `src/test/java/com/landit/landitbe/feature/auth/migration/HttpAppleUserMigrationClientTest.java`

**Interfaces:**
- Produces: `String requestAccessToken()`
- Produces: `String createTransferSub(String accessToken, String providerUserId)`
- Produces: `AppleRecipientUser exchangeTransferSub(String accessToken, String transferSub)`
- Produces: `AppleRecipientUser(String providerUserId, String providerEmail)`
- Produces: `AppleUserMigrationException.failureCode()` containing only a fixed internal classification such as `APPLE_HTTP_400` or `APPLE_RESPONSE_INVALID`

- [ ] **Step 1: Write failing HTTP contract tests**

Use a local JDK `HttpServer` and assert real request bodies and parsed results without printing secrets.

```java
@Test
void requestsMigrationTokenWithClientCredentialsScope() {
  server.respond("/auth/token", 200, "{\"access_token\":\"access-token\"}");

  assertThat(client.requestAccessToken()).isEqualTo("access-token");
  assertThat(server.lastForm())
      .containsEntry("grant_type", "client_credentials")
      .containsEntry("scope", "user.migration")
      .containsEntry("client_id", "app.client")
      .containsEntry("client_secret", "client-secret");
}

@Test
void exchangesTransferIdentifierForRecipientUser() {
  server.respond("/auth/usermigrationinfo", 200,
      "{\"sub\":\"new-sub\",\"email\":\"new@privaterelay.appleid.com\",\"is_private_email\":true}");

  assertThat(client.exchangeTransferSub("access-token", "transfer-sub"))
      .isEqualTo(new AppleRecipientUser("new-sub", "new@privaterelay.appleid.com"));
}
```

Also verify PREPARE includes `sub` and `target`, both migration calls include bearer authorization and credentials, missing JSON fields are rejected, and non-2xx responses expose only sanitized failure codes.

- [ ] **Step 2: Run the client test and verify RED**

Run: `./gradlew test --tests '*HttpAppleUserMigrationClientTest'`

Expected: compilation fails because the client types do not exist.

- [ ] **Step 3: Implement the minimal JDK HttpClient adapter**

Encode `application/x-www-form-urlencoded` values with UTF-8, set a finite request timeout, parse only required JSON fields with Jackson, and throw sanitized exceptions without response bodies or request values.

```java
public interface AppleUserMigrationClient {
  String requestAccessToken();

  String createTransferSub(String accessToken, String providerUserId);

  AppleRecipientUser exchangeTransferSub(String accessToken, String transferSub);
}
```

- [ ] **Step 4: Run the client test and verify GREEN**

Run: `./gradlew test --tests '*HttpAppleUserMigrationClientTest'`

Expected: PASS.

- [ ] **Step 5: Commit Task 2**

```bash
git add src/main/java/com/landit/landitbe/feature/auth/migration src/test/java/com/landit/landitbe/feature/auth/migration/HttpAppleUserMigrationClientTest.java
git commit -m "feat: Apple 사용자 이전 API 클라이언트 추가"
```

### Task 3: 단계별 서비스와 독립 실행 CLI

**Files:**
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationService.java`
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationSettings.java`
- Create: `src/main/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationRunner.java`
- Create: `src/test/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationServiceTest.java`
- Create: `src/test/java/com/landit/landitbe/feature/auth/migration/AppleUserMigrationSettingsTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: Task 1 repository/candidate/summary and Task 2 client/recipient user/exception
- Produces: `AppleUserMigrationSummary run(AppleUserMigrationPhase phase)`
- Produces: `AppleUserMigrationSettings.from(Map<String, String>)`
- Produces: `static int run(Map<String, String> environment)` returning `0` only when the selected phase has no unresolved rows
- Produces: Gradle task `migrateAppleUsers`

- [ ] **Step 1: Write failing service and settings tests**

Use an H2 repository and an in-memory fake `AppleUserMigrationClient` that returns literal results or throws a sanitized exception.

```java
@Test
void prepareContinuesAfterOneUserFailsAndReturnsFailedSummary() {
  client.failCreateFor("old-sub-1", "APPLE_HTTP_400");

  AppleUserMigrationSummary summary = service.run(AppleUserMigrationPhase.PREPARE);

  assertThat(summary).isEqualTo(new AppleUserMigrationSummary(2, 1, 1, 1));
  assertThat(repository.findCandidates(AppleUserMigrationPhase.PREPARE))
      .extracting(AppleUserMigrationCandidate::providerUserId)
      .containsExactly("old-sub-1");
}

@Test
void completeSkipsCompletedUsersWhenRetried() {
  service.run(AppleUserMigrationPhase.COMPLETE);
  client.rejectAnyFurtherCall();

  assertThat(service.run(AppleUserMigrationPhase.COMPLETE).completed()).isTrue();
}
```

Also verify token failure aborts before user processing, COMPLETE updates all successful rows, missing required environment values fail without echoing their values, and target Team ID is required only for PREPARE.

- [ ] **Step 2: Run service/settings tests and verify RED**

Run: `./gradlew test --tests '*AppleUserMigrationServiceTest' --tests '*AppleUserMigrationSettingsTest'`

Expected: compilation fails because the service, settings, and runner do not exist.

- [ ] **Step 3: Implement the minimal orchestration and runner**

PREPARE initializes target rows, requests one access token, continues after per-user failures, and marks successful rows prepared. COMPLETE does not discover new users, exchanges only retryable rows, and delegates the atomic identity update to the repository.

```java
public AppleUserMigrationSummary run(AppleUserMigrationPhase phase) {
  if (phase == AppleUserMigrationPhase.PREPARE) {
    repository.initializePending();
  }
  String accessToken = client.requestAccessToken();
  for (AppleUserMigrationCandidate candidate : repository.findCandidates(phase)) {
    migrateOne(phase, accessToken, candidate);
  }
  return repository.summarize(phase);
}
```

The runner prints only `phase`, `target`, `success`, `failure`, and `unresolved`, and returns non-zero when `summary.completed()` is false. Add a JavaExec task using the main runtime classpath.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `./gradlew test --tests '*AppleUserMigration*Test' --tests '*HttpAppleUserMigrationClientTest'`

Expected: PASS.

- [ ] **Step 5: Commit Task 3**

```bash
git add build.gradle src/main/java/com/landit/landitbe/feature/auth/migration src/test/java/com/landit/landitbe/feature/auth/migration
git commit -m "feat: Apple 사용자 이전 CLI 추가"
```

### Task 4: 운영 수동 workflow와 전체 검증

**Files:**
- Create: `.github/workflows/apple-user-migration.yml`
- Modify: `docs/tasks/LAN-426/plan.md`

**Interfaces:**
- Consumes: Gradle task `migrateAppleUsers`
- Consumes SSM: `/landit/prod/DB_URL`, `/landit/prod/DB_USERNAME`, `/landit/prod/DB_PASSWORD`, `/landit/prod/APPLE_MIGRATION_CLIENT_ID`, `/landit/prod/APPLE_MIGRATION_CLIENT_SECRET`, `/landit/prod/APPLE_MIGRATION_TARGET_TEAM_ID`
- Produces: manual inputs `phase` and `confirmation`, where confirmation must equal `RUN-PREPARE` or `RUN-COMPLETE`

- [ ] **Step 1: Add the manually dispatched production workflow**

Use `environment: prod`, `contents: read`, `id-token: write`, one non-cancelling concurrency group, reject non-main refs, validate the confirmation string, read encrypted SSM values without echoing them, omit target Team ID retrieval for COMPLETE, and invoke:

```bash
export APPLE_MIGRATION_PHASE="${{ inputs.phase }}"
./gradlew migrateAppleUsers
```

- [ ] **Step 2: Review workflow behavior and secret boundaries**

Check the complete YAML diff rather than matching one source line. Confirm there is no `pull_request`, `push`, develop option, shell tracing, secret echo, or application deployment step, and that the job fails on CLI non-zero exit.

- [ ] **Step 3: Run focused and full verification**

Run:

```bash
./gradlew test --tests '*AppleUserMigration*Test' --tests '*HttpAppleUserMigrationClientTest'
./gradlew check
git diff --check
```

Expected: all commands PASS.

- [ ] **Step 4: Record exact verification results in this plan**

Under a `## Verification Results` section, record the commands, exit results, and any operational checks that remain unverified without production credentials. Do not claim the workflow was executed against production.

- [ ] **Step 5: Commit Task 4**

```bash
git add .github/workflows/apple-user-migration.yml docs/tasks/LAN-426/plan.md
git commit -m "ci: Apple 사용자 이전 수동 workflow 추가"
```

- [ ] **Step 6: Run independent high-risk review**

Have a reviewer inspect the complete diff, design/plan coverage, test evidence, identity continuity, transaction boundaries, retry behavior, and secret handling. Fix blocking findings with a reproducing test, rerun affected checks, and commit the fix separately.
