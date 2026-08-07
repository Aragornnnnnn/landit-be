# LAN-249 date 미입력 시 당일 시나리오 조회 구현 계획

> **구현 작업자:** `superpowers:test-driven-development`를 적용해 아래 체크박스를 순서대로 수행한다.

**목표:** `GET /api/v1/scenarios/daily` 요청에서 `date`를 생략하면 `Asia/Seoul` 기준 당일 시나리오를 조회하고, 삭제된 `GET /api/v1/scenarios` 목록 조회 계약을 복구한다.

**구현 방향:** 컨트롤러는 `date`를 선택 파라미터로 전달하고, 이미 `Clock`과 서비스 시간대를 소유한 `DailyScenarioQueryService`가 요청 시각을 한 번 평가해 기본 조회일을 결정한다. 삭제 직전의 목록 응답, 조회 서비스, 전용 Repository와 Projection, 통합 테스트를 복원하고 현재 `/daily`, `/calendar` API와 같은 컨트롤러에서 공존시킨다.

**기술 스택:** Java 21, Spring Boot 4, Spring MVC, JUnit 5, MockMvc, springdoc-openapi.

## 공통 제약

- 날짜 기본값과 미래 날짜 판정은 `Asia/Seoul`을 기준으로 한다.
- `Clock`에서 한 번 얻은 `Instant`로 당일 날짜와 시나리오 진행 상태를 함께 평가한다.
- `date`가 전달된 요청의 기존 응답 계약은 변경하지 않는다.
- DB 스키마와 Repository는 변경하지 않는다.
- 목록 API는 삭제 직전의 경로, 인증, 응답 필드, 정렬, 잠금 및 진행 상태 계약을 유지한다.
- `/api/v1/scenarios/daily`와 `/api/v1/scenarios/calendar`의 현재 계약은 변경하지 않는다.

---

### Task 1: 일일 시나리오 조회의 date 기본값 적용

**변경 파일:**

- 수정: `src/test/java/com/landit/landitbe/feature/content/DailyScenarioApiIntegrationTests.java`
- 수정: `src/main/java/com/landit/landitbe/feature/content/ScenarioController.java`
- 수정: `src/main/java/com/landit/landitbe/feature/content/service/DailyScenarioQueryService.java`
- 수정: `src/main/java/com/landit/landitbe/feature/content/docs/ScenarioControllerDocs.java`

**인터페이스:**

- 입력: 인증된 `GET /api/v1/scenarios/daily` 요청과 선택 파라미터 `date: LocalDate`.
- 출력: `date` 생략 시 서비스 기준 당일 날짜가 담긴 기존 `ApiResponse<DailyScenarioResponse>`.
- 유지 계약: 유효한 명시 날짜는 해당 날짜를 조회하고, 미래 날짜는 `400 INVALID_REQUEST`, 잘못된 날짜 형식은 `400`으로 처리한다.

- [x] **Step 1: date 생략 시 당일 조회 실패 테스트로 기존 계약을 변경한다.**

  `dailyScenarioRejectsMissingDate`를 다음 의도의 테스트로 교체한다.

  ```java
  @Test
  void dailyScenarioUsesTodayWhenDateIsMissing() throws Exception {
    JsonNode loginResponseBody = login();
    String accessToken = loginResponseBody.get("data").get("accessToken").asText();
    seedDailyScenarios();

    mockMvc
        .perform(
            get("/api/v1/scenarios/daily")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.date").value("2026-07-28"))
        .andExpect(jsonPath("$.data.playable").value(true))
        .andExpect(jsonPath("$.data.scenario.scenarioId").value(100));
  }
  ```

- [x] **Step 2: 실패 테스트를 실행해 현재 date 필수 계약을 확인한다.**

  실행:

  ```bash
  ./gradlew test --tests com.landit.landitbe.feature.content.DailyScenarioApiIntegrationTests.dailyScenarioUsesTodayWhenDateIsMissing --no-daemon
  ```

  예상 결과: HTTP `400` 응답으로 테스트가 실패한다.

- [x] **Step 3: 컨트롤러에서 date를 선택 파라미터로 변경한다.**

  `ScenarioController.getDailyScenario`의 파라미터를 다음과 같이 변경한다.

  ```java
  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ```

  컨트롤러에서 `LocalDate.now()`를 호출하거나 별도 시간대를 계산하지 않고 nullable 값을 서비스로 전달한다.

- [x] **Step 4: 서비스에서 요청 시각과 기본 조회일을 한 번에 확정한다.**

  `DailyScenarioQueryService.getDailyScenario`에서 기존 `evaluatedAt`과 `today` 계산 직후 실제 조회일을 결정하고, 이후 검증과 조회에는 이 값만 사용한다.

  ```java
  Instant evaluatedAt = clock.instant();
  LocalDate today = evaluatedAt.atZone(SERVICE_ZONE_ID).toLocalDate();
  LocalDate queryDate = date != null ? date : today;
  validateNotFuture(queryDate, today);
  ```

  `findAccessGrantedOn`, `completedResponse`, `currentOrEmptyResponse` 호출에 기존 `date` 대신 `queryDate`를 전달한다. 공개 메서드 Javadoc의 `date` 계약에는 `null이면 Asia/Seoul 기준 당일을 조회한다`고 명시한다.

- [x] **Step 5: OpenAPI에서 date 선택값과 기본 동작을 문서화한다.**

  `ScenarioControllerDocs`에서 다음 내용을 반영한다.

  - `date` Javadoc을 `조회 날짜. 생략하면 서버 기준 오늘`로 변경한다.
  - `@Operation.description`에 `date` 생략 시 당일 조회 동작을 추가한다.
  - `@Parameter`의 설명에 `yyyy-MM-dd` 형식과 생략 시 당일 조회를 명시하고 `required = true`를 제거한다.
  - `400` 응답 설명에서 `날짜 누락`을 제거하고 날짜 형식 오류와 미래 날짜만 남긴다.
  - OpenAPI 통합 테스트의 `parameters[0].required` 기대값을 `false`로 변경한다.

- [x] **Step 6: 일일 시나리오 통합 테스트를 실행한다.**

  실행:

  ```bash
  ./gradlew test --tests com.landit.landitbe.feature.content.DailyScenarioApiIntegrationTests --no-daemon
  ```

  예상 결과: date 생략, 명시 날짜, 미래 날짜, 인증, OpenAPI 계약 테스트가 모두 통과한다.

- [x] **Step 7: 전체 품질 검사를 실행한다.**

  실행:

  ```bash
  ./gradlew check --rerun-tasks --no-daemon
  ```

  예상 결과: Spotless, Checkstyle, 전체 테스트가 모두 통과한다.

- [x] **Step 8: 하나의 논리적 변경으로 커밋한다.**

  ```bash
  git add src/main/java/com/landit/landitbe/feature/content/ScenarioController.java \
    src/main/java/com/landit/landitbe/feature/content/service/DailyScenarioQueryService.java \
    src/main/java/com/landit/landitbe/feature/content/docs/ScenarioControllerDocs.java \
    src/test/java/com/landit/landitbe/feature/content/DailyScenarioApiIntegrationTests.java \
    docs/tasks/LAN-249/plan.md
  git commit -m "fix: date 미입력 시 당일 시나리오 조회"
  ```

## 계획 검토 결과

- 요구사항 연결: date 생략 시 당일 조회, 명시 날짜 동작 유지, OpenAPI 계약 변경을 Task 1에서 모두 다룬다.
- 변경 범위: Controller, Service, API 문서, 통합 테스트만 수정하며 DB와 Repository는 제외한다.
- 검증 순서: 실패 테스트 확인 후 최소 구현, 대상 통합 테스트, 전체 `check` 순으로 검증한다.

### Task 2: 기존 시나리오 목록 조회 API 복구

**변경 파일:**

- 복원: `src/test/java/com/landit/landitbe/feature/content/ScenarioListApiIntegrationTests.java`
- 복원: `src/main/java/com/landit/landitbe/feature/content/dto/ScenarioListResponse.java`
- 복원: `src/main/java/com/landit/landitbe/feature/content/repository/ScenarioListQueryRepository.java`
- 복원: `src/main/java/com/landit/landitbe/feature/content/repository/projection/ScenarioListProjection.java`
- 복원: `src/main/java/com/landit/landitbe/feature/content/service/ScenarioQueryService.java`
- 수정: `src/main/java/com/landit/landitbe/feature/content/ScenarioController.java`
- 수정: `src/main/java/com/landit/landitbe/feature/content/docs/ScenarioControllerDocs.java`
- 수정: `src/main/java/com/landit/landitbe/config/security/AuthSecurityConfig.java`

**인터페이스:**

- 입력: Bearer access token이 포함된 `GET /api/v1/scenarios` 요청.
- 출력: `ApiResponse<ScenarioListResponse>`에 카테고리별 시나리오와 접근 상태, 신규·재도전 구분, 별점, 시작 메시지 미리보기를 반환한다.
- 유지 계약: 카테고리와 시나리오 표시 순서, 비활성 콘텐츠 잠금, 완료·오늘·잠금 상태, TTS 음성 응답을 삭제 직전과 동일하게 유지한다.

- [x] **Step 1: 삭제 직전 목록 API 통합 테스트를 복원한다.**

  `0bd0f8a7^`의 `ScenarioListApiIntegrationTests`를 먼저 복원한다. 인증, 정렬, 진행 상태, 잠금, 미리보기, OpenAPI 계약을 기존 기대값 그대로 사용한다.

- [x] **Step 2: 목록 통합 테스트를 실행해 API 부재로 실패하는지 확인한다.**

  실행:

  ```bash
  ./gradlew test --tests com.landit.landitbe.feature.content.ScenarioListApiIntegrationTests --no-daemon --console=plain
  ```

  예상 결과: `GET /api/v1/scenarios` 매핑 또는 목록 응답 계약이 없어 테스트가 실패한다.

- [x] **Step 3: 삭제 직전 목록 조회 구성요소를 복원한다.**

  `0bd0f8a7^`에서 `ScenarioListResponse`, `ScenarioListQueryRepository`, `ScenarioListProjection`, `ScenarioQueryService`를 복원한다. 현재 남아 있는 `ScenarioProgressionService`, `ScenarioAccessService`, `UserProfileService` 공개 계약을 그대로 사용한다.

- [x] **Step 4: 현재 시나리오 컨트롤러와 문서에 목록 엔드포인트를 병합한다.**

  `ScenarioController`에 `ScenarioQueryService` 의존성과 다음 메서드를 추가한다.

  ```java
  @Override
  @GetMapping("/api/v1/scenarios")
  public ApiResponse<ScenarioListResponse> listScenarios(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(scenarioQueryService.getScenarioList(principal.userId()));
  }
  ```

  `ScenarioControllerDocs`에는 삭제 직전의 `listScenarios` Javadoc과 OpenAPI 계약을 추가하되 기존 일일·캘린더 메서드는 유지한다.

- [x] **Step 5: 목록 조회 경로에 인증을 다시 적용한다.**

  `AuthSecurityConfig`에 `GET /api/v1/scenarios`를 `authenticated()` 경로로 추가하고, 현재 `/daily`, `/calendar` 인증 규칙을 유지한다.

- [x] **Step 6: 목록 통합 테스트를 실행한다.**

  실행:

  ```bash
  ./gradlew test --tests com.landit.landitbe.feature.content.ScenarioListApiIntegrationTests --no-daemon --console=plain
  ```

  예상 결과: 삭제 직전 목록 API 계약 테스트가 모두 통과한다.

- [x] **Step 7: 전체 품질 검사를 실행한다.**

  실행:

  ```bash
  ./gradlew check --rerun-tasks --no-daemon --console=plain
  ```

  예상 결과: Spotless, Checkstyle, 목록·일일·캘린더를 포함한 전체 테스트가 모두 통과한다.

- [x] **Step 8: 목록 API 복구를 하나의 논리적 변경으로 커밋한다.**

  ```bash
  git add src/main/java/com/landit/landitbe/config/security/AuthSecurityConfig.java \
    src/main/java/com/landit/landitbe/feature/content \
    src/test/java/com/landit/landitbe/feature/content/ScenarioListApiIntegrationTests.java \
    docs/tasks/LAN-249/plan.md
  git commit -m "fix: 시나리오 목록 조회 API 복구"
  ```

## 구현 결과

- `date`를 생략하면 `Asia/Seoul` 기준 당일 날짜로 일일 시나리오를 조회한다.
- 명시적 날짜 조회, 미래 날짜 거부, 인증, OpenAPI 계약을 기존 정책에 맞게 유지·갱신했다.
- 삭제 직전의 `GET /api/v1/scenarios` 목록 응답, 조회 서비스, 전용 조회, 인증, OpenAPI 계약과 통합 테스트를 복원했다.
- 일일 조회 통합 테스트에서 목록 API 제거를 검증하던 상충 계약을 제거하고 목록 API 통합 테스트로 책임을 분리했다.
- 목록 API 복원 전 통합 테스트 13개 중 12개가 엔드포인트 부재로 실패하는 RED를 확인했고, 복원 후 13개가 모두 통과했다.
- `./gradlew check --rerun-tasks --no-daemon`을 통과했다.
