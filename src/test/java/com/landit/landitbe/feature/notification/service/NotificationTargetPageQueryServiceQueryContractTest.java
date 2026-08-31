// 예약 알림 대상 일괄 조회 SQL의 사용자별 행 제한 계약을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** 예약 알림 대상 일괄 조회 SQL의 사용자별 행 제한 계약을 검증한다. */
class NotificationTargetPageQueryServiceQueryContractTest {

  private static final long USER_ID = 1L;
  private static final LocalDate SCHEDULED_DATE = LocalDate.of(2026, 7, 30);

  /** 활동 조회는 사용자당 하나의 집계 행만 읽도록 SQL을 발행한다. */
  @Test
  void aggregatesActivityRowsPerUser() {
    NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
    when(
            jdbcTemplate.query(
                anyString(),
                any(MapSqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any()))
        .thenReturn(List.of(USER_ID), List.of());
    doNothing()
        .when(jdbcTemplate)
        .query(anyString(), any(MapSqlParameterSource.class), any(RowCallbackHandler.class));

    NotificationTargetPageQueryService queryService =
        new NotificationTargetPageQueryService(jdbcTemplate);
    queryService.loadPage(0L, 1, SCHEDULED_DATE);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate, atLeast(1))
        .query(
            sqlCaptor.capture(),
            any(MapSqlParameterSource.class),
            any(RowCallbackHandler.class));
    String activitySql =
        sqlCaptor.getAllValues().stream()
            .filter(sql -> sql.contains("user_daily_activity"))
            .findFirst()
            .orElseThrow();

    assertThat(activitySql.toLowerCase(Locale.ROOT))
        .contains("group by user_profile_id")
        .contains("max(case");
  }
}
