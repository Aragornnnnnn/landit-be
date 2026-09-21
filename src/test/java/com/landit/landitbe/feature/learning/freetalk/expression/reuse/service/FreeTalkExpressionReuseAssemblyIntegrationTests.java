// 재사용 기록의 출처 제목을 실제 시나리오·스몰톡 세션에서 읽어 오고, 만든 기록이 그대로 저장되는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkUsedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.repository.FreeTalkExpressionReuseRepository;
import com.landit.landitbe.shared.domain.Locale;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 재사용 기록의 출처 제목을 실제 시나리오·스몰톡 세션에서 읽어 오고, 만든 기록이 그대로 저장되는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class FreeTalkExpressionReuseAssemblyIntegrationTests {

  private static final long USER_ID = 996001L;
  private static final long OTHER_USER_ID = 996002L;
  private static final long CATEGORY_ID = 996010L;
  private static final long SCENARIO_ID = 996020L;
  private static final long SCENARIO_EXPRESSION_ID = 996101L;
  private static final long FREE_TALK_EXPRESSION_ID = 996102L;
  private static final long UNTITLED_EXPRESSION_ID = 996103L;
  // 기준 ID에서 학습 세션, +1 프리톡 세션, +2 대화 기록, +3 사용자 발화를 만든다.
  private static final long CURRENT_SESSION = 996200L;
  private static final long FIRST_LEARNED_SESSION = 996300L;
  private static final long LATER_LEARNED_SESSION = 996400L;
  private static final long NOT_LEARNED_SESSION = 996500L;
  private static final long OTHER_USER_SESSION = 996600L;
  private static final long UNTITLED_SESSION = 996700L;
  private static final List<Long> SESSIONS =
      List.of(
          CURRENT_SESSION,
          FIRST_LEARNED_SESSION,
          LATER_LEARNED_SESSION,
          NOT_LEARNED_SESSION,
          OTHER_USER_SESSION,
          UNTITLED_SESSION);
  private static final String CONTENT = "Let's grab a coffee. It is up to you. No worries at all.";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FreeTalkExpressionReuseAssemblyService service;

  @Autowired private FreeTalkExpressionReuseRepository reuseRepository;

  @BeforeEach
  void seed() {
    seedUser(USER_ID);
    seedUser(OTHER_USER_ID);
    seedScenario();
    seedExpression(SCENARIO_EXPRESSION_ID);
    seedExpression(FREE_TALK_EXPRESSION_ID);
    seedExpression(UNTITLED_EXPRESSION_ID);
    seedSession(CURRENT_SESSION, USER_ID, "오늘의 대화");
    seedSession(FIRST_LEARNED_SESSION, USER_ID, "카페 이야기");
    seedSession(LATER_LEARNED_SESSION, USER_ID, "나중에 또 배운 세션");
    seedSession(NOT_LEARNED_SESSION, USER_ID, "추천만 받고 안 배운 세션");
    seedSession(OTHER_USER_SESSION, OTHER_USER_ID, "다른 사용자의 세션");
    seedSession(UNTITLED_SESSION, USER_ID, null);
    // 추천만 받은 세션이 가장 먼저지만 학습을 마치지 않았다. 다른 사용자는 더 일찍 마쳤다.
    linkExpression(NOT_LEARNED_SESSION, FREE_TALK_EXPRESSION_ID, null);
    linkExpression(OTHER_USER_SESSION, FREE_TALK_EXPRESSION_ID, LocalDateTime.of(2026, 9, 1, 9, 0));
    linkExpression(
        LATER_LEARNED_SESSION, FREE_TALK_EXPRESSION_ID, LocalDateTime.of(2026, 9, 12, 9, 0));
    linkExpression(
        FIRST_LEARNED_SESSION, FREE_TALK_EXPRESSION_ID, LocalDateTime.of(2026, 9, 10, 9, 0));
    linkExpression(UNTITLED_SESSION, UNTITLED_EXPRESSION_ID, LocalDateTime.of(2026, 9, 11, 9, 0));
  }

  @AfterEach
  void clearFixtures() {
    jdbcTemplate.update(
        "delete from free_talk_expression_reuse where user_profile_id in (?, ?)",
        USER_ID,
        OTHER_USER_ID);
    for (long baseId : SESSIONS) {
      jdbcTemplate.update(
          "delete from free_talk_session_expression where free_talk_session_id = ?", baseId + 1);
      jdbcTemplate.update("delete from free_talk_session where id = ?", baseId + 1);
      jdbcTemplate.update("delete from session_history_message where id = ?", baseId + 3);
      jdbcTemplate.update("delete from session_history where id = ?", baseId + 2);
      jdbcTemplate.update("delete from learning_session where id = ?", baseId);
    }
    jdbcTemplate.update("delete from writing_expression where id between 996101 and 996103");
    jdbcTemplate.update("delete from scenario_language_variant where scenario_id = ?", SCENARIO_ID);
    jdbcTemplate.update("delete from scenario where id = ?", SCENARIO_ID);
    jdbcTemplate.update("delete from category_language_variant where category_id = ?", CATEGORY_ID);
    jdbcTemplate.update("delete from category where id = ?", CATEGORY_ID);
    jdbcTemplate.update("delete from user_profile where id in (?, ?)", USER_ID, OTHER_USER_ID);
  }

  @DisplayName("시나리오 출처는 기준 언어의 시나리오 제목을, 스몰톡 출처는 본인이 처음 학습을 마친 세션의 제목을 담아 저장된다.")
  @Test
  void storesReusesWithSourceTitlesReadFromScenarioAndFreeTalkSession() {
    List<FreeTalkExpressionReuse> reuses =
        service.assemble(
            USER_ID,
            CURRENT_SESSION + 1,
            Locale.EN,
            Locale.KR,
            List.of(
                new AiConversationHistoryMessage(CURRENT_SESSION + 3, 1, "USER", CONTENT, null)),
            List.of(
                learned(
                    SCENARIO_EXPRESSION_ID, FreeTalkExpressionReuseSource.SCENARIO, SCENARIO_ID),
                learned(FREE_TALK_EXPRESSION_ID, FreeTalkExpressionReuseSource.FREE_TALK, null),
                learned(UNTITLED_EXPRESSION_ID, FreeTalkExpressionReuseSource.FREE_TALK, null)),
            List.of(
                new AiFreeTalkUsedExpression(
                    SCENARIO_EXPRESSION_ID, CURRENT_SESSION + 3, "grab a coffee"),
                new AiFreeTalkUsedExpression(
                    FREE_TALK_EXPRESSION_ID, CURRENT_SESSION + 3, "up to you"),
                new AiFreeTalkUsedExpression(
                    UNTITLED_EXPRESSION_ID, CURRENT_SESSION + 3, "No worries")));

    reuseRepository.saveAll(reuses);

    assertThat(
            jdbcTemplate.queryForList(
                "select source_title from free_talk_expression_reuse "
                    + "where free_talk_session_id = ? order by id",
                String.class,
                CURRENT_SESSION + 1))
        .containsExactly("주말 계획", "카페 이야기", null);
    assertThat(
            jdbcTemplate.queryForList(
                "select quoted_sentence from free_talk_expression_reuse "
                    + "where free_talk_session_id = ? order by id",
                String.class,
                CURRENT_SESSION + 1))
        .containsExactly("Let's grab a coffee.", "It is up to you.", "No worries at all.");
  }

  private static FreeTalkLearnedExpression learned(
      long expressionId, FreeTalkExpressionReuseSource sourceType, Long scenarioId) {
    return new FreeTalkLearnedExpression(
        expressionId,
        "expression " + expressionId,
        "뜻",
        sourceType,
        scenarioId,
        LocalDate.of(2026, 9, 10));
  }

  private void seedUser(long userId) {
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at)
        values (?, ?, 'EN', 'KR', 1, ?, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        "reuse-" + userId,
        aiTutorId());
  }

  // 같은 시나리오에 다른 기준 언어의 제목도 둬서 언어 조합으로 고르는지 본다.
  private void seedScenario() {
    jdbcTemplate.update(
        """
        insert into category (id, display_order, status, created_at, updated_at)
        values (?, 9960001, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        CATEGORY_ID);
    jdbcTemplate.update(
        """
        insert into scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            display_order, status, created_at, updated_at)
        values (?, ?, 'tutor', 'EASY', 'USER', 3, 9960001, 'INACTIVE',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        SCENARIO_ID,
        CATEGORY_ID);
    for (String[] variant : List.of(new String[] {"KR", "주말 계획"}, new String[] {"JP", "週末の予定"})) {
      jdbcTemplate.update(
          """
          insert into scenario_language_variant (
              scenario_id, target_locale, base_locale, title, briefing,
              user_opening_instruction, conversation_goal, status, created_at, updated_at)
          values (?, 'EN', ?, ?, '설명', '시작', '목표', 'INACTIVE',
              CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """,
          SCENARIO_ID,
          variant[0],
          variant[1]);
    }
  }

  private void seedSession(long baseId, long userId, String title) {
    jdbcTemplate.update(
        """
        insert into learning_session (
            id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
            input_mode, status, ended_by, completion_reason, started_at, ended_at,
            created_at, updated_at)
        values (?, ?, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', 'COMPLETED', 'USER', 'USER_ENDED',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        baseId,
        userId,
        aiTutorId());
    jdbcTemplate.update(
        """
        insert into free_talk_session (
            id, learning_session_id, start_mode, character_id, title, conversation_status,
            accumulated_speaking_duration_ms, memory_generation_status,
            memory_generation_started_at, created_at, updated_at)
        values (?, ?, 'USER_FIRST', 'chloe', ?, 'COMPLETED', 0, 'READY',
            NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        baseId + 1,
        baseId,
        title);
    jdbcTemplate.update(
        """
        insert into session_history (
            id, learning_session_id, user_profile_id, session_type, target_locale,
            base_locale, started_at, ended_at, duration_seconds, user_message_count, created_at)
        values (?, ?, ?, 'FREE_TALK', 'EN', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            0, 1, CURRENT_TIMESTAMP)
        """,
        baseId + 2,
        baseId,
        userId);
    jdbcTemplate.update(
        """
        insert into session_history_message (
            id, session_history_id, message_sequence, turn_number, role, content,
            input_type, created_at, updated_at)
        values (?, ?, 1, 1, 'USER', ?, 'TEXT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        baseId + 3,
        baseId + 2,
        CONTENT);
  }

  private void linkExpression(long baseId, long expressionId, LocalDateTime completedAt) {
    jdbcTemplate.update(
        """
        insert into free_talk_session_expression (
            free_talk_session_id, writing_expression_id, display_order, completed_at,
            created_at, updated_at)
        values (?, ?, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        baseId + 1,
        expressionId,
        completedAt == null ? null : Timestamp.valueOf(completedAt));
  }

  private void seedExpression(long id) {
    jdbcTemplate.update(
        """
        INSERT INTO writing_expression (
            id, scenario_id, expression_source, expression_type,
            usage_frequency_level, difficulty_level, target_locale, base_locale, display_order,
            target_expression_text, base_expression_meaning_text, usage_summary,
            usage_description, representative_sentence_text,
            representative_sentence_translation, representative_sentence_words,
            representative_sentence_word_choices, practice_examples_payload, status,
            created_at, updated_at
        )
        VALUES (
            ?, NULL, 'FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', 3, 'EN', 'KR', 1, ?, '뜻',
            '용법 요약', '용법 설명', 'Example sentence.', '예문 번역',
            ARRAY['Example'], ARRAY['Example'], '[]' FORMAT JSON, 'ACTIVE',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """,
        id,
        "expression " + id);
  }

  private Long aiTutorId() {
    return jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
  }
}
