<!-- LAN-374 어드민 피드백 상세 조회 API 구현 순서와 검증 결과를 기록한다. -->
# LAN-374 Admin Feedback Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `feedbackId`로 피드백과 nullable 최신 답장을 함께 조회하는 어드민 상세 API를 추가한다.

**Architecture:** `AdminMailboxController`가 상세 요청을 `AdminMailboxService`에 위임한다. Service는 피드백·사용자 정보를 조회하고 canonical `resolvedByFeedbackId`와 요청 `feedbackId`를 답장 후보로 계산한 뒤, 사용자 ID와 두 후보에 연결된 수신 정보에서 가장 최근 `REPLY` 편지를 조회해 전용 상세 DTO로 조합한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, MockMvc, Gradle.

**Spec:** `docs/tasks/LAN-374/design.md`.

## Global Constraints

- 엔드포인트는 정확히 `GET /api/v1/admin/mailbox/feedbacks/{feedbackId}`다.
- 상세 조회 키는 `feedbackId`이며 검색 응답은 변경하지 않는다.
- `reply`는 nullable이고 `letterId`, `title`, `bodyText`, `sentAt`만 포함한다.
- 답장 정렬은 `publishedAt DESC, id DESC`이며 가장 최근 한 건만 반환한다.
- 비대표 답장 조회는 canonical `resolvedByFeedbackId`와 요청 `feedbackId` 양쪽 후보를 사용하고, `publishedAt DESC, id DESC` 최신 한 건을 반환한다.
- 답장 조회 쿼리는 피드백의 `userProfileId`를 함께 조건으로 사용한다.
- 존재하지 않는 피드백은 `RESOURCE_NOT_FOUND`를 반환한다.
- DB 스키마와 답장 작성 동작은 변경하지 않는다.

---

### Task 1: 어드민 피드백 상세 조회 API

**Files:**
- Create: `src/main/java/com/landit/landitbe/feature/mailbox/dto/AdminMailboxFeedbackDetailResponse.java`
- Modify: `src/main/java/com/landit/landitbe/feature/mailbox/AdminMailboxController.java`
- Modify: `src/main/java/com/landit/landitbe/feature/mailbox/docs/AdminMailboxControllerDocs.java`
- Modify: `src/main/java/com/landit/landitbe/feature/mailbox/service/AdminMailboxService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/mailbox/repository/AdminMailboxFeedbackRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/mailbox/repository/AdminMailboxLetterRepository.java` 또는 기존 소유권에 맞는 편지함 Repository 한 곳.
- Test: `src/test/java/com/landit/landitbe/feature/mailbox/AdminMailboxApiIntegrationTests.java`
- Modify: `docs/tasks/LAN-374/plan.md`

**Interfaces:**
- Consumes: `MailboxFeedback.resolvedByFeedbackId`, `MailboxLetterRecipient.representativeFeedbackId`, `MailboxLetter.id`.
- Produces: `AdminMailboxService.getFeedback(Long feedbackId)`와 `AdminMailboxFeedbackDetailResponse`.

- [x] **Step 1: 상세 API 통합 테스트를 먼저 작성한다.**

  실제 MockMvc와 H2 데이터를 사용해 다음 계약을 각각 검증한다.

  ```java
  get("/api/v1/admin/mailbox/feedbacks/{feedbackId}", feedbackId)
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
  ```

  - 미답변 피드백은 기존 피드백·사용자 필드와 `reply: null`을 반환한다.
  - 답장을 두 번 보낸 대표 피드백은 두 번째 답장의 `letterId`, `title`, `bodyText`, `sentAt`을 반환한다.
  - 일괄 답장의 비대표 피드백은 canonical `resolvedByFeedbackId`와 요청 `feedbackId` 양쪽에 연결된 답장 중 최신 답장을 반환한다.
  - 존재하지 않는 ID는 HTTP 404와 `RESOURCE_NOT_FOUND`를 반환한다.

- [x] **Step 2: 추가한 통합 테스트가 기능 부재로 실패하는지 확인한다.**

  Run: `./gradlew test --tests com.landit.landitbe.feature.mailbox.AdminMailboxApiIntegrationTests`

  Expected: 새 상세 API 테스트가 HTTP 404 또는 응답 필드 부재로 실패한다.

  Result: `./gradlew test --tests com.landit.landitbe.feature.mailbox.AdminMailboxApiIntegrationTests` 실행 시 `27 tests completed, 3 failed`였고, 새 상세 조회 테스트 3건이 `Status expected:<200> but was:<404>`로 실패했다. 존재하지 않는 피드백 404 테스트는 통과했다.

- [x] **Step 3: 최소 구현으로 상세 계약을 만족시킨다.**

  `AdminMailboxFeedbackDetailResponse`는 기존 목록 응답과 동일한 피드백·사용자 필드에 다음 nullable 중첩 record를 추가한다.

  ```java
  public record Reply(Long letterId, String title, String bodyText, LocalDateTime sentAt) {}
  ```

  Controller와 OpenAPI 문서에 상세 엔드포인트를 추가한다. Service는 피드백이 없으면 `new ApiException(ErrorCode.RESOURCE_NOT_FOUND)`를 던지고, canonical `resolvedByFeedbackId`와 요청 `feedbackId`를 답장 후보로 계산한다. Repository 쿼리는 피드백의 `userProfileId`와 두 후보 ID에 연결된 `REPLY` 편지를 `publishedAt DESC, id DESC`로 정렬하고 첫 한 건만 Service에 제공한다. 기존 답장 작성 동작과 DB 스키마는 변경하지 않는다.

- [x] **Step 4: 집중 테스트를 통과시킨다.**

  Run: `./gradlew test --tests com.landit.landitbe.feature.mailbox.AdminMailboxApiIntegrationTests`

  Expected: `BUILD SUCCESSFUL`.

  Result: `./gradlew test --tests com.landit.landitbe.feature.mailbox.AdminMailboxApiIntegrationTests` → `BUILD SUCCESSFUL in 5s` (최종 fix 회귀·OpenAPI nullable 테스트 포함).

- [x] **Step 5: 전체 저장소 검증을 실행하고 결과를 이 문서에 기록한다.**

  Run: `./gradlew check`

  Expected: `BUILD SUCCESSFUL`.

  Result: `./gradlew check` → `BUILD SUCCESSFUL in 31s` (최종 fix 검증).

- [x] **Step 6: 변경을 자체 검토하고 하나의 논리적 커밋으로 저장한다.**

  Run: `git diff --check`와 `git diff --stat`.

  Commits:

  - `feat: 어드민 피드백 상세 조회 API 추가`.
  - `test: 어드민 피드백 상세 조회 계약 검증`.
  - `docs: LAN-374 피드백 상세 조회 설계와 검증 기록`.

  Result: 커밋 전 자체 검토와 `git diff --check`를 완료했고 모두 출력 없이 통과했다. 구현, 통합 테스트, 작업 문서를 저장소 커밋 컨벤션에 맞는 논리 단위로 분리했다.
