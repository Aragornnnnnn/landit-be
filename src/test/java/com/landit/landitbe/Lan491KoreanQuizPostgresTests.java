// 폐기 가능한 PostgreSQL에서 LAN-491의 실제 Flyway 적용과 전체 롤백을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 전용 로컬 DB가 설정된 경우 3,000개 표현을 매번 격리한 스키마에서 검증한다. */
@EnabledIfEnvironmentVariable(named = "LAN491_TEST_POSTGRES_URL", matches = ".+")
class Lan491KoreanQuizPostgresTests {

  private static final String URL = System.getenv("LAN491_TEST_POSTGRES_URL");
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final long OUTSIDE_ID = 900001L;

  @TempDir Path migrationDirectory;

  private final String schema = "lan491_" + UUID.randomUUID().toString().replace("-", "");
  private final Map<Long, ArrayNode> originalPayloads = new HashMap<>();
  private final Map<Long, String> originalSources = new HashMap<>();
  private Connection connection;
  private JsonNode manifest;
  private Flyway flyway;

  @BeforeEach
  void setUpIsolatedSchema() throws Exception {
    assertThat(URL).isEqualTo("jdbc:postgresql://127.0.0.1:55491/lan491_test?user=lan491_test");
    connection = DriverManager.getConnection(URL);
    execute("CREATE SCHEMA " + schema);
    connection.setSchema(schema);
    execute(
        "CREATE TABLE writing_expression (id BIGINT PRIMARY KEY, expression_source TEXT, "
            + "practice_examples_payload JSONB, updated_at TIMESTAMPTZ, untouched_marker TEXT)");
    manifest = Lan491KoreanQuizMigrationTests.readManifest();
    insertOriginalPayloads();
    Files.writeString(
        migrationDirectory.resolve("V106__add_korean_quiz_accepted_answers.sql"),
        Lan491KoreanQuizMigrationTests.readSql());
    flyway =
        Flyway.configure()
            .dataSource(URL, "lan491_test", "")
            .schemas(schema)
            .defaultSchema(schema)
            .locations("filesystem:" + migrationDirectory)
            .baselineOnMigrate(true)
            .baselineVersion("105")
            .load();
  }

  @AfterEach
  void cleanUpIsolatedSchema() throws Exception {
    if (connection != null) {
      try {
        execute("DROP SCHEMA " + schema + " CASCADE");
      } finally {
        connection.close();
      }
    }
  }

  @Test
  void migratesEveryReviewedAnswerPreservesOtherFieldsAndSkipsCompletedVersion() throws Exception {
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    Map<Long, ArrayNode> expected = expectedPayloads();
    int rows = 0;
    try (var statement = connection.createStatement();
        var results = statement.executeQuery("SELECT * FROM writing_expression ORDER BY id")) {
      while (results.next()) {
        long id = results.getLong("id");
        assertThat(MAPPER.readTree(results.getString("practice_examples_payload")))
            .as("expression %s", id)
            .isEqualTo(expected.get(id));
        assertThat(results.getString("untouched_marker")).isEqualTo("keep-" + id);
        assertThat(results.getString("expression_source")).isEqualTo(originalSources.get(id));
        if (id == OUTSIDE_ID) {
          assertThat(results.getTimestamp("updated_at").toInstant().toString())
              .isEqualTo("2026-01-01T00:00:00Z");
        } else {
          assertThat(results.getTimestamp("updated_at").toInstant())
              .isAfter(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        }
        rows++;
      }
    }
    assertThat(rows).isEqualTo(3001);
    String appliedSnapshot = snapshot();
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(snapshot()).isEqualTo(appliedSnapshot);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing",
        "order",
        "canonical",
        "choices",
        "question",
        "source",
        "existingAnswers",
        "malformed",
        "extraExample"
      })
  void rejectsSourceDriftWithoutChangingAnyExpression(String fault) throws Exception {
    execute(sourceFault(fault));
    String before = snapshot();
    assertThatThrownBy(() -> flyway.migrate())
        .isInstanceOf(FlywayException.class)
        .hasStackTraceContaining("LAN-491 source precondition failed: expression 1, example 1");
    assertRolledBack(before);
  }

  @ParameterizedTest
  @ValueSource(strings = {"sentenceWords", "sentenceTranslateAcceptedAnswers"})
  void rollsBackAllUpdatesWhenPostconditionsFail(String corruptedField) throws Exception {
    execute(
        "CREATE FUNCTION corrupt_payload() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN "
            + "IF NEW.id = 1 THEN NEW.practice_examples_payload = "
            + "jsonb_set(NEW.practice_examples_payload, '{0,"
            + corruptedField
            + "}', '[\"changed\"]'); "
            + "END IF; RETURN NEW; END $$");
    execute(
        "CREATE TRIGGER corrupt_payload BEFORE UPDATE ON writing_expression "
            + "FOR EACH ROW EXECUTE FUNCTION corrupt_payload()");
    String before = snapshot();
    String message =
        corruptedField.equals("sentenceWords")
            ? "LAN-491 unrelated payload fields changed"
            : "LAN-491 answer postcondition failed";
    assertThatThrownBy(() -> flyway.migrate())
        .isInstanceOf(FlywayException.class)
        .hasStackTraceContaining(message);
    assertRolledBack(before);
  }

  private void insertOriginalPayloads() throws Exception {
    for (JsonNode mapping : manifest) {
      long id = mapping.path("expressionId").asLong();
      ObjectNode example = mapping.path("expectedExample").deepCopy();
      example.put("imageUrl", "https://fixture.invalid/" + id);
      example.putArray("sentenceWords").add("keep").add("English");
      example.putArray("sentenceWordChoices").add("English").add("keep").add("extra");
      example.putObject("futureField").put("keep", true);
      ArrayNode payload = originalPayloads.computeIfAbsent(id, ignored -> MAPPER.createArrayNode());
      payload.add(example);
      originalSources.put(id, mapping.path("expressionSource").asText());
    }
    originalPayloads
        .values()
        .forEach(
            payload -> {
              payload
                  .addObject()
                  .put("exampleNumber", 3)
                  .putArray("sentenceTranslateWordChoices")
                  .add("정답")
                  .add("오답1")
                  .add("오답2")
                  .add("오답3")
                  .add("오답4");
              payload.addObject().put("exampleNumber", 4).put("imageUrl", "keep-fourth-image");
            });
    originalPayloads.put(OUTSIDE_ID, MAPPER.createArrayNode().add("untouched"));
    originalSources.put(OUTSIDE_ID, "FREE_TALK");
    connection.setAutoCommit(false);
    try (var insert =
        connection.prepareStatement(
            "INSERT INTO writing_expression VALUES "
                + "(?, ?, ?::jsonb, '2026-01-01T00:00:00Z', ?)")) {
      for (var entry : originalPayloads.entrySet()) {
        insert.setLong(1, entry.getKey());
        insert.setString(2, originalSources.get(entry.getKey()));
        insert.setString(3, entry.getValue().toString());
        insert.setString(4, "keep-" + entry.getKey());
        insert.addBatch();
      }
      insert.executeBatch();
    }
    connection.commit();
    connection.setAutoCommit(true);
  }

  private Map<Long, ArrayNode> expectedPayloads() {
    Map<Long, ArrayNode> expected = new HashMap<>();
    originalPayloads.forEach((id, payload) -> expected.put(id, payload.deepCopy()));
    for (JsonNode mapping : manifest) {
      ObjectNode example =
          (ObjectNode)
              expected
                  .get(mapping.path("expressionId").asLong())
                  .get(mapping.path("practiceExampleNumber").asInt() - 1);
      example.set("sentenceTranslateAcceptedAnswers", mapping.get("acceptedAnswers"));
      example.set("sentenceTranslateWordChoices", mapping.get("wordChoices"));
    }
    return expected;
  }

  private String sourceFault(String fault) {
    String payload = "practice_examples_payload";
    String update = "UPDATE writing_expression SET " + payload + " = ";
    String value =
        switch (fault) {
          case "missing" -> null;
          case "order" ->
              "jsonb_set(jsonb_set("
                  + payload
                  + ", '{0}', "
                  + payload
                  + "->1), '{1}', "
                  + payload
                  + "->0)";
          case "canonical" ->
              "jsonb_set("
                  + payload
                  + ", '{0,sentenceTranslateWords}', "
                  + "'[\"날엔\",\"더운\",\"시원한\",\"맥주만\",\"한\",\"게\",\"없지\"]')";
          case "choices" ->
              "jsonb_set("
                  + payload
                  + ", '{0,sentenceTranslateWordChoices}', "
                  + "'[\"게\",\"맥주만\",\"없지\",\"추운\",\"날엔\",\"더운\",\"시원한\",\"커피만\",\"한\",\"밤엔\"]')";
          case "question" -> "jsonb_set(" + payload + ", '{0,practiceQuestion}', '\"changed\"')";
          case "source" -> null;
          case "existingAnswers" ->
              "jsonb_set(" + payload + ", '{0,sentenceTranslateAcceptedAnswers}', '[]')";
          case "malformed" -> "'{}'::jsonb";
          case "extraExample" -> payload + " || '[{}]'::jsonb";
          default -> throw new IllegalArgumentException(fault);
        };
    if (fault.equals("missing")) {
      return "DELETE FROM writing_expression WHERE id = 1";
    }
    if (fault.equals("source")) {
      return "UPDATE writing_expression SET expression_source = 'FREE_TALK' WHERE id = 1";
    }
    return update + value + " WHERE id = 1";
  }

  private void assertRolledBack(String before) throws Exception {
    assertThat(snapshot()).isEqualTo(before);
    try (var statement = connection.createStatement();
        var rows =
            statement.executeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '106'")) {
      rows.next();
      assertThat(rows.getInt(1)).isZero();
    }
  }

  private String snapshot() throws Exception {
    try (var statement = connection.createStatement();
        var rows =
            statement.executeQuery(
                "SELECT md5(string_agg(row_to_json(expression)::text, "
                    + "'' ORDER BY id)) FROM writing_expression expression")) {
      rows.next();
      return rows.getString(1);
    }
  }

  private void execute(String sql) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
