# DBML Entities And Flyway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** DBML 기준의 테이블, 제약, JPA Entity를 추가하고 기존 인증 구현을 `user_profile`과 `oauth_identity` 구조로 전환한다.

**Architecture:** 기존 LAN-55 인증 흐름은 유지하되 사용자 저장 모델만 DBML 구조로 바꾼다. 인증에서 실제 탐색이 필요한 `UserProfile`, `OauthIdentity`, `RefreshToken`만 객체 관계를 두고, 나머지 Entity는 FK를 `Long` 컬럼으로 매핑한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, Flyway, PostgreSQL, H2 test profile, Hibernate JSON mapping.

## Global Constraints

- 새 소스 파일 첫 줄에는 파일 역할을 설명하는 한국어 한 줄 주석을 둔다.
- DBML이 표현하지 못하는 PostgreSQL `check constraint`와 `partial unique index`는 Flyway에서 관리한다.
- `jsonb` 컬럼은 Jackson `JsonNode`와 `@JdbcTypeCode(SqlTypes.JSON)`로 매핑한다.
- 구현은 테스트를 먼저 추가하고 실패를 확인한 뒤 진행한다.
- 커밋은 의미 있는 작업 단위로 나눈다.

---

### Task 1: DBML 스키마 회귀 테스트

**Files:**
- Create: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`

**Interfaces:**
- Consumes: Flyway가 생성한 실제 테스트 DB 스키마.
- Produces: DBML 핵심 테이블, partial unique index, 순환 FK 제거 여부를 확인하는 회귀 테스트.

- [ ] DBML 핵심 테이블 존재 테스트를 추가한다.
- [ ] `oauth_identity` ACTIVE partial unique index 존재 테스트를 추가한다.
- [ ] `session_history_message_feedback.user_learning_expression_id` 컬럼이 없다는 테스트를 추가한다.
- [ ] `./gradlew test --tests 'com.landit.landitbe.DatabaseSchemaIntegrationTests'`를 실행해 실패를 확인한다.

### Task 2: Flyway 스키마 전환

**Files:**
- Create: `src/main/resources/db/migration/V4__apply_dbml_schema.sql`

**Interfaces:**
- Consumes: 기존 `users`, `refresh_tokens` 테이블.
- Produces: DBML 기준 `user_profile`, `oauth_identity`, `refresh_token`과 나머지 도메인 테이블.

- [ ] 기존 인증 데이터를 `user_profile`, `oauth_identity`, `refresh_token`으로 이관한다.
- [ ] DBML 테이블과 FK를 생성한다.
- [ ] 합의한 check constraint와 partial unique index를 추가한다.
- [ ] 기존 `users`, `refresh_tokens` 테이블을 제거한다.
- [ ] 스키마 테스트를 다시 실행해 통과를 확인한다.

### Task 3: 인증 Entity 리팩터링

**Files:**
- Delete: `src/main/java/com/landit/landitbe/auth/domain/User.java`
- Delete: `src/main/java/com/landit/landitbe/auth/infrastructure/UserRepository.java`
- Create: `src/main/java/com/landit/landitbe/auth/domain/UserProfile.java`
- Create: `src/main/java/com/landit/landitbe/auth/domain/OauthIdentity.java`
- Create: `src/main/java/com/landit/landitbe/auth/domain/OauthIdentityStatus.java`
- Create: `src/main/java/com/landit/landitbe/auth/domain/UserProfileStatus.java`
- Create: `src/main/java/com/landit/landitbe/auth/domain/LearningLevel.java`
- Create: `src/main/java/com/landit/landitbe/auth/domain/PushPermissionStatus.java`
- Create: `src/main/java/com/landit/landitbe/auth/infrastructure/UserProfileRepository.java`
- Create: `src/main/java/com/landit/landitbe/auth/infrastructure/OauthIdentityRepository.java`
- Modify: `src/main/java/com/landit/landitbe/auth/domain/RefreshToken.java`
- Modify: `src/main/java/com/landit/landitbe/auth/application/AuthService.java`
- Modify: `src/main/java/com/landit/landitbe/auth/application/LanditTokenService.java`
- Modify: `src/main/java/com/landit/landitbe/auth/security/AuthTokenFilter.java`
- Modify: `src/test/java/com/landit/landitbe/auth/SocialAuthApiIntegrationTests.java`

**Interfaces:**
- Consumes: `OidcUserInfo`.
- Produces: 기존 API 응답을 유지하는 DBML 기반 인증 저장 흐름.

- [ ] 기존 auth 통합 테스트를 실행해 구조 변경 전 기준을 확인한다.
- [ ] 인증 Entity와 Repository를 DBML 구조로 바꾼다.
- [ ] 로그인, refresh, logout, withdraw 흐름을 새 Entity로 연결한다.
- [ ] auth 통합 테스트를 다시 실행해 통과를 확인한다.

### Task 4: 나머지 DBML Entity 추가

**Files:**
- Create: content, session, learning, quest, character, notification, app domain Entity와 enum.
- Create: `src/main/java/com/landit/landitbe/common/domain/BaseCreatedAtEntity.java`

**Interfaces:**
- Consumes: DBML 테이블과 enum 정의.
- Produces: Hibernate validate가 통과하는 JPA Entity 매핑.

- [ ] created_at만 쓰는 테이블용 공통 기반 Entity를 추가한다.
- [ ] 콘텐츠 Entity와 enum을 추가한다.
- [ ] 세션과 히스토리 Entity와 enum을 추가한다.
- [ ] 복습, 퀘스트, 캐릭터, 앱 버전, 푸시 토큰 Entity와 enum을 추가한다.
- [ ] 전체 `./gradlew test`를 실행해 Hibernate validate까지 확인한다.

### Task 5: 검증과 커밋

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Interfaces:**
- Consumes: 구현 결과.
- Produces: 검증 기록과 의미 단위 커밋.

- [ ] `git diff --check`를 실행한다.
- [ ] `./gradlew test`를 실행한다.
- [ ] 스키마 전환, 인증 리팩터링, 나머지 Entity 추가, 문서 메모를 의미 단위로 커밋한다.
