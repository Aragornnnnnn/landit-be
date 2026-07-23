# LAN-200 백엔드 패키지 구조 및 책임 개선 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 백엔드 전체를 `feature/config/shared` 구조로 재배치하고, 공개 비즈니스 진입점을 Service로 통일하며 Repository 소유권·record 변환·Controller·예외·로그·Javadoc 규칙을 코드에 반영한다.

**Architecture:** 사용자 기능은 `feature.<name>` 아래에 모으고, Spring 설정은 `config`, 기능 독립적인 공통 코드는 `shared`에 둔다. Controller는 기능 루트에서 요청 흐름 Service만 호출하고, 요청 흐름 Service는 다른 기능의 공개 Service와 record를 통해 협력한다. 외부 AI 호출과 DB 트랜잭션은 분리하고 API 및 DB 계약은 유지한다.

**Tech Stack:** Java 21, Spring Boot 4.0.7, Spring Data JPA, Spring Security, springdoc-openapi 3.0.2, PostgreSQL, H2, Gradle 9.5.1, JUnit 5, Mockito, Spotless, Checkstyle.

## Global Constraints

- public API 경로, 요청·응답 JSON, DB 스키마와 AI 요청·응답 계약을 변경하지 않는다.
- 최상위 패키지는 `feature`, `config`, `shared`로 구분한다.
- 모든 공개 비즈니스 로직 클래스 이름은 `Service`로 끝낸다.
- Controller와 요청 흐름 Service는 Repository를 직접 참조하지 않는다.
- 다른 기능과는 공개 Service와 record로만 통신한다.
- 순수 Entity·Projection 변환은 대상 record의 `from()`, 요청 record의 `toEntity()`가 담당한다.
- 클래스 레벨 `@RequestMapping`을 제거하고 메서드 Mapping에 전체 경로를 작성한다.
- Swagger 애너테이션은 `docs` 인터페이스에 둔다.
- 공개 타입과 `public`, `protected` 메서드는 계약 중심 Javadoc을 작성한다.
- 외부 AI 호출 중에는 DB 트랜잭션과 row lock을 유지하지 않는다.
- 새 Java 파일의 첫 줄에는 역할을 설명하는 한국어 `//` 주석을 둔다.
- 각 작업은 관련 테스트와 `./gradlew check`를 통과한 뒤 커밋한다.

---

### Task 1: 저장소 규칙과 기준 문서 갱신

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/architecture/backend.md`
- Modify: `docs/development/java-style.md`
- Modify: `docs/tasks/LAN-200/design.md`
- Modify: `docs/tasks/LAN-200/plan.md`

**Interfaces:**
- Consumes: `docs/tasks/LAN-200/design.md`의 승인된 구조.
- Produces: 이후 모든 작업에서 적용할 패키지·Service·Javadoc 규칙.

- [x] **Step 1: 기존 4계층 규칙을 승인된 구조로 교체한다**

```text
com.landit.landitbe
├── feature
├── config
└── shared
```

`AGENTS.md`와 `docs/architecture/backend.md`에서 `api/application/domain/infrastructure` 및 UseCase·Service 구분을 제거한다. `feature.<name>` 내부 역할 패키지와 Repository 소유 Service 규칙을 기록한다.

- [x] **Step 2: Javadoc 계약을 문서화한다**

```java
/**
 * 사용자가 소유한 진행 중 세션을 조회한다.
 *
 * @param userId 세션 소유자 ID
 * @param sessionId 조회할 세션 ID
 * @return 조회된 학습 세션
 * @throws SessionException 세션이 없거나 접근할 수 없을 때
 */
public LearningSession findOwnedInProgress(long userId, long sessionId) {
  // ...
}
```

공개 타입과 `public`, `protected` 메서드에 요약, `@param`, `@return`, `@throws`를 작성하고 private 메서드는 의도가 불명확할 때만 Javadoc을 사용하도록 명시한다.

- [x] **Step 3: 문서 변경을 검증한다**

Run: `rg -n "api.*application.*domain.*infrastructure|UseCase와 Service 기준" AGENTS.md docs/architecture/backend.md`

Expected: 과거 구조를 현재 규칙으로 지시하는 결과가 없어야 한다.

Run: `git diff --check`

Expected: 출력 없이 종료 코드 0.

- [x] **Step 4: 문서 변경을 커밋한다**

```bash
git add AGENTS.md docs/architecture/backend.md docs/development/java-style.md docs/tasks/LAN-200
git commit -m "docs: LAN-200 백엔드 구조 규칙 갱신"
```

### Task 2: 최상위 feature/config/shared 패키지 재배치

**Files:**
- Move: `src/main/java/com/landit/landitbe/common/**` → `src/main/java/com/landit/landitbe/shared/**`
- Move: `src/main/java/com/landit/landitbe/app/**` → `src/main/java/com/landit/landitbe/feature/app/**`
- Move: `src/main/java/com/landit/landitbe/auth/**` → `src/main/java/com/landit/landitbe/feature/auth/**`
- Move: `src/main/java/com/landit/landitbe/character/**` → `src/main/java/com/landit/landitbe/feature/character/**`
- Move: `src/main/java/com/landit/landitbe/content/**` → `src/main/java/com/landit/landitbe/feature/content/**`
- Move: `src/main/java/com/landit/landitbe/learning/**` → `src/main/java/com/landit/landitbe/feature/learning/**`
- Move: `src/main/java/com/landit/landitbe/notification/**` → `src/main/java/com/landit/landitbe/feature/notification/**`
- Move: `src/main/java/com/landit/landitbe/nps/**` → `src/main/java/com/landit/landitbe/feature/nps/**`
- Move: `src/main/java/com/landit/landitbe/quest/**` → `src/main/java/com/landit/landitbe/feature/quest/**`
- Move: `src/main/java/com/landit/landitbe/session/**` → `src/main/java/com/landit/landitbe/feature/session/**`
- Move: corresponding `src/test/java/com/landit/landitbe/{app,auth,common,content,learning,nps,session}/**` paths.
- Modify: all affected Java package declarations and imports.

**Interfaces:**
- Consumes: 기존 FQCN 전체.
- Produces: `com.landit.landitbe.feature.*`, `com.landit.landitbe.config.*`, `com.landit.landitbe.shared.*`.

- [x] **Step 1: 패키지 이동 전 기준 테스트를 확인한다**

Run: `./gradlew check`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 2: 기능 및 공통 패키지를 이동하고 FQCN을 일괄 변경한다**

```text
com.landit.landitbe.<feature>  → com.landit.landitbe.feature.<feature>
com.landit.landitbe.common     → com.landit.landitbe.shared
```

메인 애플리케이션 클래스와 Flyway 실행기는 `com.landit.landitbe` 루트에 유지한다.

- [x] **Step 3: 이동 누락을 검사한다**

Run: `find src/main/java/com/landit/landitbe -mindepth 1 -maxdepth 1 -type d | sort`

Expected:

```text
src/main/java/com/landit/landitbe/feature
src/main/java/com/landit/landitbe/shared
```

Run: `rg -n "com\\.landit\\.landitbe\\.(app|auth|character|common|content|learning|notification|nps|quest|session)" src/main/java src/test/java`

Expected: 출력 없음.

- [x] **Step 4: 컴파일과 테스트를 검증한다**

Run: `./gradlew check`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: 패키지 이동을 커밋한다**

```bash
git add src/main/java src/test/java
git commit -m "refactor: 기능과 공통 패키지 상위 경계 분리"
```

### Task 3: 역할 패키지와 설정·외부 Client 재배치

**Files:**
- Move: every `feature/*/api/dto/*.java` → `feature/*/dto/*.java`
- Move: every `feature/*/api/*Controller.java` → `feature/*/*Controller.java`
- Move: every `feature/*/api/docs/*.java` → `feature/*/docs/*.java`
- Move: every `feature/*/application/*.java` → `feature/*/service/*.java`
- Move: every `feature/*/infrastructure/*Repository.java` → `feature/*/repository/*.java`
- Move: content and session `*Row.java` → each `repository/projection/*Projection.java`
- Move: `feature/session/infrastructure/ai/*ConversationClient.java` → `feature/session/client/ai/`
- Move: `feature/session/application/port/Ai*.java` → `feature/session/client/ai/`
- Move: `feature/auth/application/OidcProperties.java` → `config/auth/OidcProperties.java`
- Move: `feature/auth/application/TokenProperties.java` → `config/auth/TokenProperties.java`
- Move: `feature/session/infrastructure/ai/AiClientProperties.java` → `config/ai/AiClientProperties.java`
- Move: `shared/web/CorsProperties.java` → `config/web/CorsProperties.java`
- Move: `shared/web/OpenApiDocsEncodingAdvice.java` → `config/web/OpenApiDocsEncodingAdvice.java`
- Move: `feature/auth/security/AuthSecurityConfig.java` → `config/security/AuthSecurityConfig.java`
- Modify: all affected package declarations, imports, JPQL constructor expressions and tests.

**Interfaces:**
- Produces: 기능 루트 Controller, `dto`, `docs`, `domain`, `repository`, `service`, `client`, `exception`.
- Preserves: Spring component scanning, JPA repository scanning and Configuration Properties binding.

- [x] **Step 1: Projection 이름과 JPQL 생성자 경로를 함께 변경한다**

```text
ScenarioListRow → ScenarioListProjection
ScenarioQuestionRow → ScenarioQuestionProjection
ScenarioSessionStartRow → ScenarioSessionStartProjection
ScenarioSessionLockRow → ScenarioSessionLockProjection
ScenarioSessionMessageContextRow → ScenarioSessionMessageContextProjection
```

- [x] **Step 2: 역할 패키지 이동 후 구 패키지를 검사한다**

Run: `find src/main/java/com/landit/landitbe/feature -type d \( -name api -o -name application -o -name infrastructure \)`

Expected: 출력 없음.

- [x] **Step 3: 설정 위치를 검사한다**

Run: `rg -l "@ConfigurationProperties|@Configuration" src/main/java/com/landit/landitbe/feature src/main/java/com/landit/landitbe/shared`

Expected: 기능 고유 설정이 아닌 최상위 설정 클래스 출력 없음.

- [x] **Step 4: 컴파일과 Repository 통합 테스트를 실행한다**

Run: `./gradlew test --tests '*QueryRepositoryIntegrationTests'`

Expected: 관련 Repository 통합 테스트 통과.

Run: `./gradlew check`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: 역할 패키지 이동을 커밋한다**

```bash
git add src/main/java src/test/java
git commit -m "refactor: 패키지를 실제 코드 역할 기준으로 재배치"
```

### Task 4: Profile·Learning 소유권과 기능 간 Service 경계 확립

**Files:**
- Move: `feature/auth/domain/UserProfile.java` → `feature/profile/domain/UserProfile.java`
- Move: `feature/auth/domain/UserProfileStatus.java` → `feature/profile/domain/UserProfileStatus.java`
- Move: `feature/auth/domain/LearningLevel.java` → `feature/profile/domain/LearningLevel.java`
- Move: `feature/auth/domain/PushPermissionStatus.java` → `feature/profile/domain/PushPermissionStatus.java`
- Move: `feature/auth/repository/UserProfileRepository.java` → `feature/profile/repository/UserProfileRepository.java`
- Move: `feature/auth/service/UserProfileService.java` → `feature/profile/service/UserProfileService.java`
- Move: `feature/auth/service/UserLocale.java` → `feature/profile/dto/UserLocale.java`
- Move: `feature/content/domain/UserWritingExpressionCompletion.java` → `feature/learning/domain/UserWritingExpressionCompletion.java`
- Move: `feature/content/repository/UserWritingExpressionCompletionRepository.java` → `feature/learning/repository/UserWritingExpressionCompletionRepository.java`
- Create: `feature/content/service/AiTutorService.java`
- Create: `feature/learning/service/LearningProgressService.java`
- Modify: `feature/auth/service/AuthService.java`
- Modify: `feature/auth/security/AuthTokenFilter.java`
- Modify: `feature/content/service/ExpressionQueryService.java`
- Modify: `feature/content/service/ExpressionLearningCompletionService.java`
- Test: `src/test/java/com/landit/landitbe/feature/profile/service/UserProfileServiceTest.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/service/ExpressionLearningCompletionServiceTest.java`

**Interfaces:**
- Produces: `UserProfileService.getUserLocale(Long)`, `UserProfileService.requireActive(Long)`, `AiTutorService.requireActive(Long)`, `LearningProgressService.completeExpression(Long, Long)`.
- Consumers: Auth, Security, Content and Session Services.

- [x] **Step 1: Service 경계 단위 테스트를 작성한다**

```java
@Test
void requireActiveReturnsActiveProfile() {
  given(userProfileRepository.findByIdAndStatus(userId, UserProfileStatus.ACTIVE))
      .willReturn(Optional.of(userProfile));

  assertThat(userProfileService.requireActive(userId)).isSameAs(userProfile);
}
```

- [x] **Step 2: Repository 직접 의존을 Service 호출로 교체한다**

`AuthService`는 `UserProfileRepository`와 `AiTutorRepository` 대신 `UserProfileService`와 `AiTutorService`를 사용한다. `AuthTokenFilter`도 `UserProfileService`를 통해 활성 사용자를 확인한다.

- [x] **Step 3: 기능 간 Repository 의존이 사라졌는지 검사한다**

Run: `rg -n "feature\\.(auth|content|learning|profile)\\.repository" src/main/java/com/landit/landitbe/feature/{auth,content,learning,profile}/service`

Expected: 같은 기능의 소유 Service 외에는 다른 기능 Repository import가 없어야 한다.

- [x] **Step 4: 관련 테스트와 전체 검사를 실행한다**

Run: `./gradlew test --tests '*UserProfileServiceTest' --tests '*Expression*Test'`

Expected: 지정 테스트 통과.

Run: `./gradlew check`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: 기능 소유권 변경을 커밋한다**

```bash
git add src/main/java src/test/java
git commit -m "refactor: 프로필과 학습 진행 소유권 분리"
```

### Task 5: Controller 전체 경로·Swagger·record 변환 통일

**Files:**
- Modify: every `feature/*/*Controller.java`
- Modify: every `feature/*/docs/*ControllerDocs.java`
- Modify: every `feature/*/dto/*.java`
- Modify: `feature/nps/service/NpsService.java`
- Modify: content and session Service response construction.
- Test: API integration tests under `src/test/java/com/landit/landitbe/feature/{app,auth,content,nps,session}/`.

**Interfaces:**
- Preserves: all existing HTTP paths and JSON contracts.
- Produces: Controller의 전체 Mapping, Swagger docs interface, response record `from()`, request record `toEntity()`.

- [x] **Step 1: Controller 경로를 전체 경로로 변경한다**

```java
@PostMapping("/api/v1/scenarios/{scenarioId}/sessions")
public ResponseEntity<ApiResponse<SessionStartResponse>> startScenarioSession(...) {
  return ApiResponse.success(
      HttpStatus.CREATED,
      scenarioSessionStartService.startScenarioSession(principal.userId(), scenarioId));
}
```

클래스 레벨 `@RequestMapping`을 모두 제거한다.

- [x] **Step 2: LAN-102 Swagger 인터페이스 패턴을 모든 Controller에 적용한다**

Controller는 `*ControllerDocs`를 구현하고 Mapping 애너테이션만 소유한다. `@Operation`, `@ApiResponses`, `@Parameter`는 `docs` 인터페이스에 둔다.

- [x] **Step 3: 변환 코드를 record로 이동한다**

```java
public static SessionInnerThoughtResponse from(SessionHistoryMessage message) {
  return new SessionInnerThoughtResponse(
      message.getId(), message.getInnerThought(), message.getInnerThoughtType());
}
```

```java
public NpsResponse toEntity(Long userProfileId) {
  return new NpsResponse(userProfileId, score, opinionText);
}
```

- [x] **Step 4: Controller와 Service의 변환 잔여물을 검사한다**

Run: `rg -n "toResponse|new .*Response\\(" src/main/java/com/landit/landitbe/feature/*/service`

Expected: 순수 필드 매핑을 담당하는 Service 코드가 없어야 한다.

Run: `rg -n "@RequestMapping" src/main/java/com/landit/landitbe/feature`

Expected: 출력 없음.

- [x] **Step 5: API와 OpenAPI 테스트를 실행한다**

Run: `./gradlew test --tests '*ApiIntegrationTests' --tests '*OpenApi*Tests'`

Expected: 기존 API 계약과 OpenAPI 테스트 통과.

- [x] **Step 6: Controller와 변환 변경을 커밋한다**

```bash
git add src/main/java src/test/java
git commit -m "refactor: Controller 경로와 DTO 변환 책임 통일"
```

### Task 6: Session Repository 소유 Service와 Service 명명 통일

**Files:**
- Rename: `feature/session/service/ScenarioSessionStartUseCase.java` → `ScenarioSessionStartService.java`
- Rename: `feature/session/service/SessionMessageSubmitUseCase.java` → `SessionMessageSubmitService.java`
- Rename: `feature/session/service/SessionFeedbackUseCase.java` → `SessionFeedbackService.java`
- Rename: `feature/content/service/CompleteExpressionLearningUseCase.java` → `ExpressionLearningCompletionService.java`
- Create: `feature/session/service/LearningSessionService.java`
- Create: `feature/session/service/ScenarioSessionService.java`
- Create: `feature/session/service/SessionHistoryService.java`
- Create: `feature/session/service/SessionMessageService.java`
- Create: `feature/session/service/SessionFeedbackDataService.java`
- Rename: `SessionFeedbackContextLoader.java` → `SessionFeedbackContextService.java`
- Rename: `SessionFeedbackRecorder.java` → `SessionFeedbackCompletionService.java`
- Merge and delete: `LearningSessionFinder.java`, `SessionEndUseCase.java`, `SubmittedMessageRecorder.java`, `GeneratedMessageRecorder.java`, `SessionInnerThoughtRecorder.java`, `SessionMessageFeedbackRecorder.java`.
- Modify: all session Controllers, Services and tests.
- Create tests for each Repository owning Service and request flow Service.

**Interfaces:**
- Produces: the Repository ownership table fixed in `design.md`.
- Preserves: message processing status, session ownership, optimistic/row locking, compensation delete and feedback idempotency.

- [x] **Step 1: Repository 소유 Service 단위 테스트를 작성한다**

```java
@Test
void findOwnedInProgressRejectsCompletedSession() {
  given(learningSessionRepository.findByIdAndUserProfileId(sessionId, userId))
      .willReturn(Optional.of(completedSession));

  assertThatThrownBy(() -> learningSessionService.findOwnedInProgress(userId, sessionId))
      .isInstanceOf(SessionException.class);
}
```

- [x] **Step 2: 요청 흐름 Service의 Repository 의존을 소유 Service로 교체한다**

```text
ScenarioSessionStartService
├── UserProfileService
├── ScenarioContentService
├── LearningProgressService
├── LearningSessionService
├── ScenarioSessionService
├── SessionHistoryService
└── SessionMessageService
```

- [x] **Step 3: AI 호출과 저장 트랜잭션을 분리한다**

```text
SessionFeedbackService
├── SessionFeedbackContextService
├── AiConversationClient
└── SessionFeedbackCompletionService
```

`SessionFeedbackService`에는 `@Transactional`을 두지 않는다. `SessionFeedbackContextService`는 읽기 전용 트랜잭션, `SessionFeedbackCompletionService`는 결과 저장 트랜잭션을 시작한다.

- [x] **Step 4: 금지된 이름과 직접 Repository 의존을 검사한다**

Run: `rg -n "class .*UseCase|class .*Finder|class .*Recorder|class .*Loader" src/main/java/com/landit/landitbe/feature`

Expected: 공개 비즈니스 로직 클래스에서 결과 없음.

Run: `rg -n "private final .*Repository" src/main/java/com/landit/landitbe/feature/session/service`

Expected: 설계 문서의 Repository 소유 Service에서만 결과가 나와야 한다.

- [x] **Step 5: Session 테스트와 전체 검사를 실행한다**

Run: `./gradlew test --tests '*Session*Test' --tests '*Session*IntegrationTests'`

Expected: 세션 단위·Repository·API 통합 테스트 통과.

Run: `./gradlew check`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 6: Session Service 변경을 커밋한다**

```bash
git add src/main/java src/test/java
git commit -m "refactor: 세션 Repository 소유 Service 통합"
```

### Task 7: 기능 예외·상태 변경 로그·Javadoc 계약 완성

**Files:**
- Create: `feature/session/exception/SessionException.java`
- Create: `feature/session/exception/SessionErrorCode.java`
- Create: `feature/profile/exception/UserProfileException.java`
- Create: `feature/profile/exception/UserProfileErrorCode.java`
- Modify: `shared/exception/GlobalExceptionHandler.java`
- Modify: state-changing Services under `feature/auth/service`, `feature/content/service`, `feature/nps/service`, `feature/session/service`.
- Modify: public types and methods under `src/main/java/com/landit/landitbe/feature`, `config`, `shared`.
- Modify: `config/checkstyle/google_checks.xml` only if a rule can be enforced without false positives.
- Test: feature Service tests and `shared/exception/GlobalExceptionHandlerTests.java`.

**Interfaces:**
- Produces: 기능별 예상 오류와 민감정보를 제외한 상태 변경 로그.
- Preserves: HTTP status and error response schema.

- [x] **Step 1: 기능 예외의 HTTP 변환 테스트를 작성한다**

```java
@Test
void handlesSessionException() {
  ResponseEntity<ErrorResponse> response =
      handler.handleFeatureException(new SessionException(SessionErrorCode.SESSION_NOT_FOUND));

  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
}
```

- [x] **Step 2: 상태 변경 Service에 안전한 로그를 추가한다**

```java
log.info("scenario session started: userId={}, scenarioId={}, sessionId={}",
    userId, scenarioId, sessionId);
```

Token, 이메일, 사용자 메시지 원문과 전체 Request Body는 기록하지 않는다.

- [x] **Step 3: 공개 API에 계약 중심 Javadoc을 작성한다**

파라미터가 있는 공개 메서드는 `@param`, 반환값이 있으면 `@return`, 호출자가 처리할 기능 예외가 있으면 `@throws`를 작성한다. record는 구성 요소별 `@param`을 작성한다.

- [x] **Step 4: Javadoc과 로그를 검사한다**

Run: `./gradlew checkstyleMain checkstyleTest`

Expected: Checkstyle 통과.

Run: `rg -n "log\\.(info|warn|error).*?(token|email|content|message)" src/main/java`

Expected: 민감 값 원문을 기록하는 로그 없음.

- [x] **Step 5: 전체 검사를 실행하고 커밋한다**

Run: `./gradlew check`

Expected: `BUILD SUCCESSFUL`.

```bash
git add src/main/java src/test/java config/checkstyle
git commit -m "refactor: 기능 예외와 Javadoc 및 로그 규칙 적용"
```

### Task 8: 최종 구조 및 회귀 검증

**Files:**
- Verify: all files changed by Tasks 1–7.
- Modify if needed: `docs/tasks/LAN-200/plan.md` verification notes only.

**Interfaces:**
- Produces: LAN-200 완료 증거.

- [ ] **Step 1: 전체 빌드를 실행한다**

Run: `./gradlew clean check`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: 금지된 패키지와 이름을 검사한다**

Run: `find src/main/java/com/landit/landitbe/feature -type d \( -name api -o -name application -o -name infrastructure \)`

Expected: 출력 없음.

Run: `rg -n "class .*UseCase|class .*Finder|@RequestMapping" src/main/java/com/landit/landitbe`

Expected: 출력 없음.

Run: `rg -n "com\\.landit\\.landitbe\\.(app|auth|character|common|content|learning|notification|nps|quest|session)" src/main/java src/test/java`

Expected: 출력 없음.

- [ ] **Step 3: API 및 DB 계약 변경을 확인한다**

Run: `git diff origin/develop -- src/main/resources/db/migration`

Expected: 출력 없음.

Run: `./gradlew test --tests '*ApiIntegrationTests' --tests '*QueryRepositoryIntegrationTests'`

Expected: API와 Repository 통합 테스트 통과.

- [ ] **Step 4: 변경 범위와 작업 트리를 확인한다**

Run: `git diff --check origin/develop`

Expected: 출력 없이 종료 코드 0.

Run: `git status --short`

Expected: 계획 검증 기록 외에는 커밋되지 않은 변경 없음.
