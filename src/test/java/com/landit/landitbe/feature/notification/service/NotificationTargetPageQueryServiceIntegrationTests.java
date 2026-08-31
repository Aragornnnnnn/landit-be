// 예약 알림 대상 일괄 조회가 날짜별 시나리오와 스몰톡 사용량을 정확히 조립하는지 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 예약 알림 대상 일괄 조회가 날짜별 시나리오와 스몰톡 사용량을 정확히 조립하는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class NotificationTargetPageQueryServiceIntegrationTests {

  private static final long USER_ID = 9_950_001L;
  private static final long UNSENDABLE_USER_ID = 9_950_002L;
  private static final long CATEGORY_ID = 9_950_001L;
  private static final long FIRST_SCENARIO_ID = 9_950_001L;
  private static final long DAILY_SCENARIO_ID = 9_950_002L;
  private static final LocalDate SCHEDULED_DATE = LocalDate.of(2026, 7, 30);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private NotificationTargetPageQueryService queryService;

  /** 오늘 완료한 시나리오와 시나리오 출처 표현 완료만 반영하고 날짜별 사용량을 조립한다. */
  @Test
  void loadsCompletedDailyScenarioAndScenarioExpressionProgress() {
    seedUser();
    jdbcTemplate.update("UPDATE user_profile SET learning_level = 2 WHERE id = ?", USER_ID);
    seedCategory();
    seedScenario(FIRST_SCENARIO_ID, 1);
    seedScenario(DAILY_SCENARIO_ID, 2);
    insertScenarioAccess(FIRST_SCENARIO_ID, LocalDateTime.of(2026, 7, 29, 10, 0));
    insertScenarioAccess(DAILY_SCENARIO_ID, LocalDateTime.of(2026, 7, 30, 9, 0));
    long completedExpressionId = insertExpression(DAILY_SCENARIO_ID, 1);
    long freeTalkOnlyExpressionId = insertExpression(DAILY_SCENARIO_ID, 2);
    insertExpression(DAILY_SCENARIO_ID, 3, 4);
    insertExpressionCompletion(completedExpressionId, DAILY_SCENARIO_ID, "SCENARIO");
    insertExpressionCompletion(freeTalkOnlyExpressionId, DAILY_SCENARIO_ID, "FREE_TALK");
    jdbcTemplate.update(
        """
        INSERT INTO free_talk_daily_speaking_usage (user_profile_id, usage_date, used_speaking_duration_ms)
        VALUES (?, ?, ?)
        """,
        USER_ID,
        SCHEDULED_DATE,
        12_000L);

    NotificationTargetPage page = queryService.loadPage(USER_ID - 1, 1, SCHEDULED_DATE);
    NotificationTargetSelectionInput input = page.inputs().get(USER_ID);

    assertThat(page.userProfileIds()).containsExactly(USER_ID);
    assertThat(input.dailyScenarioId()).isEqualTo(DAILY_SCENARIO_ID);
    assertThat(input.dailyScenarioCompleted()).isTrue();
    assertThat(input.freeTalkUsedSpeakingDurationMs()).isEqualTo(12_000L);
    assertThat(input.expressions())
        .extracting(ExpressionNotificationCandidate::expressionId)
        .containsExactly(completedExpressionId, freeTalkOnlyExpressionId);
    assertThat(input.expressions().get(0).completed()).isTrue();
    assertThat(input.expressions().get(1).completed()).isFalse();
    assertThat(input.expressions().get(0).targetExpressionText()).isEqualTo("표현");
  }

  /** 닉네임, 날짜별 활동과 저장된 스트릭 요약을 예약 날짜 기준으로 조립한다. */
  @Test
  void loadsActivityAndStreakFieldsForScheduledDate() {
    seedUser();
    insertDailyActivity(LocalDate.of(2026, 7, 28), true);
    insertDailyActivity(LocalDate.of(2026, 7, 29), false);
    jdbcTemplate.update(
        """
        INSERT INTO user_learning_activity_summary (
            user_profile_id, total_session_count, completed_scenario_count,
            completed_free_talk_count, completed_review_count, total_turn_count,
            total_study_seconds, learned_expression_count, current_streak_days,
            longest_streak_days, created_at, updated_at
        )
        VALUES (?, 2, 2, 0, 0, 2, 60, 0, 2, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID);

    NotificationTargetSelectionInput input =
        queryService.loadPage(USER_ID - 1, 1, SCHEDULED_DATE).inputs().get(USER_ID);

    assertThat(input.nickname()).isEqualTo("notification-user");
    assertThat(input.activeToday()).isFalse();
    assertThat(input.activeYesterday()).isFalse();
    assertThat(input.currentStreakDays()).isEqualTo(2);
    assertThat(input.longestStreakDays()).isEqualTo(4);
    assertThat(input.priorActiveDayHistory()).isTrue();
    assertThat(input.missedDayCount()).isEqualTo(1);
  }

  @Test
  void nullLearningLevelIncludesAdvancedExpressions() {
    seedUser();
    seedCategory();
    seedScenario(DAILY_SCENARIO_ID, 1);
    long advancedExpressionId = insertExpression(DAILY_SCENARIO_ID, 1, 4);

    NotificationTargetPage page = queryService.loadPage(USER_ID - 1, 1, SCHEDULED_DATE);

    assertThat(page.inputs().get(USER_ID).expressions())
        .extracting(ExpressionNotificationCandidate::expressionId)
        .containsExactly(advancedExpressionId);
  }

  /** 오늘 완료 이력이 없으면 기존 접근 상태에서 첫 미완료 시나리오를 오늘 배정으로 계산한다. */
  @Test
  void loadsFirstUnclearedScenarioAsTodaysAssignment() {
    seedUser();
    seedCategory();
    seedScenario(FIRST_SCENARIO_ID, 1);
    seedScenario(DAILY_SCENARIO_ID, 2);
    insertScenarioAccess(FIRST_SCENARIO_ID, LocalDateTime.of(2026, 7, 29, 10, 0));

    NotificationTargetPage page = queryService.loadPage(USER_ID - 1, 1, SCHEDULED_DATE);
    NotificationTargetSelectionInput input = page.inputs().get(USER_ID);

    assertThat(input.dailyScenarioId()).isEqualTo(DAILY_SCENARIO_ID);
    assertThat(input.dailyScenarioCompleted()).isFalse();
  }

  /** ACTIVE 상태의 UserPushToken이 있는 사용자만 발송 가능 대상으로 조회한다. */
  @Test
  void loadsOnlyUsersWithActiveUserPushTokens() {
    seedUser();
    seedUser(UNSENDABLE_USER_ID, "unsendable-user");
    insertUserPushToken(USER_ID, "ExponentPushToken[sendable]", "ACTIVE");
    insertUserPushToken(UNSENDABLE_USER_ID, "ExponentPushToken[revoked]", "REVOKED");

    NotificationTargetPage page = queryService.loadPage(USER_ID - 1, 2, SCHEDULED_DATE);

    assertThat(page.userProfileIds()).containsExactly(USER_ID, UNSENDABLE_USER_ID);
    assertThat(page.sendableUserProfileIds()).containsExactly(USER_ID);
  }

  /** 최신 프리톡 사용자별 조회를 위한 인덱스를 Flyway로 생성한다. */
  @Test
  void createsLatestFreeTalkLookupIndex() {
    Integer indexCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.INDEXES
            WHERE LOWER(INDEX_NAME) = 'idx_learning_session_user_profile_started_at'
            """,
            Integer.class);

    assertThat(indexCount).isEqualTo(1);
  }

  private void seedUser() {
    seedUser(USER_ID, "notification-user");
  }

  private void seedUser(long userId, String nickname) {
    jdbcTemplate.update(
        """
        INSERT INTO user_profile (
            id, nickname, target_locale, base_locale, current_level,
            push_permission_status, status, created_at, updated_at
        )
        VALUES (?, ?, 'EN', 'KR', 1, 'NOT_DETERMINED', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        nickname);
  }

  private void insertUserPushToken(long userId, String expoPushToken, String status) {
    jdbcTemplate.update(
        """
        INSERT INTO user_push_token (
            user_profile_id, platform, expo_push_token, status, created_at, updated_at
        )
        VALUES (?, 'IOS', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        expoPushToken,
        status);
  }

  private void insertDailyActivity(LocalDate activityDate, boolean activeDay) {
    jdbcTemplate.update(
        """
        INSERT INTO user_daily_activity (
            user_profile_id, activity_date, completed_session_count, completed_review_count,
            study_seconds, review_all_correct_reward_xp, active_day, created_at, updated_at
        )
        VALUES (?, ?, 0, 0, 0, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID,
        activityDate,
        activeDay);
  }

  private void seedScenario(long scenarioId, int displayOrder) {
    jdbcTemplate.update(
        """
        INSERT INTO scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            display_order, status, created_at, updated_at
        )
        VALUES (?, ?, 'tutor', 'EASY', 'USER', 3, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        CATEGORY_ID,
        displayOrder);
    jdbcTemplate.update(
        """
        INSERT INTO scenario_language_variant (
            scenario_id, target_locale, base_locale, title, briefing,
            user_opening_instruction, conversation_goal, status, created_at, updated_at
        )
        VALUES (?, 'EN', 'KR', '알림 시나리오', '설명', '시작', '목표', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId);
  }

  private void seedCategory() {
    jdbcTemplate.update(
        """
        INSERT INTO category (id, display_order, status, created_at, updated_at)
        VALUES (?, 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        CATEGORY_ID);
    jdbcTemplate.update(
        """
        INSERT INTO category_language_variant (category_id, base_locale, name, created_at, updated_at)
        VALUES (?, 'KR', '알림 카테고리', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        CATEGORY_ID);
  }

  private long insertExpression(long scenarioId, int displayOrder) {
    return insertExpression(scenarioId, displayOrder, 3);
  }

  private long insertExpression(long scenarioId, int displayOrder, int difficultyLevel) {
    jdbcTemplate.update(
        """
        INSERT INTO writing_expression (
            scenario_id, expression_type, usage_frequency_level, difficulty_level, target_locale, base_locale,
            display_order, target_expression_text, base_expression_meaning_text, usage_summary,
            usage_description, representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            practice_examples_payload, status, created_at, updated_at
        )
        VALUES (?, 'DAILY_ROUTINE', 'BASIC', ?, 'EN', 'KR', ?, '표현', '뜻', '요약', '설명',
                '예문', '예문 번역', ARRAY['예문'], ARRAY['예문', '선택'], CAST('[]' AS jsonb),
                'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        difficultyLevel,
        displayOrder);
    return jdbcTemplate.queryForObject(
        "SELECT id FROM writing_expression WHERE scenario_id = ? AND display_order = ?",
        Long.class,
        scenarioId,
        displayOrder);
  }

  private void insertScenarioAccess(long scenarioId, LocalDateTime grantedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO user_scenario_access (
            user_profile_id, scenario_id, target_locale, granted_at, created_at, updated_at
        )
        VALUES (?, ?, 'EN', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID,
        scenarioId,
        grantedAt);
  }

  private void insertExpressionCompletion(
      long expressionId, long scenarioId, String learningSource) {
    jdbcTemplate.update(
        """
        INSERT INTO user_writing_expression_completion (
            user_profile_id, scenario_id, writing_expression_id, completed_at, last_completed_at, learning_source
        )
        VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
        """,
        USER_ID,
        scenarioId,
        expressionId,
        learningSource);
  }
}
