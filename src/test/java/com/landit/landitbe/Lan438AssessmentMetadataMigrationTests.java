// 온보딩 질문의 평가 요소만 갱신하고 질문과 번역을 보존하는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class Lan438AssessmentMetadataMigrationTests {
  @Test
  void refinesOnlyUneditedRoomKeyChoicesAndPreservesContent() throws Exception {
    try (var connection =
        DriverManager.getConnection("jdbc:h2:mem:lan438_room_key;MODE=PostgreSQL")) {
      var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
      jdbc.execute(
          "CREATE TABLE scenario_question (id BIGINT PRIMARY KEY, scenario_id BIGINT, "
              + "question_level_group VARCHAR(30), response_demand VARCHAR(10))");
      jdbc.execute(
          "CREATE TABLE scenario_question_language_variant (scenario_question_id BIGINT, "
              + "target_locale VARCHAR(5), base_locale VARCHAR(5), question_text TEXT, "
              + "question_translation TEXT, audio_url TEXT, required_response_element TEXT)");
      var questions =
          List.of(
              "No worries, it happens all the time. I can give you a temporary key, "
                  + "or someone can walk up and let you in. Which works better for you?",
              "For the replacement, we can have it ready tomorrow morning, "
                  + "or the day after in the afternoon. When would you like to pick it up?",
              "And we'll let you know when it's ready — would you rather get a text or an email?");
      for (int id = 61; id <= 64; id++) {
        String question = questions.get((id - 61) % 3);
        jdbc.update("INSERT INTO scenario_question VALUES (?, 21, 'LEVEL_4_TO_5', 'HIGH')", id);
        jdbc.update(
            "INSERT INTO scenario_question_language_variant VALUES "
                + "(?, 'EN', 'KR', ?, '기존 번역', 'existing-audio', ?)",
            id,
            question,
            question);
      }
      var script =
          new ClassPathResource("db/migration/V94__refine_room_key_assessment_metadata.sql");
      for (int attempt = 0; attempt < 2; attempt++) {
        ScriptUtils.executeSqlScript(connection, script);
      }
      assertThat(
              jdbc.queryForList(
                  "SELECT response_demand FROM scenario_question ORDER BY id", String.class))
          .containsExactly("LOW", "LOW", "LOW", "HIGH");
      assertThat(
              jdbc.queryForList(
                  "SELECT required_response_element FROM scenario_question_language_variant "
                      + "WHERE scenario_question_id < 64 ORDER BY scenario_question_id",
                  String.class))
          .containsExactly(
              "Choose one available temporary access option.",
              "State a preferred pickup time.",
              "Choose text or email for the notification.");
      assertThat(
              jdbc.queryForList(
                  "SELECT question_text FROM scenario_question_language_variant "
                      + "WHERE scenario_question_id < 64 ORDER BY scenario_question_id",
                  String.class))
          .containsExactlyElementsOf(questions);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM scenario_question_language_variant "
                      + "WHERE question_translation='기존 번역' AND audio_url='existing-audio'",
                  Integer.class))
          .isEqualTo(4);
      jdbc.update("UPDATE scenario_question SET response_demand='HIGH'");
      jdbc.update(
          "UPDATE scenario_question_language_variant "
              + "SET required_response_element=? WHERE scenario_question_id=61",
          "Curated requirement");
      jdbc.update(
          "UPDATE scenario_question_language_variant SET question_text='Changed question', "
              + "required_response_element='Changed question' WHERE scenario_question_id=62");
      jdbc.update("UPDATE scenario_question SET scenario_id=99 WHERE id=63");
      ScriptUtils.executeSqlScript(connection, script);
      assertThat(jdbc.queryForList("SELECT response_demand FROM scenario_question", String.class))
          .containsOnly("HIGH");
      assertThat(
              jdbc.queryForObject(
                  "SELECT required_response_element FROM scenario_question_language_variant "
                      + "WHERE scenario_question_id=61",
                  String.class))
          .isEqualTo("Curated requirement");
    }
  }

  @Test
  void registersSemanticRequirementsWithoutChangingQuestionContent() throws Exception {
    try (var connection =
        DriverManager.getConnection("jdbc:h2:mem:lan438_metadata;MODE=PostgreSQL")) {
      var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
      jdbc.execute(
          "CREATE TABLE scenario_question (id BIGINT PRIMARY KEY, scenario_id BIGINT, "
              + "response_demand VARCHAR(10))");
      jdbc.execute(
          "CREATE TABLE scenario_question_language_variant (scenario_question_id BIGINT, "
              + "target_locale VARCHAR(5), base_locale VARCHAR(5), question_text TEXT, "
              + "question_translation TEXT, required_response_element TEXT)");
      for (int id : new int[] {1, 2, 3, 121, 122, 123, 124, 125, 126, 999}) {
        jdbc.update("INSERT INTO scenario_question VALUES (?, ?, 'HIGH')", id, id == 999 ? 2 : 1);
        jdbc.update(
            "INSERT INTO scenario_question_language_variant VALUES "
                + "(?, 'EN', 'KR', 'Original question', '기존 번역', 'Original question')",
            id);
      }
      ScriptUtils.executeSqlScript(
          connection,
          new ClassPathResource("db/migration/V88__refine_onboarding_assessment_metadata.sql"));
      assertThat(
              jdbc.queryForObject(
                      "SELECT required_response_element FROM scenario_question_language_variant "
                          + "WHERE scenario_question_id=2",
                      String.class)
                  .lines()
                  .toList())
          .containsExactly(
              "State a hobby or leisure activity.", "Explain how you became interested in it.");
      assertThat(
              jdbc.queryForObject(
                  "SELECT required_response_element FROM scenario_question_language_variant "
                      + "WHERE scenario_question_id=3",
                  String.class))
          .isEqualTo("Recommend a first place to visit in Korea.");
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM scenario_question_language_variant "
                      + "WHERE required_response_element <> question_text",
                  Integer.class))
          .isEqualTo(9);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM scenario_question_language_variant "
                      + "WHERE question_text='Original question' AND question_translation='기존 번역'",
                  Integer.class))
          .isEqualTo(10);
      assertThat(
              jdbc.queryForList(
                  "SELECT response_demand FROM scenario_question WHERE scenario_id=1 ORDER BY id",
                  String.class))
          .containsExactly("MEDIUM", "HIGH", "LOW", "LOW", "LOW", "LOW", "MEDIUM", "MEDIUM", "LOW");
      assertThat(
              jdbc.queryForObject(
                  "SELECT response_demand FROM scenario_question WHERE id=3", String.class))
          .isEqualTo("LOW");
      assertThat(
              jdbc.queryForObject(
                  "SELECT response_demand FROM scenario_question WHERE id=999", String.class))
          .isEqualTo("HIGH");
    }
  }
}
