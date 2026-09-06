// 임베딩 유사도 검색의 정렬, 제한, 제외 조건을 검증한다.

package com.landit.landitbe.feature.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.content.repository.ExpressionEmbeddingMatch;
import com.landit.landitbe.feature.content.repository.ExpressionEmbeddingSearchRepository;
import com.landit.landitbe.feature.content.repository.FreeTalkCandidateSearch;
import com.landit.landitbe.shared.domain.Locale;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 임베딩 유사도 검색의 정렬, 제한, 제외 조건을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ExpressionEmbeddingSearchRepositoryIntegrationTests {

  private static final long USER_ID = 991001L;
  // 난이도 필터를 걸지 않는 상한. 학습 수준 4~5 사용자와 학습 수준을 모르는 사용자에게 쓰는 값이다.
  private static final int FULL_DIFFICULTY = 5;
  private static final List<Float> QUERY_EMBEDDING = List.of(1.0f, 0.0f, 0.0f);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ExpressionEmbeddingSearchRepository searchRepository;

  @Test
  void searchOrdersCandidatesByCosineDistance() {
    seedUser(USER_ID);
    seedFreeTalkExpression(991101L, "EN", "KR", "ACTIVE", "[0.6, 0.8, 0]"); // 거리 0.4
    seedFreeTalkExpression(991102L, "EN", "KR", "ACTIVE", "[1, 0, 0]"); // 거리 0
    seedFreeTalkExpression(991103L, "EN", "KR", "ACTIVE", "[0, 1, 0]"); // 거리 1

    List<ExpressionEmbeddingMatch> matches =
        searchRepository.searchFreeTalkCandidates(search(FULL_DIFFICULTY, 10));

    assertThat(matches)
        .extracting(ExpressionEmbeddingMatch::expressionId)
        .containsExactly(991102L, 991101L, 991103L);
    assertThat(matches.get(0).distance()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-9));
    assertThat(matches.get(1).distance()).isCloseTo(0.4, org.assertj.core.data.Offset.offset(1e-9));
  }

  @Test
  void searchLimitsCandidateCount() {
    seedUser(USER_ID);
    seedFreeTalkExpression(991101L, "EN", "KR", "ACTIVE", "[1, 0, 0]");
    seedFreeTalkExpression(991102L, "EN", "KR", "ACTIVE", "[0.6, 0.8, 0]");
    seedFreeTalkExpression(991103L, "EN", "KR", "ACTIVE", "[0, 1, 0]");

    List<ExpressionEmbeddingMatch> matches =
        searchRepository.searchFreeTalkCandidates(search(FULL_DIFFICULTY, 2));

    assertThat(matches)
        .extracting(ExpressionEmbeddingMatch::expressionId)
        .containsExactly(991101L, 991102L);
  }

  @Test
  void searchExcludesCompletedAndInactiveAndOtherLocaleExpressions() {
    seedUser(USER_ID);
    seedFreeTalkExpression(991101L, "EN", "KR", "ACTIVE", "[1, 0, 0]");
    seedFreeTalkExpression(991102L, "EN", "KR", "ACTIVE", "[0.9, 0.1, 0]"); // 학습 완료 → 제외
    seedFreeTalkExpression(991103L, "EN", "KR", "INACTIVE", "[1, 0, 0]"); // 비활성 → 제외
    seedFreeTalkExpression(991104L, "EN", "EN", "ACTIVE", "[1, 0, 0]"); // 기준 언어 불일치 → 제외
    seedCompletion(USER_ID, 991102L);

    List<ExpressionEmbeddingMatch> matches =
        searchRepository.searchFreeTalkCandidates(search(FULL_DIFFICULTY, 10));

    assertThat(matches).extracting(ExpressionEmbeddingMatch::expressionId).containsExactly(991101L);
  }

  @Test
  void searchRejectsDimensionMismatchedStoredEmbedding() {
    seedUser(USER_ID);
    seedFreeTalkExpression(991101L, "EN", "KR", "ACTIVE", "[1, 0]");

    assertThatThrownBy(() -> searchRepository.searchFreeTalkCandidates(search(FULL_DIFFICULTY, 10)))
        .hasRootCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void searchExcludesExpressionsOverMaxDifficultyLevel() {
    seedUser(USER_ID);
    seedFreeTalkExpression(991101L, "EN", "KR", "ACTIVE", "[0.6, 0.8, 0]", 3);
    seedFreeTalkExpression(991102L, "EN", "KR", "ACTIVE", "[0, 1, 0]", 2);
    seedFreeTalkExpression(991103L, "EN", "KR", "ACTIVE", "[1, 0, 0]", 4); // 상한 초과 → 제외
    seedFreeTalkExpression(991104L, "EN", "KR", "ACTIVE", "[1, 0, 0]", 5); // 상한 초과 → 제외

    List<ExpressionEmbeddingMatch> matches =
        searchRepository.searchFreeTalkCandidates(search(3, 10));

    // 991103, 991104는 거리 0으로 가장 가깝지만 난이도가 정렬보다 먼저 걸러진다.
    assertThat(matches)
        .extracting(ExpressionEmbeddingMatch::expressionId)
        .containsExactly(991101L, 991102L);
  }

  @Test
  void searchIncludesEveryDifficultyLevelWhenMaxIsFive() {
    seedUser(USER_ID);
    seedFreeTalkExpression(991101L, "EN", "KR", "ACTIVE", "[0, 1, 0]", 2);
    seedFreeTalkExpression(991102L, "EN", "KR", "ACTIVE", "[0.6, 0.8, 0]", 4);
    seedFreeTalkExpression(991103L, "EN", "KR", "ACTIVE", "[1, 0, 0]", 5);

    List<ExpressionEmbeddingMatch> matches =
        searchRepository.searchFreeTalkCandidates(search(FULL_DIFFICULTY, 10));

    assertThat(matches)
        .extracting(ExpressionEmbeddingMatch::expressionId)
        .containsExactly(991103L, 991102L, 991101L);
  }

  private void seedUser(long userId) {
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level,
            push_permission_status, status, created_at, updated_at
        )
        values (?, 'embedding-search-user', 'EN', 'KR', 1, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId);
  }

  // 검색 조건을 만든다. 난이도 상한만 바꿔 가며 쓰기 위해 헬퍼로 둔다.
  private FreeTalkCandidateSearch search(int maxDifficultyLevel, int limit) {
    return new FreeTalkCandidateSearch(
        QUERY_EMBEDDING, USER_ID, Locale.EN, Locale.KR, maxDifficultyLevel, limit);
  }

  // 난이도를 지정하지 않는 표현은 쉬운 구간(3)으로 넣는다.
  private void seedFreeTalkExpression(
      long expressionId, String targetLocale, String baseLocale, String status, String embedding) {
    seedFreeTalkExpression(expressionId, targetLocale, baseLocale, status, embedding, 3);
  }

  private void seedFreeTalkExpression(
      long expressionId,
      String targetLocale,
      String baseLocale,
      String status,
      String embedding,
      int difficultyLevel) {
    jdbcTemplate.update(
        """
        insert into writing_expression (
            id, scenario_id, expression_source, expression_type, usage_frequency_level, difficulty_level,
            target_locale, base_locale, display_order, target_expression_text,
            base_expression_meaning_text, usage_summary, usage_description,
            representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            practice_examples_payload, embedding, status, created_at, updated_at
        )
        values (?, NULL, 'FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', ?, ?, ?, 1, 'piece of cake',
            '식은 죽 먹기', 'usage summary', 'usage description',
            'It was a piece of cake.', '그건 식은 죽 먹기였어.',
            ARRAY['It','was','a','piece','of','cake'], ARRAY['It','was','a','piece','of','cake'],
            '[]', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        expressionId,
        difficultyLevel,
        targetLocale,
        baseLocale,
        embedding,
        status);
  }

  private void seedCompletion(long userId, long expressionId) {
    jdbcTemplate.update(
        """
        insert into user_writing_expression_completion (
            user_profile_id, scenario_id, writing_expression_id, learning_source,
            completed_at, last_completed_at
        )
        values (?, NULL, ?, 'FREE_TALK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        expressionId);
  }
}
