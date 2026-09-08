// 관리자 SQL의 지원 문법과 위험한 문법 차단을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 실제 실행 전 SQL 경계와 설정 누락 시 차단을 확인한다. */
class AdminPushAudienceSqlServiceTest {
  @Test
  void acceptsSurveyNonRespondersAndReadOnlyCte() {
    assertThatCode(
            () ->
                AdminPushAudienceSqlService.validateSql(
                    "SELECT u.id AS user_profile_id FROM "
                        + "public.user_profile u WHERE u.status='ACTIVE' "
                        + "AND NOT EXISTS (SELECT 1 FROM "
                        + "public.survey_responses s WHERE s.user_id=u.id)"))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                AdminPushAudienceSqlService.validateSql(
                    "WITH audience AS (SELECT id FROM user_profile) SELECT "
                        + "id AS user_profile_id FROM audience"))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "delete from user_profile",
        "select id as user_profile_id from user_profile; delete from user_profile",
        "with removed as (delete from user_profile returning id) select id "
            + "as user_profile_id from removed",
        "select pg_sleep(30) as user_profile_id",
        "select set_config('statement_timeout','0',true) as user_profile_id",
        "select id into backup from user_profile",
        "select id from user_profile for update",
        "select public.custom_function() as user_profile_id",
        "select pg_catalog.pg_sleep(30) as user_profile_id",
        "select E'\\'x' || set_config('statement_timeout','0',true) || "
            + "pg_sleep(12) || 'y' as user_profile_id",
        "select $$hello$$ as user_profile_id",
        "select 1 /* comment */ as user_profile_id"
      })
  void rejectsUnsupportedAndMutatingSql(String sql) {
    assertThatThrownBy(() -> AdminPushAudienceSqlService.validateSql(sql))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void neverFallsBackToApplicationDatabase() {
    var service = new AdminPushAudienceSqlService("", "", "", 100000);
    assertThatThrownBy(() -> service.query("select 1 as user_profile_id"))
        .isInstanceOf(ApiException.class);
  }
}
