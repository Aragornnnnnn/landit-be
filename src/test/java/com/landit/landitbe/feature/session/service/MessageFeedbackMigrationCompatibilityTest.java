// main 선배포와 후속 버전 마이그레이션이 피드백 기록을 유지하는지 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

class MessageFeedbackMigrationCompatibilityTest {
  @TempDir Path migrations;

  @Test
  void repeatableBootstrapDoesNotAdvanceVersionsOrReplaceExistingFeedback() throws Exception {
    String url = "jdbc:h2:mem:compat_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    Files.writeString(
        migrations.resolve("V1__parents.sql"),
        "CREATE TABLE learning_session(id BIGINT PRIMARY KEY);"
            + "CREATE TABLE session_history_message(id BIGINT PRIMARY KEY);");
    String bootstrap =
        new ClassPathResource("db/migration/R__ensure_message_feedback_work.sql")
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    Files.writeString(migrations.resolve("R__ensure_message_feedback_work.sql"), bootstrap);
    Flyway flyway =
        Flyway.configure().dataSource(url, "sa", "").locations("filesystem:" + migrations).load();
    flyway.migrate();
    assertThat(flyway.info().current().getVersion().toString()).isEqualTo("1");
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement()) {
      statement.execute("INSERT INTO learning_session VALUES(1)");
      statement.execute("INSERT INTO session_history_message VALUES(2)");
      statement.execute(
          "INSERT INTO message_feedback_work(message_id,session_id,request_payload,"
              + "result_payload,available_at) VALUES(2,1,'request','saved',CURRENT_TIMESTAMP)");
      // 후속 V100과 동일한 IF NOT EXISTS 초기화도 저장된 결과를 유지해야 한다.
      Files.writeString(migrations.resolve("V2__future_release.sql"), bootstrap);
      flyway.migrate();
      assertThat(flyway.info().current().getVersion().toString()).isEqualTo("2");
      try (var rows = statement.executeQuery("SELECT result_payload FROM message_feedback_work")) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getString(1)).isEqualTo("saved");
        assertThat(rows.next()).isFalse();
      }
      assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
  }
}
