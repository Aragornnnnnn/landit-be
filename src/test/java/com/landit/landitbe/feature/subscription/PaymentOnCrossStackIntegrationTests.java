// 수정하지 않은 FE 훅과 실제 AI HTTP 서버를 연결해 결제 ON 경계를 검증한다.

package com.landit.landitbe.feature.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:payment_cross_stack;"
          + "MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
      "landit.subscription.launched-at=2026-09-01T00:00:00+09:00",
      "landit.subscription.revenuecat.webhook-authorization=local-payment-test",
      "landit.ai.client-mode=remote",
      "server.address=127.0.0.1"
    })
@EnabledIfEnvironmentVariable(named = "LAN474_FE_ROOT", matches = ".+")
class PaymentOnCrossStackIntegrationTests {
  @LocalServerPort private int port;
  @Autowired private JdbcTemplate jdbc;

  @DynamicPropertySource
  static void aiServer(DynamicPropertyRegistry registry) {
    registry.add("landit.ai.base-url", () -> System.getenv("LAN474_AI_URL"));
  }

  @Test
  void unchangedFrontendUsesRealBackendAndAi() throws Exception {
    seedContent();
    Path root = Path.of("").toAbsolutePath();
    String feRoot = System.getenv("LAN474_FE_ROOT");
    ProcessBuilder builder =
        new ProcessBuilder(
            "node",
            feRoot + "/apps/web/node_modules/vitest/vitest.mjs",
            "run",
            "--config",
            root + "/scripts/integration/payment-on.config.mjs");
    builder.environment().put("LAN474_BE_URL", "http://127.0.0.1:" + port);
    builder.environment().put("NEXT_PUBLIC_PAYMENT_ENABLED", "true");
    builder.redirectErrorStream(true);
    builder.redirectOutput(root.resolve("build/payment-on-fe.log").toFile());
    Process process = builder.start();
    try {
      assertThat(process.waitFor(120, TimeUnit.SECONDS)).as("FE integration deadline").isTrue();
      assertThat(process.exitValue()).as("See build/payment-on-fe.log").isZero();
    } finally {
      process.destroyForcibly();
    }
  }

  private void seedContent() {
    jdbc.update(
        """
        INSERT INTO category(id,display_order,status,created_at,updated_at)
        VALUES(900001,900001,'ACTIVE',NOW(),NOW())
        """);
    jdbc.update(
        """
        INSERT INTO category_language_variant(category_id,base_locale,name,created_at,
        updated_at)
        VALUES(900001,'KR','결제 통합 테스트',NOW(),NOW())
        """);
    for (long id : new long[] {900001, 900002}) {
      jdbc.update(
          """
          INSERT INTO scenario(id,category_id,ai_role,difficulty,first_speaker,
          total_question_count,display_order,status,character_id,created_at,updated_at)
          VALUES(?,900001,'friend','EASY','AI',2,?,'ACTIVE','chloe',NOW(),NOW())
          """,
          id,
          id);
      jdbc.update(
          """
          INSERT INTO scenario_language_variant(scenario_id,target_locale,base_locale,title,
          briefing,conversation_goal,status,created_at,updated_at)
          VALUES(?,'EN','KR','음식 이야기','좋아하는 음식 이야기','취향과 경험을 설명한다','ACTIVE',NOW(),NOW())
          """,
          id);
      int groupIndex = 0;
      for (String group : new String[] {"LEVEL_1", "LEVEL_2_TO_3", "LEVEL_4_TO_5", "DIAGNOSTIC"}) {
        for (int turn = 1; turn <= 2; turn++) {
          long questionId = id * 100 + groupIndex * 10 + turn;
          jdbc.update(
              """
              INSERT INTO scenario_question(id,scenario_id,display_order,question_level_group,
              status,created_at,updated_at)
              VALUES(?,?,?,?,'ACTIVE',NOW(),NOW())
              """,
              questionId,
              id,
              turn,
              group);
          jdbc.update(
              """
              INSERT INTO scenario_question_language_variant(scenario_question_id,
              target_locale,base_locale,question_text,question_translation,audio_url,status,
              created_at,updated_at)
              VALUES(?,'EN','KR',?,'음식 질문','https://example.com/test.mp3','ACTIVE',NOW(),NOW())
              """,
              questionId,
              turn == 1 ? "What food do you like, and why?" : "What did you eat recently?");
        }
        groupIndex++;
      }
    }
    String example =
        """
        {"sentenceText":"I like pizza.","highlightingPart":"I like",
         "sentenceTranslation":"피자를 좋아해요.","practiceQuestion":"What do you like?",
         "practiceQuestionTranslation":"뭘 좋아해요?",
         "sentenceWords":["I","like","pizza"],"sentenceWordChoices":["I","like","pizza","you"],
         "sentenceTranslateWords":["피자를","좋아해요"],
         "sentenceTranslateWordChoices":["피자를","좋아해요","나는"]}
        """;
    String examples = "[" + String.join(",", java.util.Collections.nCopies(4, example)) + "]";
    jdbc.update(
        """
        INSERT INTO writing_expression( id,scenario_id,expression_type,usage_frequency_level,
        difficulty_level,target_locale,base_locale,display_order,target_expression_text,
        base_expression_meaning_text,usage_summary,usage_description,
        representative_sentence_text,representative_sentence_translation,
        representative_sentence_words,representative_sentence_word_choices,
        practice_examples_payload,status,created_at,updated_at)
        VALUES(900001,900001,'DAILY_ROUTINE','BASIC',4,'EN','KR',1,'I like','좋아하다','취향',
        '취향을 말한다','I like pizza','피자를 좋아해요',ARRAY['I','like','pizza'],ARRAY['I','like','pizza',
        'you'],? FORMAT JSON,'ACTIVE',NOW(),NOW())
        """,
        examples);
  }
}
