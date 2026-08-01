# LAN-234 스트릭 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 정상 완료 대화를 날짜별로 집계하고 현재 스트릭·월별 달력 API를 제공한다.

**Architecture:** `feature.character`의 기존 일별 활동과 요약 엔티티를 `StreakService`가 소유한다. 시나리오 완료 트랜잭션에서 활동을 기록하며, 조회 API는 요약과 월 범위 활동 행을 읽는다.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, H2 테스트 DB, MockMvc.

## Global Constraints

- 날짜는 `Asia/Seoul` 기준이다.
- 정상 완료만 반영하고 GET 요청은 데이터를 변경하지 않는다.
- 기존 `UNIQUE (user_profile_id, activity_date)` 복합 인덱스를 사용한다.
- 새 Java 소스 첫 줄에는 한국어 역할 주석과 public API Javadoc을 작성한다.

---

### Task 1: 스트릭 도메인과 조회 Service

**Files:**
- Modify: `feature/character/domain/UserDailyActivity.java`, `UserLearningActivitySummary.java`
- Create: `feature/character/repository/UserDailyActivityRepository.java`, `UserLearningActivitySummaryRepository.java`, `feature/character/service/StreakService.java`
- Test: `src/test/java/com/landit/landitbe/feature/character/service/StreakServiceTest.java`

**Interfaces:**
- Produces: `recordCompletedConversation(long userId, LocalDateTime completedAt)`, `getCurrentStreak(long userId)`, `getCalendar(long userId, YearMonth yearMonth)`.

- [x] Write failing tests for first day, yesterday 연속, 같은 날 재완료, 공백 뒤 재시작, 월 범위 조회와 만료된 조회값.

```java
@Test
void recordsOnlyOneActiveDayForSameDate() {
  service.recordCompletedConversation(USER_ID, TODAY_AT_NOON);
  service.recordCompletedConversation(USER_ID, TODAY_AT_EVENING);
  assertThat(service.getCalendar(USER_ID, YearMonth.from(TODAY)).totalActiveDays()).isEqualTo(1);
}
```

- [x] Add entity factories and state-change methods, repositories for 사용자·날짜 단건 조회와 월 범위 조회, and `StreakService`.
- [x] Create or update the daily row and summary in the caller transaction. The completion flow owns the user-profile lock order.
- [x] Run `./gradlew test --tests '*StreakServiceTest'` and commit the domain slice.

### Task 2: 스트릭 조회 HTTP API

**Files:**
- Create: `feature/character/StreakController.java`, `feature/character/docs/StreakControllerDocs.java`, `feature/character/dto/CurrentStreakResponse.java`, `StreakCalendarResponse.java`
- Test: `src/test/java/com/landit/landitbe/feature/character/StreakApiIntegrationTests.java`

**Interfaces:**
- Produces: `GET /api/v1/me/streak` and `GET /api/v1/me/streak/calendar?year={year}&month={month}`.

- [x] Write failing MockMvc tests for 인증 실패, 기본값, 월별 날짜 정렬, `month` 범위 오류와 OpenAPI 경로.

```java
mockMvc.perform(get("/api/v1/me/streak/calendar?year=2026&month=13"))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
```

- [x] Add controller, DTO and OpenAPI documentation. Validate `year >= 1` and `1 <= month <= 12`.
- [x] Run `./gradlew test --tests '*StreakApiIntegrationTests'` and commit the HTTP slice.

### Task 3: 시나리오 완료 연결과 회귀 검증

**Files:**
- Modify: `feature/session/service/SessionMessageSubmitService.java`, `GeneratedMessageService.java`
- Test: `src/test/java/com/landit/landitbe/feature/session/ScenarioSessionApiIntegrationTests.java`

**Interfaces:**
- Consumes: `StreakService.recordCompletedConversation(...)`.
- Produces: `sessionCompleted=true` 응답 뒤 스트릭 달력에서 오늘 완료를 조회할 수 있는 계약.

- [x] Write a failing 완료 흐름 테스트 that posts the last message, waits for `sessionCompleted=true`, then queries the calendar and expects today in `activeDates`.

```java
submitLastMessage(sessionId).andExpect(jsonPath("$.data.sessionCompleted").value(true));
mockMvc.perform(get("/api/v1/me/streak/calendar?year=2026&month=7").header(AUTHORIZATION, token))
    .andExpect(jsonPath("$.data.activeDates[0]").value("2026-07-30"));
```

- [x] Acquire the profile lock before `GeneratedMessageService` locks the learning session. On a completed generation, call `StreakService` after `completeBySystem` in the same `TransactionTemplate`.
- [x] Run the affected session and streak tests, then run `./gradlew check`.
- [x] Review the diff, update this plan with actual verification, and commit the integration slice.

## Verification

- `./gradlew check` 통과.
