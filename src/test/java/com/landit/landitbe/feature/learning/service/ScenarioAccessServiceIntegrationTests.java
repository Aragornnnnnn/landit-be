// 사용자 시나리오 복습 권한의 저장과 중복 완료 처리를 검증한다.

package com.landit.landitbe.feature.learning.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.shared.domain.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 시나리오 복습 권한의 보유 여부, 목록 조회와 중복 완료 처리를 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ScenarioAccessServiceIntegrationTests {

  private static final long USER_ID = 992001L;
  private static final long SCENARIO_ID = 992101L;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ScenarioAccessService scenarioAccessService;

  @Test
  void grantsAccessOnceWhenSameScenarioIsCompletedRepeatedly() {
    seedUserProfile(USER_ID);
    seedScenario(SCENARIO_ID);

    scenarioAccessService.grantAccess(USER_ID, SCENARIO_ID, Locale.EN);
    scenarioAccessService.grantAccess(USER_ID, SCENARIO_ID, Locale.EN);

    assertThat(scenarioAccessService.hasAccess(USER_ID, SCENARIO_ID, Locale.EN)).isTrue();
    assertThat(scenarioAccessService.hasAccess(USER_ID, SCENARIO_ID, Locale.KR)).isFalse();
    assertThat(scenarioAccessService.findAccessibleScenarioIds(USER_ID, Locale.EN))
        .containsExactly(SCENARIO_ID);
    assertThat(accessCount()).isEqualTo(1);
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
            USER_ID,
            SCENARIO_ID);
    return count == null ? 0 : count;
  }
}
