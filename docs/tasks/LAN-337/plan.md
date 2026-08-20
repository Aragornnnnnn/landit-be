# LAN-337 관리자 인증·응답 계약 보완 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 로그인 응답의 권한 정보, 관리자 OpenAPI 응답 계약, 앱 버전 수정 메타데이터를 실제 API와 DB에 반영한다.

**Architecture:** 인증 기능은 기존 `AuthProfile` 공개 계약을 확장해 로그인 응답에 역할과 상태를 전달한다. 관리자 사용자 목록은 중첩 `Item`을 독립 record로 분리하고 DTO record component에 OpenAPI required/nullable을 명시한다. 앱 버전은 행에 수정 시각과 수정자 ID를 저장하고 `UserProfileService`를 통해 응답용 수정자 닉네임을 조회한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Flyway, H2, PostgreSQL, springdoc OpenAPI 3.1, MockMvc.

**Spec:** `docs/tasks/LAN-337/design.md`

## Global Constraints

- `GET /api/v1/auth/me`는 추가하지 않고 로그인 응답만 확장한다.
- `role`은 `USER | ADMIN`, `status`는 `ACTIVE | WITHDRAWN | BANNED` enum 값을 사용한다.
- 앱 버전 DB는 `updated_at TIMESTAMP(6) NOT NULL`, `updated_by_user_profile_id BIGINT NULL`과 사용자 프로필 외래 키를 사용한다.
- 앱 버전 API의 `updatedBy`는 현재 수정자 닉네임 `String | null`로 반환하고 DB에는 수정자 ID를 저장한다.
- 기존 감사 로그는 변경 이력으로 유지하며, 감사 로그가 없는 기존 앱 버전은 `updated_at=created_at`, 수정자 `NULL`로 백필한다.
- 사용자 목록 컬럼 결정과 편지 `title`·`preview` 길이 제한은 변경하지 않는다.
- 새 Java 소스 파일은 첫 줄에 한국어 역할 주석을 둔다.

### Task 1: 로그인 응답에 role·status 추가

**Files:**
- Modify: `src/main/java/com/landit/landitbe/feature/profile/dto/AuthProfile.java`
- Modify: `src/main/java/com/landit/landitbe/feature/auth/dto/AuthUserResponse.java`
- Test: `src/test/java/com/landit/landitbe/feature/auth/SocialAuthApiIntegrationTests.java`

**Interfaces:**
- `AuthProfile` exposes `UserRole role()` and `UserProfileStatus status()`.
- `AuthUserResponse` serializes those values as `role` and `status`.

- [x] **Step 1: Write the failing test**

Add assertions to the social-login integration flow for a new user (`USER`, `ACTIVE`) and an existing user whose profile role is changed to `ADMIN` before the next login.

```java
.andExpect(jsonPath("$.data.user.role").value("USER"))
.andExpect(jsonPath("$.data.user.status").value("ACTIVE"));
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.landit.landitbe.feature.auth.SocialAuthApiIntegrationTests`

Expected: FAIL because `data.user.role` and `data.user.status` are absent.

- [x] **Step 3: Write minimal implementation**

Extend `AuthProfile.from(UserProfile)` with `getRole()` and `getStatus()`, then pass both enum values through `AuthUserResponse.from(...)` without adding a new controller or endpoint.

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.landit.landitbe.feature.auth.SocialAuthApiIntegrationTests`

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/landit/landitbe/feature/profile/dto/AuthProfile.java src/main/java/com/landit/landitbe/feature/auth/dto/AuthUserResponse.java src/test/java/com/landit/landitbe/feature/auth/SocialAuthApiIntegrationTests.java
git commit -m "feat: 로그인 응답에 사용자 역할과 상태 추가"
```

### Task 2: OpenAPI schema 충돌과 required 계약 수정

**Files:**
- Create: `src/main/java/com/landit/landitbe/feature/admin/dto/AdminUserListItem.java`
- Modify: `src/main/java/com/landit/landitbe/feature/admin/dto/AdminUserListResponse.java`
- Modify: `src/main/java/com/landit/landitbe/feature/admin/dto/AdminUserDetailResponse.java`
- Modify: `src/main/java/com/landit/landitbe/feature/app/dto/AdminAppVersionResponse.java`
- Modify: `src/main/java/com/landit/landitbe/feature/auth/dto/AuthUserResponse.java`
- Modify: `src/main/java/com/landit/landitbe/feature/auth/dto/AuthTokenResponse.java`
- Test: `src/test/java/com/landit/landitbe/feature/admin/AdminUserApiIntegrationTests.java`
- Test: `src/test/java/com/landit/landitbe/feature/app/AdminAppVersionApiIntegrationTests.java`

**Interfaces:**
- `AdminUserListResponse.items` is `List<AdminUserListItem>`.
- OpenAPI references `#/components/schemas/AdminUserListItem`.
- Always-present response keys use `requiredMode = REQUIRED`; nullable values use `requiredMode = REQUIRED` and `nullable = true`.

- [x] **Step 1: Write the failing tests**

Extend the OpenAPI integration tests to assert the named list item schema, its `$ref`, and required fields for login/admin response schemas.

```java
jsonPath("$.components.schemas.AdminUserListItem").exists()
jsonPath("$.components.schemas.AdminUserListResponse.properties.items.items.$ref")
    .value("#/components/schemas/AdminUserListItem")
```

- [x] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests com.landit.landitbe.feature.admin.AdminUserApiIntegrationTests --tests com.landit.landitbe.feature.app.AdminAppVersionApiIntegrationTests`

Expected: FAIL because the generated document uses the shared `Item` component and has no required arrays for the target response properties.

- [x] **Step 3: Write minimal implementation**

Move the nested list record to the new top-level `AdminUserListItem` record, update the mapper, and annotate only the in-scope response components. Preserve nullable keys such as `email`, learning summary optional values, and app-version reason/release/modifier values.

- [x] **Step 4: Run tests to verify they pass**

Run the same two targeted test classes and confirm the OpenAPI assertions pass.

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/landit/landitbe/feature/admin/dto src/main/java/com/landit/landitbe/feature/app/dto/AdminAppVersionResponse.java src/main/java/com/landit/landitbe/feature/auth/dto/AuthUserResponse.java src/main/java/com/landit/landitbe/feature/auth/dto/AuthTokenResponse.java src/test/java/com/landit/landitbe/feature/admin/AdminUserApiIntegrationTests.java src/test/java/com/landit/landitbe/feature/app/AdminAppVersionApiIntegrationTests.java
git commit -m "fix: 관리자 OpenAPI 응답 스키마 계약 보완"
```

### Task 3: 앱 버전 수정 메타데이터 저장과 응답 추가

**Files:**
- Create: `src/main/resources/db/migration/V57__add_app_version_update_metadata.sql`
- Modify: `src/main/java/com/landit/landitbe/feature/app/domain/AppVersion.java`
- Modify: `src/main/java/com/landit/landitbe/feature/app/service/AppVersionService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/app/dto/AdminAppVersionResponse.java`
- Modify: `src/main/java/com/landit/landitbe/feature/profile/service/UserProfileService.java`
- Test: `src/test/java/com/landit/landitbe/feature/app/AdminAppVersionApiIntegrationTests.java`

**Interfaces:**
- `AppVersion.update(..., Long updatedByUserProfileId)` sets `updatedAt` immediately and stores the modifier ID.
- `UserProfileService.findNickname(Long userProfileId)` returns `Optional<String>`.
- `AdminAppVersionResponse.from(AppVersion appVersion, String updatedBy)` returns the API DTO.

- [x] **Step 1: Write the failing test**

Extend the admin app-version PATCH test to assert `updatedAt`, `updatedBy`, and persisted `updated_by_user_profile_id`; add a migration fixture assertion for an app version with no audit history.

```java
.andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
.andExpect(jsonPath("$.data.updatedBy").value("관리자"));
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.landit.landitbe.feature.app.AdminAppVersionApiIntegrationTests`

Expected: FAIL because the app-version table and response do not contain the new metadata.

- [x] **Step 3: Write minimal implementation**

Add V57 using a common migration path compatible with H2 and PostgreSQL. Add nullable modifier backfill from the latest `APP_VERSION_UPDATED` audit row per platform, fall back to `created_at` and `NULL`, then enforce `updated_at NOT NULL` and the foreign key. Add entity fields, service mapping, and profile nickname lookup. Update the existing audit record call without removing it.

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.landit.landitbe.feature.app.AdminAppVersionApiIntegrationTests`

Expected: PASS, including PATCH response, DB modifier ID, and migration fallback assertions.

- [x] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V57__add_app_version_update_metadata.sql src/main/java/com/landit/landitbe/feature/app src/main/java/com/landit/landitbe/feature/profile/service/UserProfileService.java src/test/java/com/landit/landitbe/feature/app/AdminAppVersionApiIntegrationTests.java
git commit -m "feat: 앱 버전 수정 메타데이터 저장"
```

### Task 4: 통합 검증과 작업 문서 갱신

**Files:**
- Modify: `docs/tasks/LAN-337/plan.md`
- Modify: `docs/tasks/LAN-337/design.md` only if implementation reveals a contract correction.

- [x] **Step 1: Run focused regression tests**

Run: `./gradlew test --tests com.landit.landitbe.feature.auth.SocialAuthApiIntegrationTests --tests com.landit.landitbe.feature.admin.AdminUserApiIntegrationTests --tests com.landit.landitbe.feature.app.AdminAppVersionApiIntegrationTests`

- [x] **Step 2: Run repository verification**

Run: `./gradlew check`

Expected: exit code 0 with Spotless, Checkstyle, and all tests passing.

- [x] **Step 3: Inspect final diff**

Run: `git diff --check` and `git status --short`.

Expected: no whitespace errors and only LAN-337 files changed.

- [x] **Step 4: Commit verification ledger update**

```bash
git add docs/tasks/LAN-337/plan.md
git commit -m "docs: LAN-337 검증 결과 기록"
```

## Verification record

- Focused regression: `SocialAuthApiIntegrationTests` 18, `AdminUserApiIntegrationTests` 7, `AdminAppVersionApiIntegrationTests` 9, plus related app-version/schema/service tests; all passed.
- Repository verification: `./gradlew check --no-daemon --rerun-tasks` passed with 520 tests and zero failures, errors, or skipped tests.
- Final diff checks: `git diff --check 4a1e63d5..HEAD` passed and `git status --short` was clean.
- Review: Task 1, Task 2, and Task 3 independent reviews approved; Task 3 P2 test gaps were fixed in `1f4fb482` and re-approved.
- Scope confirmations: no `GET /api/v1/auth/me`, no user-list column decision, and no mailbox title/preview length change.
