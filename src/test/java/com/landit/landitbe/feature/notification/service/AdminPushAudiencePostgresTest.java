// 폐기 가능한 로컬 PostgreSQL에서 SQL 읽기 전용성과 실행 제한을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.notification.client.AdminPushAudienceJdbcClient;
import com.landit.landitbe.shared.exception.ApiException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.postgresql.Driver;

/** 전용 임시 DB URL이 주어진 경우에만 실제 PostgreSQL 계약을 확인한다. */
@EnabledIfEnvironmentVariable(named = "LAN462_TEST_POSTGRES_URL", matches = ".+")
class AdminPushAudiencePostgresTest {
  private static final String URL = System.getenv("LAN462_TEST_POSTGRES_URL");

  private static final String DATABASE = "lan462_" + UUID.randomUUID().toString().replace("-", "");
  private static final String READER = DATABASE + "_reader";
  private static boolean databaseCreated;
  private static boolean roleCreated;
  private static String fixtureUrl;

  private static Connection fixtureConnection() throws Exception {
    Properties properties = Driver.parseURL(URL, null);
    properties.setProperty("PGDBNAME", DATABASE);
    return DriverManager.getConnection("jdbc:postgresql://", properties);
  }

  @BeforeAll
  static void setup() throws Exception {
    try (Connection connection = DriverManager.getConnection(URL);
        var statement = connection.createStatement()) {
      statement.execute("create database " + DATABASE);
      databaseCreated = true;
      statement.execute("create role " + READER + " login password 'local-fixture-only'");
      roleCreated = true;
    }
    Properties source = Driver.parseURL(URL, null);
    source.setProperty("PGDBNAME", DATABASE);
    fixtureUrl =
        "jdbc:postgresql://?"
            + source.stringPropertyNames().stream()
                .sorted()
                .map(
                    key ->
                        URLEncoder.encode(key, StandardCharsets.UTF_8)
                            + "="
                            + URLEncoder.encode(source.getProperty(key), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    try (Connection connection = fixtureConnection();
        var statement = connection.createStatement()) {
      statement.execute("create table user_profile(id bigint primary key,status text)");
      statement.execute(
          "create table survey_responses(user_id bigint primary "
              + "key,answers jsonb not null,created_at timestamptz default now())");
      statement.execute(
          "insert into user_profile values " + "(1,'ACTIVE'),(2,'ACTIVE'),(3,'INACTIVE')");
      statement.execute("insert into survey_responses(user_id,answers) values (2,'{}')");
      statement.execute("create table query_numbers as select generate_series(1,100000) id");
      statement.execute(
          "create function attempt_write() returns bigint language sql "
              + "security definer as $$ insert into user_profile "
              + "values (99,'ACTIVE') returning id $$");
      statement.execute(
          "create view mutation_view as select " + "attempt_write() as user_profile_id");
      statement.execute("grant select on all tables in schema public to " + READER);
      // 테스트에서만 쓰기 권한을 부여해 트랜잭션 READ ONLY 자체의 차단을 분리한다.
      statement.execute("grant insert on user_profile to " + READER);
    }
  }

  @AfterAll
  static void cleanup() throws Exception {
    try (Connection connection = DriverManager.getConnection(URL);
        var statement = connection.createStatement()) {
      if (databaseCreated) {
        statement.execute("drop database " + DATABASE + " with (force)");
      }
      if (roleCreated) {
        statement.execute("drop role " + READER);
      }
    }
  }

  @Test
  void returnsOnlyActiveSurveyNonResponders() {
    assertThat(
            service(100000)
                .query(
                    "SELECT u.id AS user_profile_id FROM public.user_profile u "
                        + "WHERE u.status='ACTIVE' AND NOT EXISTS "
                        + "(SELECT 1 FROM public.survey_responses s WHERE s.user_id=u.id)"))
        .containsExactly(1L);
  }

  @Test
  void rejectsOverflowWrongColumnsAndNonIntegralIds() {
    assertThatThrownBy(() -> service(1).query("select id as user_profile_id from user_profile"))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> service(10).query("select 1.5 as user_profile_id"))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> service(10).query("select id from user_profile"))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> service(10).query("select 1 as user_profile_id, 'extra' as email"))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void readOnlyTransactionBlocksWritesHiddenBehindView() throws Exception {
    assertThatThrownBy(() -> service(10).query("select user_profile_id from mutation_view"))
        .isInstanceOf(ApiException.class);
    try (Connection connection = fixtureConnection();
        var statement = connection.createStatement();
        var result = statement.executeQuery("select count(*) from user_profile where id=99")) {
      result.next();
      assertThat(result.getLong(1)).isZero();
    }
  }

  @Test
  void stopsExpensiveReadAtStatementTimeout() {
    Instant start = Instant.now();
    assertThatThrownBy(
            () ->
                service(10)
                    .query(
                        "select count(*) as user_profile_id from "
                            + "query_numbers a cross join query_numbers b"))
        .isInstanceOf(ApiException.class);
    assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofSeconds(15));
  }

  private AdminPushAudienceSqlService service(int limit) {
    return new AdminPushAudienceSqlService(
        new AdminPushAudienceJdbcClient(fixtureUrl, READER, "local-fixture-only", limit));
  }
}
