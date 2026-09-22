// 배운 표현 후보가 실제 DB에서 본인 것만, 활성·언어가 맞는 표현만, 최근에 배운 순으로 읽히는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
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

/** 배운 표현 후보가 실제 DB에서 본인 것만, 활성·언어가 맞는 표현만, 최근에 배운 순으로 읽히는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class FreeTalkLearnedExpressionSelectionIntegrationTests {

  private static final long USER_ID = 995001L;
  private static final long OTHER_USER_ID = 995002L;
  private static final LocalDateTime LEARNED_AT = LocalDateTime.of(2026, 9, 10, 21, 4);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FreeTalkLearnedExpressionSelectionService service;

  private List<Long> scenarioExpressionIds;
  private long freeTalkExpressionId;
  private Long inactivatedExpressionId;

  @BeforeEach
  void seed() {
    for (long userId : new long[] {USER_ID, OTHER_USER_ID}) {
      jdbcTemplate.update(
          """
          insert into user_profile (
              id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
              push_permission_status, status, created_at, updated_at)
          values (?, 'expression-reuse-user', 'EN', 'KR', 1, (select min(id) from ai_tutor),
              'NOT_DETERMINED', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """,
          userId);
    }
    // 테스트 DB에는 표현 콘텐츠가 없어 직접 심는다. 완료 이력의 출처는 표현과 별개의 값이다.
    scenarioExpressionIds = List.of(995101L, 995102L, 995103L);
    freeTalkExpressionId = 995104L;
    // 시나리오 출처 표현은 시나리오 ID가 있어야 하므로(chk_writing_expression_scenario_source) 표현은 모두 스몰톡 출처로 심는다.
    seedExpression(995101L, "grab a coffee", "커피 한잔하다");
    seedExpression(995102L, "work out", "운동하다");
    seedExpression(995103L, "hit the gym", "헬스장에 가다");
    seedExpression(995104L, "piece of cake", "식은 죽 먹기");
  }

  @AfterEach
  void clear() {
    if (inactivatedExpressionId != null) {
      jdbcTemplate.update(
          "update writing_expression set status = 'ACTIVE' where id = ?", inactivatedExpressionId);
      inactivatedExpressionId = null;
    }
    jdbcTemplate.update(
        "delete from user_writing_expression_completion where user_profile_id in (?, ?)",
        USER_ID,
        OTHER_USER_ID);
    jdbcTemplate.update("delete from user_profile where id in (?, ?)", USER_ID, OTHER_USER_ID);
    jdbcTemplate.update("delete from writing_expression where id between 995101 and 995104");
  }

  @DisplayName("본인이 학습을 마친 표현만, 시나리오·스몰톡 출처를 가리지 않고 가장 최근에 마친 순으로 본문과 함께 읽는다.")
  @Test
  void readsOwnLearnedExpressionsMostRecentFirst() {
    long oldest = scenarioExpressionIds.get(0);
    long newest = scenarioExpressionIds.get(1);
    complete(USER_ID, oldest, "SCENARIO", LEARNED_AT, LEARNED_AT);
    complete(
        USER_ID, freeTalkExpressionId, "FREE_TALK", LEARNED_AT.plusDays(1), LEARNED_AT.plusDays(1));
    // 먼저 배웠지만 가장 최근에 다시 학습을 마친 표현이 맨 앞에 온다. 배운 날은 처음 마친 날이다.
    complete(USER_ID, newest, "SCENARIO", LEARNED_AT.minusDays(5), LEARNED_AT.plusDays(3));
    complete(OTHER_USER_ID, scenarioExpressionIds.get(2), "SCENARIO", LEARNED_AT, LEARNED_AT);

    List<FreeTalkLearnedExpression> candidates =
        service.select(USER_ID, Locale.EN, Locale.KR, List.of("hello"));

    assertThat(candidates)
        .extracting(FreeTalkLearnedExpression::expressionId)
        .containsExactly(newest, freeTalkExpressionId, oldest);
    assertThat(candidates.get(0).learnedOn()).isEqualTo(LocalDate.of(2026, 9, 5));
    assertThat(candidates.get(0).sourceType()).isEqualTo(FreeTalkExpressionReuseSource.SCENARIO);
    assertThat(candidates.get(1).sourceType()).isEqualTo(FreeTalkExpressionReuseSource.FREE_TALK);
    assertThat(candidates.get(0).text())
        .isEqualTo(
            jdbcTemplate.queryForObject(
                "select target_expression_text from writing_expression where id = ?",
                String.class,
                newest));
    assertThat(candidates.get(0).meaning()).isNotBlank();
  }

  @DisplayName("지금은 비활성인 표현과 다른 학습 언어로 요청한 표현은 후보에 넣지 않는다.")
  @Test
  void excludesInactiveExpressionsAndOtherLocales() {
    long active = scenarioExpressionIds.get(0);
    inactivatedExpressionId = scenarioExpressionIds.get(1);
    complete(USER_ID, active, "SCENARIO", LEARNED_AT, LEARNED_AT);
    complete(USER_ID, inactivatedExpressionId, "SCENARIO", LEARNED_AT, LEARNED_AT.plusDays(1));
    jdbcTemplate.update(
        "update writing_expression set status = 'INACTIVE' where id = ?", inactivatedExpressionId);

    assertThat(service.select(USER_ID, Locale.EN, Locale.KR, List.of("hello")))
        .extracting(FreeTalkLearnedExpression::expressionId)
        .containsExactly(active);
    assertThat(service.select(USER_ID, Locale.KR, Locale.EN, List.of("hello"))).isEmpty();
  }

  @DisplayName("배운 표현이 없는 사용자는 후보가 없다.")
  @Test
  void returnsNothingForUserWithoutLearnedExpressions() {
    assertThat(service.select(USER_ID, Locale.EN, Locale.KR, List.of("hello"))).isEmpty();
  }

  private void complete(
      long userId,
      long expressionId,
      String source,
      LocalDateTime completedAt,
      LocalDateTime lastCompletedAt) {
    jdbcTemplate.update(
        "insert into user_writing_expression_completion (user_profile_id, scenario_id,"
            + " writing_expression_id, learning_source, completed_at, last_completed_at)"
            + " values (?, NULL, ?, ?, ?, ?)",
        userId,
        expressionId,
        source,
        Timestamp.valueOf(completedAt),
        Timestamp.valueOf(lastCompletedAt));
  }

  private void seedExpression(long id, String text, String meaning) {
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
            ?, NULL, 'FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', 3, 'EN', 'KR', 1, ?, ?,
            '용법 요약', '용법 설명', 'Example sentence.', '예문 번역',
            ARRAY['Example'], ARRAY['Example'], '[]' FORMAT JSON, 'ACTIVE',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """,
        id,
        text,
        meaning);
  }
}
