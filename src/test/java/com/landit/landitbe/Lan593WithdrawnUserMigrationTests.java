// 기존 탈퇴 회원 소급 정리가 실제 Flyway 스키마에서 보존 범위와 재실행 안전성을 지키는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class Lan593WithdrawnUserMigrationTests {
  private static final String MIGRATION = "db/migration/V133__sanitize_withdrawn_user_data.sql";
  private static final List<String> TABLES =
      List.of(
          "user_profile",
          "oauth_identity",
          "refresh_token",
          "apple_user_migration",
          "user_push_token",
          "push_delivery",
          "mailbox_feedback",
          "mailbox_feedback_attachment",
          "mailbox_letter",
          "mailbox_letter_recipient",
          "subscription_event",
          "learning_session",
          "free_talk_session",
          "session_history",
          "session_history_message",
          "conversation_memory",
          "conversation_memory_source",
          "free_talk_memory_retrieval");
  private static final List<String> RETAINED_TABLES =
      List.of(
          "push_delivery",
          "mailbox_feedback",
          "mailbox_feedback_attachment",
          "mailbox_letter",
          "mailbox_letter_recipient",
          "subscription_event",
          "learning_session",
          "free_talk_session",
          "session_history",
          "session_history_message");

  @DisplayName("기존 탈퇴 회원만 정리하고 재가입 계정·이전된 기기·문의·거래 이력을 보존한다.")
  @Test
  void upgradesWithdrawnUsersAndIsIdempotent() throws Exception {
    try (Connection connection = openDatabase()) {
      var dataSource = new SingleConnectionDataSource(connection, true);
      migrate(dataSource, "129");
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("db/lan593/withdrawn_user_fixtures.sql"));
      var jdbc = new JdbcTemplate(dataSource);
      final var retained = snapshot(jdbc, RETAINED_TABLES);
      final var profileLinks = profileLinks(jdbc);
      final var activeProfile = jdbc.queryForMap("SELECT * FROM user_profile WHERE id = 90002");
      final var activeIdentities =
          jdbc.queryForList(
              "SELECT * FROM oauth_identity WHERE user_profile_id = 90002 ORDER BY id");
      final var transferredDevice =
          jdbc.queryForMap("SELECT * FROM user_push_token WHERE id = 90003");
      final var activeMigration =
          jdbc.queryForMap("SELECT * FROM apple_user_migration WHERE oauth_identity_id = 90004");

      migrate(dataSource, "133");

      assertSanitized(jdbc);
      assertThat(profileLinks(jdbc)).isEqualTo(profileLinks);
      assertThat(snapshot(jdbc, RETAINED_TABLES)).isEqualTo(retained);
      assertThat(jdbc.queryForMap("SELECT * FROM user_profile WHERE id = 90002"))
          .isEqualTo(activeProfile);
      assertThat(
              jdbc.queryForList(
                  "SELECT * FROM oauth_identity WHERE user_profile_id = 90002 ORDER BY id"))
          .isEqualTo(activeIdentities);
      assertThat(jdbc.queryForMap("SELECT * FROM user_push_token WHERE id = 90003"))
          .isEqualTo(transferredDevice);
      assertThat(
              jdbc.queryForMap(
                  "SELECT * FROM apple_user_migration WHERE oauth_identity_id = 90004"))
          .isEqualTo(activeMigration);
      var sanitized = snapshot(jdbc, TABLES);
      ScriptUtils.executeSqlScript(connection, new ClassPathResource(MIGRATION));
      assertThat(snapshot(jdbc, TABLES)).isEqualTo(sanitized);
    }
  }

  @DisplayName("신규 DB에서도 V133이 Apple 반복 마이그레이션보다 먼저 실행될 수 있다.")
  @Test
  void freshDatabaseMigratesBeforeAppleRepeatable() throws Exception {
    try (Connection connection = openDatabase()) {
      var dataSource = new SingleConnectionDataSource(connection, true);
      migrate(dataSource, "133");
      var jdbc = new JdbcTemplate(dataSource);
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM apple_user_migration", Long.class))
          .isZero();
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_profile", Long.class)).isZero();
    }
  }

  private void assertSanitized(JdbcTemplate jdbc) {
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_profile", Long.class)).isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM user_profile WHERE status = 'WITHDRAWN'
                  AND nickname = '탈퇴한 사용자' AND email IS NULL AND profile_image_url IS NULL
                """,
                Long.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM oauth_identity WHERE user_profile_id IN (90001, 90003)
                  AND provider_user_id = 'withdrawn' AND provider_email IS NULL AND status = 'UNLINKED'
                """,
                Long.class))
        .isEqualTo(2);
    assertThat(jdbc.queryForList("SELECT user_profile_id FROM refresh_token", Long.class))
        .containsExactly(90002L);
    assertThat(jdbc.queryForList("SELECT oauth_identity_id FROM apple_user_migration", Long.class))
        .containsExactly(90004L);
    assertThat(
            jdbc.queryForList(
                "SELECT id FROM user_push_token WHERE status = 'REVOKED' ORDER BY id", Long.class))
        .containsExactly(90001L, 90002L, 90004L);
    assertThat(jdbc.queryForList("SELECT id FROM conversation_memory", Long.class))
        .containsExactly(90003L);
    assertThat(jdbc.queryForList("SELECT memory_id FROM conversation_memory_source", Long.class))
        .containsExactly(90003L);
    assertThat(jdbc.queryForList("SELECT id FROM free_talk_memory_retrieval", Long.class))
        .containsExactly(90003L);
  }

  private List<Map<String, Object>> profileLinks(JdbcTemplate jdbc) {
    return jdbc.queryForList(
        """
        SELECT id, status, created_at, subscription_status, subscription_product_id,
            subscription_store, subscription_event_at, subscription_expires_at
        FROM user_profile ORDER BY id
        """);
  }

  private Map<String, List<Map<String, Object>>> snapshot(JdbcTemplate jdbc, List<String> tables) {
    Map<String, List<Map<String, Object>>> snapshots = new LinkedHashMap<>();
    for (String table : tables) {
      snapshots.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1"));
    }
    return snapshots;
  }

  private Connection openDatabase() throws Exception {
    return DriverManager.getConnection(
        "jdbc:h2:mem:lan593_"
            + UUID.randomUUID()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "sa",
        "");
  }

  private void migrate(SingleConnectionDataSource dataSource, String version) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration", "classpath:db/h2")
        .target(version)
        .load()
        .migrate();
  }
}
