# LAN-144 Next Message Early Response Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 일반 턴에서 최종 `aiMessage`가 준비되면 속마음과 메시지 피드백 완료를 기다리지 않고 응답하고, 속마음은 별도 조회 API로 전달한다.

**Architecture:** `landit-ai`의 다음 메시지와 속마음 동기 API를 BE가 동시에 호출한다. 요청 스레드는 다음 메시지만 기다리고, 속마음 결과는 별도 트랜잭션으로 저장하며 FE는 1초 간격으로 BE를 폴링한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Java `CompletableFuture`, Flyway, PostgreSQL, H2, MockMvc

## Global Constraints

- 이 계획은 `docs/tasks/LAN-144/design.md`를 단일 설계 기준으로 사용한다.
- 일반 턴만 분리하고 종료 턴의 `closing-message` 계약은 유지한다.
- `ProcessingStatus`를 재사용하고 새 상태 enum을 만들지 않는다.
- FE 폴링 주기는 1초, `PREPARING` 만료 기준은 90초다.
- SSE, WebSocket, SQS, 신규 Worker, 자동 재시도는 추가하지 않는다.
- 새 Java 소스 파일 첫 줄에는 역할을 설명하는 한국어 한 줄 주석을 둔다.

---

### Task 1: 속마음 처리 상태 저장

**Files:**
- Create: `src/main/resources/db/migration/V20__add_inner_thought_processing_status.sql`
- Modify: `src/main/java/com/landit/landitbe/session/domain/SessionHistoryMessage.java`
- Modify: `src/main/java/com/landit/landitbe/session/api/dto/SessionMessageSubmitResponse.java`
- Modify: `src/main/java/com/landit/landitbe/session/application/GeneratedMessageRecorder.java`
- Test: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`
- Test: `src/test/java/com/landit/landitbe/session/ScenarioSessionApiIntegrationTests.java`

**Interfaces:**
- Produces: 사용자 메시지의 nullable `innerThoughtProcessingStatus`와 `PREPARING`, `COMPLETED`, `FAILED` 전이 메서드.
- Produces: 제출 응답의 `submittedMessage.innerThoughtProcessingStatus` 필드.

- [ ] **Step 1: 스키마와 응답 계약 실패 테스트를 작성한다.**

```java
assertColumnExists("session_history_message", "inner_thought_processing_status");

mockMvc.perform(post("/api/v1/sessions/{sessionId}/messages", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(jsonPath("$.data.submittedMessage.innerThoughtProcessingStatus")
                .value("PREPARING"));
```

- [ ] **Step 2: 관련 테스트가 새 컬럼과 응답 필드 부재로 실패하는지 확인한다.**

Run: `./gradlew test --tests com.landit.landitbe.DatabaseSchemaIntegrationTests --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`

Expected: 새 컬럼 또는 `innerThoughtProcessingStatus`가 없어 FAIL.

- [ ] **Step 3: V20 마이그레이션과 엔티티 상태 전이를 최소 구현한다.**

```sql
-- 사용자 메시지의 속마음 비동기 처리 상태를 저장한다.
ALTER TABLE session_history_message
    ADD COLUMN inner_thought_processing_status VARCHAR(20);

UPDATE session_history_message
SET inner_thought_processing_status = CASE
    WHEN inner_thought IS NOT NULL AND inner_thought_type IS NOT NULL THEN 'COMPLETED'
    ELSE 'FAILED'
END
WHERE role = 'USER';

ALTER TABLE session_history_message
    ADD CONSTRAINT chk_session_message_inner_thought_status
        CHECK (inner_thought_processing_status IS NULL
            OR inner_thought_processing_status IN ('PREPARING', 'COMPLETED', 'FAILED'));
```

`SessionHistoryMessage.user(...)`는 `PREPARING`으로 시작한다. 기존 `recordInnerThought(...)`는 속마음 두 필드와 상태 `COMPLETED`를 함께 기록하고, `markInnerThoughtFailed()`는 `PREPARING`일 때만 `FAILED`로 전환한다. AI 메시지는 상태를 `null`로 유지한다. `GeneratedMessageRecorder`는 `generation.completed()`인 종료 턴에서만 이 메서드를 호출해 일반 턴의 최초 상태를 `PREPARING`으로 유지한다.

- [ ] **Step 4: 제출 응답 매핑을 상태 기반으로 변경한다.**

```java
public record SubmittedMessageResponse(
        Long messageId,
        int turnNumber,
        int messageSequence,
        String role,
        String feedbackProcessingStatus,
        String innerThoughtProcessingStatus,
        String innerThought,
        String innerThoughtType
) {
}
```

일반 턴은 `PREPARING`과 null 속마음을, 종료 턴은 `COMPLETED`와 기존 `closing-message` 속마음을 반환하도록 `GeneratedMessageRecorder` 테스트를 갱신한다.

- [ ] **Step 5: 관련 테스트를 다시 실행한다.**

Run: `./gradlew test --tests com.landit.landitbe.DatabaseSchemaIntegrationTests --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`

Expected: PASS.

- [ ] **Step 6: 상태 저장 변경을 커밋한다.**

```bash
git add src/main/resources/db/migration/V20__add_inner_thought_processing_status.sql \
  src/main/java/com/landit/landitbe/session/domain/SessionHistoryMessage.java \
  src/main/java/com/landit/landitbe/session/api/dto/SessionMessageSubmitResponse.java \
  src/main/java/com/landit/landitbe/session/application/GeneratedMessageRecorder.java \
  src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java \
  src/test/java/com/landit/landitbe/session/ScenarioSessionApiIntegrationTests.java
git commit -m "feat: 속마음 처리 상태 저장"
```

### Task 2: AI 다음 메시지와 속마음 계약 분리

**Files:**
- Create: `src/main/java/com/landit/landitbe/session/application/port/AiInnerThoughtRequest.java`
- Create: `src/main/java/com/landit/landitbe/session/application/port/AiInnerThoughtResult.java`
- Modify: `src/main/java/com/landit/landitbe/session/application/port/AiConversationClient.java`
- Modify: `src/main/java/com/landit/landitbe/session/application/port/AiNextMessageResult.java`
- Modify: `src/main/java/com/landit/landitbe/session/infrastructure/ai/RemoteAiConversationClient.java`
- Modify: `src/main/java/com/landit/landitbe/session/infrastructure/ai/LocalAiConversationClient.java`
- Modify: `src/main/java/com/landit/landitbe/session/application/SessionMessageAiGenerator.java`
- Test: `src/test/java/com/landit/landitbe/session/infrastructure/ai/RemoteAiConversationClientTest.java`
- Test: `src/test/java/com/landit/landitbe/session/ScenarioSessionApiIntegrationTests.java`

**Interfaces:**
- Produces: `AiConversationClient.generateInnerThought(AiInnerThoughtRequest)`.
- Produces: 다음 메시지 결과에서 속마음 필드를 제거한 `AiNextMessageResult`.

- [ ] **Step 1: 분리된 원격 계약 테스트를 작성한다.**

```java
public record AiInnerThoughtRequest(
        Long sessionId,
        Long submittedMessageId,
        int submittedTurnNumber,
        AiScenarioContext scenario,
        List<AiConversationHistoryMessage> conversationHistory
) {
}

public record AiInnerThoughtResult(
        Long sessionId,
        Long messageId,
        String innerThought,
        InnerThoughtType innerThoughtType
) {
}
```

테스트 서버의 `/api/v1/conversation/next-message` 응답에서는 속마음 필드를 제거하고, `/api/v1/conversation/inner-thought`에 대해 요청 식별자·마지막 USER 히스토리 전송과 성공 매핑을 검증한다. 빈 결과는 502, 비 2xx와 I/O 실패는 503으로 매핑되는지 각각 검증한다.

- [ ] **Step 2: 원격 클라이언트 테스트가 새 메서드 부재로 실패하는지 확인한다.**

Run: `./gradlew test --tests com.landit.landitbe.session.infrastructure.ai.RemoteAiConversationClientTest`

Expected: `generateInnerThought`와 새 결과 타입이 없어 컴파일 FAIL.

- [ ] **Step 3: 포트와 원격·로컬 클라이언트를 구현한다.**

```java
public interface AiConversationClient {
    AiNextMessageResult generateNextMessage(AiNextMessageRequest request);
    AiInnerThoughtResult generateInnerThought(AiInnerThoughtRequest request);
    AiClosingMessageResult generateClosingMessage(AiClosingMessageRequest request);
    AiMessageFeedbackResult requestMessageFeedback(AiMessageFeedbackRequest request);
    AiSessionFeedbackResult generateSessionFeedback(AiSessionFeedbackRequest request);
}
```

`RemoteAiConversationClient`는 `INNER_THOUGHT_PATH = "/api/v1/conversation/inner-thought"`를 사용한다. 응답의 식별자, 속마음, 타입을 모두 검증하고 기존 `AI_RESPONSE_INVALID` 502와 `AI_GENERATION_FAILED` 503 매핑을 재사용한다. `LocalAiConversationClient`는 요청 식별자와 고정 속마음을 반환한다.

- [ ] **Step 4: 다음 메시지 생성 결과에서 속마음을 제거한다.**

```java
public record AiNextMessageResult(
        String aiMessage,
        String translatedMessage,
        GoalCompletionStatus goalCompletionStatus
) {
}
```

`SessionMessageAiGenerator`의 일반 턴은 속마음 없이 `Generation`을 만들고, 종료 턴만 기존 `closing-message` 속마음을 유지한다.

통합 테스트의 `FakeAiConversationClient`도 새 메서드를 구현하고 다음 메시지 결과 생성자에서 속마음 필드를 제거해 전체 테스트 소스가 컴파일되게 한다.

- [ ] **Step 5: 원격 클라이언트 테스트를 다시 실행한다.**

Run: `./gradlew test --tests com.landit.landitbe.session.infrastructure.ai.RemoteAiConversationClientTest`

Expected: PASS.

- [ ] **Step 6: AI 계약 분리를 커밋한다.**

```bash
git add src/main/java/com/landit/landitbe/session/application/port \
  src/main/java/com/landit/landitbe/session/infrastructure/ai/RemoteAiConversationClient.java \
  src/main/java/com/landit/landitbe/session/infrastructure/ai/LocalAiConversationClient.java \
  src/main/java/com/landit/landitbe/session/application/SessionMessageAiGenerator.java \
  src/test/java/com/landit/landitbe/session/infrastructure/ai/RemoteAiConversationClientTest.java \
  src/test/java/com/landit/landitbe/session/ScenarioSessionApiIntegrationTests.java
git commit -m "feat: 다음 메시지와 속마음 AI 계약 분리"
```

### Task 3: 일반 턴 병렬 실행과 결과 저장

**Files:**
- Create: `src/main/java/com/landit/landitbe/session/application/SessionInnerThoughtGenerator.java`
- Create: `src/main/java/com/landit/landitbe/session/application/SessionInnerThoughtResultRecorder.java`
- Modify: `src/main/java/com/landit/landitbe/session/application/SessionMessageSubmitUseCase.java`
- Modify: `src/main/java/com/landit/landitbe/session/infrastructure/SessionHistoryMessageRepository.java`
- Test: `src/test/java/com/landit/landitbe/session/ScenarioSessionApiIntegrationTests.java`

**Interfaces:**
- Consumes: Task 2의 `generateInnerThought` 포트.
- Produces: `SessionInnerThoughtGenerator.generate(SubmittedMessageContext)`.
- Produces: `SessionInnerThoughtResultRecorder.complete(Long, AiInnerThoughtResult)`와 `fail(Long)`.
- Produces: 다음 메시지만 기다리는 일반 턴 실행 흐름과 최초 성공 상태만 확정하는 속마음 저장.

- [ ] **Step 1: 일반 턴이 속마음 완료를 기다리지 않는 통합 테스트를 작성한다.**

`FakeAiConversationClient`에 latch를 두어 속마음 호출을 멈춘 상태에서도 제출 API가 다음 메시지와 `innerThoughtProcessingStatus = PREPARING`을 반환하는지 검증한다. latch 해제 후 폴링 가능한 DB 상태가 `COMPLETED`와 속마음 값으로 바뀌는지 검증한다. 속마음 실패 시 상태만 `FAILED`가 되고 다음 AI 메시지는 남는지 검증한다.

- [ ] **Step 2: 통합 테스트가 현재 순차 흐름에서 실패하는지 확인한다.**

Run: `./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`

Expected: 속마음 API 부재 또는 제출 요청 대기로 FAIL.

- [ ] **Step 3: 속마음 요청 조립과 결과 검증을 구현한다.**

`SessionInnerThoughtGenerator`는 `SubmittedMessageContext`의 식별자, 시나리오, 전체 히스토리를 `AiInnerThoughtRequest`로 변환한다. 응답의 `sessionId`와 `messageId`가 요청값과 일치하고 두 속마음 필드가 유효한지 검증한다.

- [ ] **Step 4: 일반 턴의 비필수 작업을 먼저 시작하고 다음 메시지만 기다린다.**

`SessionMessageSubmitUseCase`에 Spring Boot가 제공하는 `org.springframework.core.task.TaskExecutor`를 주입하고 별도 executor 설정은 만들지 않는다.

```java
CompletableFuture<AiInnerThoughtResult> innerThoughtFuture = CompletableFuture.supplyAsync(
        () -> sessionInnerThoughtGenerator.generate(submittedContext),
        taskExecutor
);
CompletableFuture<ProcessingStatus> feedbackFuture = CompletableFuture.supplyAsync(
        () -> requestMessageFeedback(submittedContext),
        taskExecutor
);

SessionMessageAiGenerator.Generation generation = generateAiMessage(submittedContext);
SessionMessageSubmitResponse response = executeInTransaction(() -> generatedMessageRecorder.record(
        submittedContext,
        generation,
        ProcessingStatus.PREPARING
));
```

일반 턴에서만 위 흐름을 사용한다. 다음 메시지 저장이 끝난 뒤 `whenCompleteAsync`로 속마음 성공·실패를 별도 트랜잭션에 기록하고, 메시지 피드백 future 실패는 로그만 남긴다. 다음 메시지 생성·저장 실패 시 두 future를 취소하고 기존 사용자 메시지 정리를 유지한다. 종료 턴은 기존 순차 흐름과 `closing-message` 속마음을 유지한다.

기존 메시지 피드백 실패·응답 불일치 테스트는 사용자 메시지 롤백 대신 다음 메시지 응답 유지와 경고 로그 경계를 검증하도록 바꾼다.

- [ ] **Step 5: 최초 터미널 상태만 저장하도록 조건부 업데이트를 구현한다.**

`SessionHistoryMessageRepository`에 상태가 `PREPARING`일 때만 `COMPLETED` 또는 `FAILED`로 바꾸는 `@Modifying` 쿼리를 추가한다. 완료 쿼리는 속마음 두 필드도 함께 저장한다. `SessionInnerThoughtResultRecorder`는 이 쿼리를 트랜잭션 안에서 호출해 늦게 도착한 결과가 이미 확정된 상태를 덮어쓰지 못하게 한다.

- [ ] **Step 6: 일반 턴 통합 테스트를 다시 실행한다.**

Run: `./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`

Expected: PASS.

- [ ] **Step 7: 병렬 실행 변경을 커밋한다.**

```bash
git add src/main/java/com/landit/landitbe/session/application/SessionInnerThoughtGenerator.java \
  src/main/java/com/landit/landitbe/session/application/SessionInnerThoughtResultRecorder.java \
  src/main/java/com/landit/landitbe/session/application/SessionMessageSubmitUseCase.java \
  src/main/java/com/landit/landitbe/session/infrastructure/SessionHistoryMessageRepository.java \
  src/test/java/com/landit/landitbe/session/ScenarioSessionApiIntegrationTests.java
git commit -m "feat: 다음 메시지 응답 경로와 속마음 생성 분리"
```

### Task 4: 속마음 폴링 API와 최종 검증

**Files:**
- Create: `src/main/java/com/landit/landitbe/session/api/dto/SessionInnerThoughtResponse.java`
- Create: `src/main/java/com/landit/landitbe/session/application/SessionInnerThoughtQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/session/api/SessionController.java`
- Modify: `src/main/java/com/landit/landitbe/session/application/LearningSessionFinder.java`
- Test: `src/test/java/com/landit/landitbe/session/ScenarioSessionApiIntegrationTests.java`

**Interfaces:**
- Produces: `GET /api/v1/sessions/{sessionId}/messages/{messageId}/inner-thought`.

- [ ] **Step 1: 폴링 API의 상태·권한·만료 테스트를 작성한다.**

```java
public record SessionInnerThoughtResponse(
        String processingStatus,
        String innerThought,
        String innerThoughtType
) {
}
```

`PREPARING`과 `FAILED`는 null 내용, `COMPLETED`는 저장된 내용, 타 사용자 세션은 403, 없는 세션·메시지는 404를 반환하는지 검증한다. 테스트 데이터의 생성 시각을 90초 이전으로 변경한 뒤 조회하면 `FAILED`로 전환되는지도 검증한다.

- [ ] **Step 2: 새 GET API 부재로 테스트가 실패하는지 확인한다.**

Run: `./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`

Expected: GET 경로가 없어 FAIL.

- [ ] **Step 3: 소유권 확인과 90초 만료를 구현한다.**

`LearningSessionFinder`에 진행 상태를 제한하지 않는 소유 세션 조회 메서드를 추가한다. `SessionInnerThoughtQueryService`는 세션 소유권, 메시지의 세션 히스토리 소속, USER 역할을 확인한다. `PREPARING`이고 `createdAt`이 현재보다 90초 이상 이전이면 Task 3의 조건부 실패 업데이트를 호출한 뒤 최신 상태를 다시 조회한다.

- [ ] **Step 4: 컨트롤러와 OpenAPI 설명을 추가한다.**

```java
@GetMapping("/{sessionId}/messages/{messageId}/inner-thought")
public ResponseEntity<ApiResponse<SessionInnerThoughtResponse>> getInnerThought(
        @AuthenticationPrincipal AuthUserPrincipal principal,
        @PathVariable Long sessionId,
        @PathVariable Long messageId
) {
    return ApiResponse.success(
            HttpStatus.OK,
            sessionInnerThoughtQueryService.get(principal.userId(), sessionId, messageId)
    );
}
```

- [ ] **Step 5: 폴링 API 테스트와 전체 테스트를 실행한다.**

Run: `./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`

Expected: PASS.

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 6: HIGH 위험 검증으로 실제 diff와 테스트 증거를 독립 검토한다.**

검토자는 다음 메시지 응답이 속마음·피드백 future를 기다리지 않는지, 종료 턴 계약이 유지되는지, 조건부 상태 업데이트가 늦은 완료와 90초 만료의 경합을 막는지 확인한다. 발견된 기준 위반만 수정하고 영향을 받은 테스트와 `./gradlew test`를 다시 실행한다.

- [ ] **Step 7: 폴링 API를 커밋한다.**

```bash
git add src/main/java/com/landit/landitbe/session/api/dto/SessionInnerThoughtResponse.java \
  src/main/java/com/landit/landitbe/session/application/SessionInnerThoughtQueryService.java \
  src/main/java/com/landit/landitbe/session/api/SessionController.java \
  src/main/java/com/landit/landitbe/session/application/LearningSessionFinder.java \
  src/test/java/com/landit/landitbe/session/ScenarioSessionApiIntegrationTests.java
git commit -m "feat: 속마음 처리 상태 조회 API 추가"
```

## 구현 결과

- `origin/develop`에 같은 버전 마이그레이션이 추가되어 속마음 상태 마이그레이션은 V20으로 반영했다.
- 일반 턴은 `applicationTaskExecutor`에서 다음 질문, 속마음, 메시지별 피드백 요청을 병렬 시작하고 다음 질문 저장 뒤에만 속마음 상태를 조건부로 확정한다.
- 속마음 조회 API는 소유권을 확인하고, `PREPARING`이 90초 이상이면 조건부로 `FAILED`로 바꾼다.
- 완료·실패·만료·종료 턴·권한 경계와 전체 테스트 검증을 완료했고, 독립 검토에서 제안한 만료-완료 경합 테스트도 반영했다.
