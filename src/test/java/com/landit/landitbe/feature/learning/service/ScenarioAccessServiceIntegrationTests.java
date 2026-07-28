// 사용자 시나리오 복습 권한의 저장과 중복 완료 처리를 검증한다.

package com.landit.landitbe.feature.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 시나리오 복습 권한의 보유 여부, 목록 조회와 중복 완료 처리를 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ScenarioAccessServiceIntegrationTests {

  private static final long USER_ID = 992001L;
  private static final long SCENARIO_ID = 992101L;
  private static final LocalDateTime GRANTED_AT = LocalDateTime.of(2026, 7, 28, 12, 0);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ScenarioAccessService scenarioAccessService;

  @Test
  void grantsAccessOnceWhenSameScenarioIsCompletedRepeatedly() {
    seedUserProfile(USER_ID);
    seedScenario(SCENARIO_ID);

    scenarioAccessService.grantAccess(USER_ID, SCENARIO_ID, Locale.EN, GRANTED_AT);
    scenarioAccessService.grantAccess(USER_ID, SCENARIO_ID, Locale.EN, GRANTED_AT.plusMinutes(1));

    assertThat(scenarioAccessService.hasAccess(USER_ID, SCENARIO_ID, Locale.EN)).isTrue();
    assertThat(scenarioAccessService.hasAccess(USER_ID, SCENARIO_ID, Locale.KR)).isFalse();
    assertThat(scenarioAccessService.findAccessibleScenarioIds(USER_ID, Locale.EN))
        .containsExactly(SCENARIO_ID);
    assertThat(accessCount()).isEqualTo(1);
    assertThat(grantedAt()).isEqualTo(GRANTED_AT);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void grantsAccessIdempotentlyInConcurrentTransactions() throws Exception {
    long concurrentUserId = 992002L;
    long concurrentScenarioId = 992102L;
    seedUserProfile(concurrentUserId);
    seedScenario(concurrentScenarioId);

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    Callable<Void> grantAccess =
        () -> {
          ready.countDown();
          assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
          scenarioAccessService.grantAccess(
              concurrentUserId, concurrentScenarioId, Locale.EN, GRANTED_AT);
          return null;
        };

    try {
      Future<Void> first = executorService.submit(grantAccess);
      Future<Void> second = executorService.submit(grantAccess);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      assertThatCode(() -> first.get(10, TimeUnit.SECONDS)).doesNotThrowAnyException();
      assertThatCode(() -> second.get(10, TimeUnit.SECONDS)).doesNotThrowAnyException();
    } finally {
      executorService.shutdownNow();
    }

    assertThat(accessCount(concurrentUserId, concurrentScenarioId)).isEqualTo(1);
  }

  private void seedUserProfile(long userId) {
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, push_permission_status,
            status, created_at, updated_at
        )
        values (?, 'scenario-access-test-user', 'EN', 'KR', 1, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId);
  }

  private void seedScenario(long scenarioId) {
    jdbcTemplate.update(
        """
        insert into category (id, display_order, status, created_at, updated_at)
        values (?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        scenarioId);
    jdbcTemplate.update(
        """
        insert into scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            display_order, status, created_at, updated_at
        )
        values (?, ?, 'friend', 'EASY', 'AI', 2, 1, 'ACTIVE',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        scenarioId,
        scenarioId);
  }

  private int accessCount() {
    return accessCount(USER_ID, SCENARIO_ID);
  }

  private int accessCount(long userId, long scenarioId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from user_scenario_access
            where user_profile_id = ?
              and scenario_id = ?
              and target_locale = 'EN'
            """,
            Integer.class,
            userId,
            scenarioId);
    return count == null ? 0 : count;
  }

  private LocalDateTime grantedAt() {
    return jdbcTemplate.queryForObject(
        """
        select granted_at
        from user_scenario_access
        where user_profile_id = ?
          and scenario_id = ?
          and target_locale = 'EN'
        """,
        LocalDateTime.class,
        USER_ID,
        SCENARIO_ID);
  }
}
