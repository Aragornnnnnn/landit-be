// 500명 단위로 알림 대상 선정에 필요한 학습 데이터를 일괄 조회한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.learning.domain.UserScenarioProgressStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/** 500명 단위로 알림 대상 선정에 필요한 학습 데이터를 일괄 조회한다. */
@Service
@RequiredArgsConstructor
public class NotificationTargetPageQueryService {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  /**
   * 활성 사용자를 Keyset 방식으로 조회하고 각 사용자의 시나리오·표현·완료 이력을 일괄 조립한다.
   *
   * @param lastUserProfileId 직전 페이지의 마지막 사용자 ID. 첫 페이지면 {@code 0}
   * @param pageSize 한 페이지 최대 사용자 수
   * @return 다음 대상 선정 페이지
   */
  public NotificationTargetPage loadPage(long lastUserProfileId, int pageSize) {
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
    MapSqlParameterSource parameters = new MapSqlParameterSource("userIds", userIds);
    loadScenarioRows(parameters, rowsByUserId);
    loadExpressionRows(parameters, rowsByUserId);
    loadLatestScenarioCompletionRows(parameters, rowsByUserId);
    loadLatestExpressionCompletionRows(parameters, rowsByUserId);
    List<Long> sendableUserIds = findSendableUserIds(parameters);
    return new NotificationTargetPage(
        List.copyOf(userIds),
        rowsByUserId.entrySet().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, entry -> entry.getValue().toInput(entry.getKey()))),
        List.copyOf(sendableUserIds));
  }

  /** 현재 발송 가능한 설치가 하나 이상 있는 사용자를 한 번에 조회한다. */
  private List<Long> findSendableUserIds(MapSqlParameterSource parameters) {
    return jdbcTemplate.query(
        """
        select distinct user_profile_id
        from user_push_token
        where user_profile_id in (:userIds)
          and push_enabled = true
          and token is not null
          and status = 'ACTIVE'
        """,
        parameters,
        (resultSet, rowNumber) -> resultSet.getLong("user_profile_id"));
  }

  /** 현재 사용자 언어에서 보이는 시나리오와 진행도를 한 번에 조회한다. */
  private void loadScenarioRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select up.id as user_profile_id, c.id as category_id, c.status as category_status,
               s.id as scenario_id, s.display_order as scenario_display_order,
               s.status as scenario_status, slv.status as variant_status, usp.status as progress_status
        from user_profile up
        join category c on 1 = 1
        join scenario s on s.category_id = c.id
        join scenario_language_variant slv on slv.scenario_id = s.id
          and slv.target_locale = up.target_locale and slv.base_locale = up.base_locale
        left join user_scenario_progress usp on usp.user_profile_id = up.id
          and usp.scenario_id = s.id and usp.target_locale = up.target_locale
        where up.id in (:userIds)
        order by up.id, c.display_order, s.display_order, s.id
        """,
        parameters,
        (RowCallbackHandler) resultSet -> addScenarioRow(resultSet, rowsByUserId));
  }

  /** 활성 표현과 부모 시나리오 완료 상태를 한 번에 조회한다. */
  private void loadExpressionRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select up.id as user_profile_id, s.id as scenario_id, s.display_order as scenario_display_order,
               we.id as expression_id, we.display_order as expression_display_order,
               usp.status as parent_progress_status, uwec.id as completion_id
        from user_profile up
        join writing_expression we on we.target_locale = up.target_locale
          and we.base_locale = up.base_locale and we.status = 'ACTIVE'
        join scenario s on s.id = we.scenario_id and s.status = 'ACTIVE'
        join category c on c.id = s.category_id and c.status = 'ACTIVE'
        left join user_scenario_progress usp on usp.user_profile_id = up.id
          and usp.scenario_id = s.id and usp.target_locale = up.target_locale
        left join user_writing_expression_completion uwec on uwec.user_profile_id = up.id
          and uwec.writing_expression_id = we.id
        where up.id in (:userIds)
        order by up.id, s.display_order, s.id, we.display_order, we.id
        """,
        parameters,
        (RowCallbackHandler) resultSet -> addExpressionRow(resultSet, rowsByUserId));
  }

  /** 마지막 실제 시나리오 완료 시각을 현재 콘텐츠와 무관하게 일괄 조회한다. */
  private void loadLatestScenarioCompletionRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select user_profile_id, scenario_id, last_cleared_at
        from user_scenario_progress
        where user_profile_id in (:userIds) and last_cleared_at is not null
        """,
        parameters,
        (RowCallbackHandler) resultSet -> addLatestScenarioCompletionRow(resultSet, rowsByUserId));
  }

  /** 마지막 실제 표현 완료 시각을 현재 콘텐츠와 무관하게 일괄 조회한다. */
  private void loadLatestExpressionCompletionRows(
      MapSqlParameterSource parameters, Map<Long, UserTargetRows> rowsByUserId) {
    jdbcTemplate.query(
        """
        select user_profile_id, scenario_id, last_completed_at
        from user_writing_expression_completion
        where user_profile_id in (:userIds)
        """,
        parameters,
        (RowCallbackHandler)
            resultSet -> addLatestExpressionCompletionRow(resultSet, rowsByUserId));
  }

  private void addScenarioRow(ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId)
      throws SQLException {
    long userProfileId = resultSet.getLong("user_profile_id");
    rowsByUserId
        .get(userProfileId)
        .scenarios
        .add(
            new ScenarioNotificationCandidate(
                userProfileId,
                resultSet.getLong("category_id"),
                "ACTIVE".equals(resultSet.getString("category_status")),
                resultSet.getLong("scenario_id"),
                resultSet.getInt("scenario_display_order"),
                "ACTIVE".equals(resultSet.getString("scenario_status")),
                "ACTIVE".equals(resultSet.getString("variant_status")),
                UserScenarioProgressStatus.CLEARED
                    .name()
                    .equals(resultSet.getString("progress_status"))));
  }

  private void addExpressionRow(ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId)
      throws SQLException {
    long userProfileId = resultSet.getLong("user_profile_id");
    rowsByUserId
        .get(userProfileId)
        .expressions
        .add(
            new ExpressionNotificationCandidate(
                userProfileId,
                resultSet.getLong("scenario_id"),
                resultSet.getInt("scenario_display_order"),
                resultSet.getLong("expression_id"),
                resultSet.getInt("expression_display_order"),
                UserScenarioProgressStatus.CLEARED
                    .name()
                    .equals(resultSet.getString("parent_progress_status")),
                resultSet.getObject("completion_id") != null));
  }

  private void addLatestScenarioCompletionRow(
      ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId) throws SQLException {
    UserTargetRows rows = rowsByUserId.get(resultSet.getLong("user_profile_id"));
    LocalDateTime completedAt = resultSet.getTimestamp("last_cleared_at").toLocalDateTime();
    if (rows.lastScenarioCompletedAt == null || completedAt.isAfter(rows.lastScenarioCompletedAt)) {
      rows.lastScenarioCompletedAt = completedAt;
      rows.lastScenarioId = resultSet.getLong("scenario_id");
    }
  }

  private void addLatestExpressionCompletionRow(
      ResultSet resultSet, Map<Long, UserTargetRows> rowsByUserId) throws SQLException {
    UserTargetRows rows = rowsByUserId.get(resultSet.getLong("user_profile_id"));
    LocalDateTime completedAt = resultSet.getTimestamp("last_completed_at").toLocalDateTime();
    if (rows.lastExpressionCompletedAt == null
        || completedAt.isAfter(rows.lastExpressionCompletedAt)) {
      rows.lastExpressionCompletedAt = completedAt;
      rows.lastExpressionScenarioId = resultSet.getLong("scenario_id");
    }
  }

  private static class UserTargetRows {
    private final List<ScenarioNotificationCandidate> scenarios = new ArrayList<>();
    private final List<ExpressionNotificationCandidate> expressions = new ArrayList<>();
    private LocalDateTime lastScenarioCompletedAt;
    private Long lastScenarioId;
    private LocalDateTime lastExpressionCompletedAt;
    private Long lastExpressionScenarioId;

    private NotificationTargetSelectionInput toInput(Long userProfileId) {
      return new NotificationTargetSelectionInput(
          userProfileId,
          lastScenarioCompletedAt,
          lastScenarioId,
          lastExpressionCompletedAt,
          lastExpressionScenarioId,
          List.copyOf(scenarios),
          List.copyOf(expressions));
    }
  }
}
