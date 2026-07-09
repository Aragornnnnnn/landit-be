# Scenario List API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 인증된 사용자가 카테고리별 시나리오 목록, 진행도, 잠금 상태, 시작 메시지 미리보기를 조회하는 `GET /api/v1/scenarios` API를 구현한다.

**Architecture:** 기존 인증 필터와 공통 응답 형식을 재사용한다. 목록 조회 전용 JPA projection query에서 카테고리, 시나리오 variant, 사용자 진행도를 한 번에 읽어 application service에서 응답 DTO로 조립한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring WebMVC, Spring Security, Spring Data JPA, JUnit 5, MockMvc, H2.

## Global Constraints

- 새 소스 파일 첫 줄에는 파일 역할을 설명하는 한국어 한 줄 주석을 둔다.
- `GET /api/v1/scenarios`는 Bearer access token 인증이 필요하다.
- 목록 조회만으로는 세션이나 메시지를 저장하지 않는다.
- 카테고리와 시나리오는 `display_order` 오름차순으로 정렬한다.
- 조회 locale은 `user_profile`의 `target_locale`, `base_locale`을 따른다.
- 잠긴 시나리오는 `openingPreview`를 `null`로 내려준다.
- AI first preview는 AI 메시지, 번역, 속마음, 속마음 유형을 내려준다.
- USER first preview는 사용자 시작 안내만 내려주고 AI 메시지와 속마음은 `null`로 내려준다.
- `ttsVoiceSetId`가 `null`이면 그대로 `null`을 내려준다.

---

### Task 1: API RED 테스트

**Files:**
- Create: `src/test/java/com/landit/landitbe/content/ScenarioListApiIntegrationTests.java`

**Interfaces:**
- Consumes: MockMvc, fake OIDC login, H2 schema.
- Produces: API 요구사항을 고정하는 실패 테스트.

- [ ] 인증 없이 `GET /api/v1/scenarios` 호출 시 `AUTH_REQUIRED` 401을 기대하는 테스트를 작성한다.
- [ ] 잘못된 Bearer token 호출 시 `INVALID_TOKEN` 401을 기대하는 테스트를 작성한다.
- [ ] displayOrder 정렬, 완료 여부, 별점, 잠금, AI/USER preview 분기를 검증하는 테스트를 작성한다.
- [ ] `./gradlew test --tests 'com.landit.landitbe.content.ScenarioListApiIntegrationTests'`를 실행해 RED를 확인한다.

### Task 2: 최소 API 구현

**Files:**
- Create: `src/main/java/com/landit/landitbe/content/api/ScenarioController.java`
- Create: `src/main/java/com/landit/landitbe/content/api/dto/ScenarioListResponse.java`
- Create: `src/main/java/com/landit/landitbe/content/application/ScenarioQueryService.java`
- Create: `src/main/java/com/landit/landitbe/content/infrastructure/ScenarioListQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/auth/security/AuthSecurityConfig.java`
- Modify: `src/main/java/com/landit/landitbe/auth/application/LanditTokenService.java`
- Modify: `src/main/java/com/landit/landitbe/common/exception/ErrorCode.java`

**Interfaces:**
- Consumes: `AuthUserPrincipal.userId()`, content tables, `user_scenario_progress`.
- Produces: `ApiResponse<ScenarioListResponse>`.

- [ ] `GET /api/v1/scenarios`를 인증 필요 경로로 등록한다.
- [ ] `INVALID_TOKEN` 오류 코드를 추가하고 잘못된 access token을 해당 코드로 매핑한다.
- [ ] 조회 repository에서 기본 locale의 카테고리/시나리오/progress row를 읽는다.
- [ ] service에서 displayOrder 정렬, 잠금 상태, 별점 변환, openingPreview 분기를 조립한다.
- [ ] 컨트롤러에서 공통 응답으로 반환한다.
- [ ] Scenario API 테스트를 다시 실행해 GREEN을 확인한다.

### Task 3: 검증과 기록

**Files:**
- Modify: `docs/tasks/LAN-62/checklist.md`
- Modify: `docs/tasks/LAN-62/context-notes.md`

**Interfaces:**
- Consumes: 구현 결과와 테스트 결과.
- Produces: 재개 가능한 작업 기록과 커밋.

- [ ] `git diff --check`를 실행한다.
- [ ] `./gradlew test`를 실행한다.
- [ ] `docs/tasks/LAN-62`의 checklist와 context notes를 완료 상태로 갱신한다.
- [ ] 논리 단위 커밋을 생성한다.
