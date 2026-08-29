// 500명 단위로 오늘 예약 알림 대상 선정에 필요한 학습 데이터를 일괄 조회한다.

package com.landit.landitbe.feature.notification.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/** 500명 단위로 오늘 예약 알림 대상 선정에 필요한 학습 데이터를 일괄 조회한다. */
@Service
@RequiredArgsConstructor
public class NotificationTargetPageQueryService {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  /**
   * 활성 사용자를 Keyset 방식으로 조회하고 오늘의 시나리오·표현·스몰톡 사용량을 일괄 조립한다.
   *
   * @param lastUserProfileId 직전 페이지의 마지막 사용자 ID. 첫 페이지면 {@code 0}
   * @param pageSize 한 페이지 최대 사용자 수
   * @param scheduledDate 알림 정책을 평가할 서울 기준 날짜
   * @return 다음 대상 선정 페이지
   */
  public NotificationTargetPage loadPage(
      long lastUserProfileId, int pageSize, LocalDate scheduledDate) {
    List<Long> userIds =
        jdbcTemplate.query(
            """
            select id
            from user_profile
            where status = 'ACTIVE'
              and id > :lastUserProfileId
            order by id
            limit :pageSize
            """,
            new MapSqlParameterSource()
                .addValue("lastUserProfileId", lastUserProfileId)
                .addValue("pageSize", pageSize),
            (resultSet, rowNumber) -> resultSet.getLong("id"));
    if (userIds.isEmpty()) {
      return new NotificationTargetPage(List.of(), Map.of(), List.of());
    }

    Map<Long, UserTargetRows> rowsByUserId = new HashMap<>();
    userIds.forEach(userId -> rowsByUserId.put(userId, new UserTargetRows()));
    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("userIds", userIds)
            .addValue("scheduledDate", scheduledDate)
            .addValue("dayStart", scheduledDate.atStartOfDay())
            .addValue("nextDayStart", scheduledDate.plusDays(1).atStartOfDay());
    loadScenarioRows(parameters, rowsByUserId);
    loadDailyScenarioCompletionRows(parameters, rowsByUserId);
    loadExpressionRows(parameters, rowsByUserId);
    loadLatestScenarioCompletionRows(parameters, rowsByUserId);
    loadLatestExpressionCompletionRows(parameters, rowsByUserId);
    loadFreeTalkUsageRows(parameters, rowsByUserId);
    List<Long> sendableUserIds = findSendableUserIds(parameters);
    return new NotificationTargetPage(
        List.copyOf(userIds),
        rowsByUserId.entrySet().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, entry -> entry.getValue().toInput(entry.getKey()))),
        List.copyOf(sendableUserIds));
  }

  /** 현재 활성 UserPushToken이 하나 이상 있는 사용자를 한 번에 조회한다. */
  private List<Long> findSendableUserIds(MapSqlParameterSource parameters) {
    return jdbcTemplate.query(
        """
        select distinct user_profile_id
        from user_push_token
        where user_profile_id in (:userIds)
          and status = 'ACTIVE'
        """,
        parameters,
        (resultSet, rowNumber) -> resultSet.getLong("user_profile_id"));
  }

  /** 사용자의 언어에서 활성화된 시나리오와 접근 권한을 노출 순서로 조회한다. */
  private void loadScenarioRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select up.id as user_profile_id, s.id as scenario_id, usa.id as access_id
        from user_profile up
        join category_language_variant clv on clv.base_locale = up.base_locale
        join category c on c.id = clv.category_id and c.status = 'ACTIVE'
        join scenario s on s.category_id = c.id and s.status = 'ACTIVE'
        join scenario_language_variant slv on slv.scenario_id = s.id
          and slv.target_locale = up.target_locale and slv.base_locale = up.base_locale
          and slv.status = 'ACTIVE'
        left join user_scenario_access usa on usa.user_profile_id = up.id
          and usa.scenario_id = s.id and usa.target_locale = up.target_locale
        where up.id in (:userIds)
        order by up.id, s.display_order, s.id
        """,
        parameters,
        (RowCallbackHandler) resultSet -> addScenarioRow(resultSet, rowsByUserId));
  }

  /** 오늘 날짜에 시나리오를 완료한 최초 이력을 사용자별로 조회한다. */
  private void loadDailyScenarioCompletionRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select usa.user_profile_id, usa.scenario_id, usa.granted_at, usa.id
        from user_scenario_access usa
        join user_profile up on up.id = usa.user_profile_id
        where usa.user_profile_id in (:userIds)
          and usa.target_locale = up.target_locale
          and usa.granted_at >= :dayStart
          and usa.granted_at < :nextDayStart
        order by usa.user_profile_id, usa.granted_at, usa.id
        """,
        parameters,
        (RowCallbackHandler) resultSet -> addDailyScenarioCompletionRow(resultSet, rowsByUserId));
  }

  /** 오늘 완료한 시나리오의 활성 표현과 시나리오 출처 완료 여부를 조회한다. */
  private void loadExpressionRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select up.id as user_profile_id, s.id as scenario_id, we.id as expression_id,
               case when uwec.id is not null then true else false end as completed
        from user_profile up
        join category_language_variant clv on clv.base_locale = up.base_locale
        join category c on c.id = clv.category_id and c.status = 'ACTIVE'
        join scenario s on s.category_id = c.id and s.status = 'ACTIVE'
        join scenario_language_variant slv on slv.scenario_id = s.id
          and slv.target_locale = up.target_locale and slv.base_locale = up.base_locale
          and slv.status = 'ACTIVE'
        join writing_expression we on we.scenario_id = s.id
          and we.target_locale = up.target_locale and we.base_locale = up.base_locale
          and we.status = 'ACTIVE'
        left join user_writing_expression_completion uwec on uwec.user_profile_id = up.id
          and uwec.writing_expression_id = we.id and uwec.learning_source = 'SCENARIO'
        where up.id in (:userIds)
        order by up.id, s.display_order, s.id, we.display_order, we.id
        """,
        parameters,
        (RowCallbackHandler) resultSet -> addExpressionRow(resultSet, rowsByUserId));
  }

  /** 마지막 시나리오 완료 시각을 접근 권한 이력에서 사용자별로 일괄 조회한다. */
  private void loadLatestScenarioCompletionRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select user_profile_id, granted_at
        from user_scenario_access
        where user_profile_id in (:userIds)
        """,
        parameters,
        (RowCallbackHandler) resultSet -> addLatestScenarioCompletionRow(resultSet, rowsByUserId));
  }

  /** 마지막 표현 완료 시각을 사용자별로 일괄 조회한다. */
  private void loadLatestExpressionCompletionRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select user_profile_id, last_completed_at
        from user_writing_expression_completion
        where user_profile_id in (:userIds)
        """,
        parameters,
        (RowCallbackHandler)
            resultSet -> addLatestExpressionCompletionRow(resultSet, rowsByUserId));
  }

  /** 예약 날짜의 스몰톡 누적 발화 시간을 사용자별로 일괄 조회한다. */
  private void loadFreeTalkUsageRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select user_profile_id, used_speaking_duration_ms
        from free_talk_daily_speaking_usage
        where user_profile_id in (:userIds) and usage_date = :scheduledDate
        """,
        parameters,
        (RowCallbackHandler) resultSet -> addFreeTalkUsageRow(resultSet, rowsByUserId));
  }

  private void addScenarioRow(ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId)
      throws SQLException {
    UserTargetRows rows = rowsByUserId.get(resultSet.getLong("user_profile_id"));
    long scenarioId = resultSet.getLong("scenario_id");
    rows.scenarioIds.add(scenarioId);
    if (resultSet.getObject("access_id") != null) {
      rows.clearedScenarioIds.add(scenarioId);
    }
  }

  private void addDailyScenarioCompletionRow(
      ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId) throws SQLException {
    UserTargetRows rows = rowsByUserId.get(resultSet.getLong("user_profile_id"));
    if (!rows.dailyScenarioCompletionFound) {
      rows.dailyScenarioCompletionFound = true;
      rows.dailyScenarioId = resultSet.getLong("scenario_id");
    }
  }

  private void addExpressionRow(ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId)
      throws SQLException {
    UserTargetRows rows = rowsByUserId.get(resultSet.getLong("user_profile_id"));
    rows.expressions.add(
        new ExpressionNotificationCandidate(
            resultSet.getLong("scenario_id"),
            resultSet.getLong("expression_id"),
            resultSet.getBoolean("completed")));
  }

  private void addLatestScenarioCompletionRow(
      ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId) throws SQLException {
    UserTargetRows rows = rowsByUserId.get(resultSet.getLong("user_profile_id"));
    LocalDateTime completedAt = resultSet.getTimestamp("granted_at").toLocalDateTime();
    if (rows.lastScenarioCompletedAt == null || completedAt.isAfter(rows.lastScenarioCompletedAt)) {
      rows.lastScenarioCompletedAt = completedAt;
    }
  }

  private void addLatestExpressionCompletionRow(
      ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId) throws SQLException {
    UserTargetRows rows = rowsByUserId.get(resultSet.getLong("user_profile_id"));
    LocalDateTime completedAt = resultSet.getTimestamp("last_completed_at").toLocalDateTime();
    if (rows.lastExpressionCompletedAt == null
        || completedAt.isAfter(rows.lastExpressionCompletedAt)) {
      rows.lastExpressionCompletedAt = completedAt;
    }
  }

  private void addFreeTalkUsageRow(ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId)
      throws SQLException {
    rowsByUserId.get(resultSet.getLong("user_profile_id")).freeTalkUsedSpeakingDurationMs =
        resultSet.getLong("used_speaking_duration_ms");
  }

  private static class UserTargetRows {
    private final List<Long> scenarioIds = new ArrayList<>();
    private final Set<Long> clearedScenarioIds = new HashSet<>();
    private final List<ExpressionNotificationCandidate> expressions = new ArrayList<>();
    private Long dailyScenarioId;
    private boolean dailyScenarioCompletionFound;
    private long freeTalkUsedSpeakingDurationMs;
    private LocalDateTime lastScenarioCompletedAt;
    private LocalDateTime lastExpressionCompletedAt;

    private NotificationTargetSelectionInput toInput(Long userProfileId) {
      Long resolvedDailyScenarioId = dailyScenarioId;
      boolean dailyScenarioCompleted = dailyScenarioCompletionFound;
      if (!dailyScenarioCompletionFound) {
        resolvedDailyScenarioId =
            scenarioIds.stream()
                .filter(scenarioId -> !clearedScenarioIds.contains(scenarioId))
                .findFirst()
                .orElse(null);
        dailyScenarioCompleted = false;
      } else if (!scenarioIds.contains(dailyScenarioId)) {
        resolvedDailyScenarioId = null;
      }
      return new NotificationTargetSelectionInput(
          userProfileId,
          resolvedDailyScenarioId,
          dailyScenarioCompleted,
          freeTalkUsedSpeakingDurationMs,
          lastScenarioCompletedAt,
          lastExpressionCompletedAt,
          List.copyOf(expressions));
    }
  }
}
