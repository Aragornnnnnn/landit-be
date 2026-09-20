// 후보 선정의 병합, 임계값, 최상위 유지, 실패 조건을 단위 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.content.ExpressionSearchProperties;
import com.landit.landitbe.feature.content.expression.recommendation.dto.ExpressionEmbeddingMatch;
import com.landit.landitbe.feature.content.expression.recommendation.dto.FreeTalkCandidateSearch;
import com.landit.landitbe.feature.content.expression.recommendation.service.ExpressionRecommendationService;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiConversationExcerpt;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 후보 선정의 병합, 임계값, 최상위 유지, 실패 조건을 단위 검증한다. */
@ExtendWith(MockitoExtension.class)
class ExpressionCandidateSelectionServiceTest {

  private static final long USER_ID = 7L;
  private static final int MAX_DIFFICULTY_LEVEL = 3;
  private static final AiConversationExcerpt FIRST_EXCERPT =
      new AiConversationExcerpt("That's easy for me.", List.of(1.0f));
  private static final AiConversationExcerpt SECOND_EXCERPT =
      new AiConversationExcerpt("I cook every day.", List.of(0.5f));

  @Mock private ExpressionRecommendationService expressionRecommendationService;

  private ExpressionCandidateSelectionService selectionService(
      int maxCandidates, double distanceThreshold) {
    return new ExpressionCandidateSelectionService(
        expressionRecommendationService,
        new ExpressionSearchProperties("in-memory", maxCandidates, distanceThreshold));
  }

  // 발화 임베딩으로 검색 조건을 구분하는 matcher.
  private static FreeTalkCandidateSearch searchFor(AiConversationExcerpt excerpt) {
    return argThat(search -> search != null && search.embedding().equals(excerpt.embedding()));
  }

  private List<Long> selectWith(
      ExpressionCandidateSelectionService selectionService, AiConversationExcerpt... excerpts) {
    return selectionService.selectCandidateIds(
        List.of(excerpts), USER_ID, Locale.EN, Locale.KR, MAX_DIFFICULTY_LEVEL);
  }

  @DisplayName("대화 발췌별 검색 결과를 최소 거리로 합치고 가까운 순서로 정렬한다.")
  @Test
  void mergesExcerptResultsWithMinimumDistanceAndSortsAscending() {
    when(expressionRecommendationService.searchFreeTalkCandidatesByEmbedding(
            searchFor(FIRST_EXCERPT)))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.5), new ExpressionEmbeddingMatch(202L, 0.3)));
    when(expressionRecommendationService.searchFreeTalkCandidatesByEmbedding(
            searchFor(SECOND_EXCERPT)))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.1), // 같은 표현이 더 가까운 거리로 재등장 → 병합
                new ExpressionEmbeddingMatch(203L, 0.4)));

    List<Long> candidateIds = selectWith(selectionService(30, 0.6), FIRST_EXCERPT, SECOND_EXCERPT);

    assertThat(candidateIds).containsExactly(201L, 202L, 203L);
  }

  @DisplayName("거리 임계값을 넘는 표현 후보를 제외한다.")
  @Test
  void filtersCandidatesOverDistanceThreshold() {
    when(expressionRecommendationService.searchFreeTalkCandidatesByEmbedding(any()))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.2),
                new ExpressionEmbeddingMatch(202L, 0.61),
                new ExpressionEmbeddingMatch(203L, 0.9)));

    List<Long> candidateIds = selectWith(selectionService(30, 0.6), FIRST_EXCERPT);

    assertThat(candidateIds).containsExactly(201L);
  }

  @DisplayName("거리 임계값을 만족하는 후보가 없으면 가장 가까운 후보를 유지한다.")
  @Test
  void keepsClosestCandidateWhenNoneMeetsThreshold() {
    when(expressionRecommendationService.searchFreeTalkCandidatesByEmbedding(any()))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.8), new ExpressionEmbeddingMatch(202L, 0.7)));

    List<Long> candidateIds = selectWith(selectionService(30, 0.6), FIRST_EXCERPT);

    assertThat(candidateIds).containsExactly(202L);
  }

  @DisplayName("조건을 통과한 표현도 최대 후보 수까지만 반환한다.")
  @Test
  void limitsPassingCandidatesToMaxCandidates() {
    when(expressionRecommendationService.searchFreeTalkCandidatesByEmbedding(any()))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.1),
                new ExpressionEmbeddingMatch(202L, 0.2),
                new ExpressionEmbeddingMatch(203L, 0.3)));

    List<Long> candidateIds = selectWith(selectionService(2, 0.6), FIRST_EXCERPT);

    assertThat(candidateIds).containsExactly(201L, 202L);
  }

  @DisplayName("표현 검색에 최대 난이도를 포함한 검색 조건을 전달한다.")
  @Test
  void passesSearchConditionsIncludingMaxDifficultyLevel() {
    when(expressionRecommendationService.searchFreeTalkCandidatesByEmbedding(any()))
        .thenReturn(List.of(new ExpressionEmbeddingMatch(201L, 0.1)));

    selectWith(selectionService(30, 0.6), FIRST_EXCERPT);

    ArgumentCaptor<FreeTalkCandidateSearch> captor =
        ArgumentCaptor.forClass(FreeTalkCandidateSearch.class);
    verify(expressionRecommendationService).searchFreeTalkCandidatesByEmbedding(captor.capture());
    FreeTalkCandidateSearch search = captor.getValue();
    assertThat(search.maxDifficultyLevel()).isEqualTo(MAX_DIFFICULTY_LEVEL);
    assertThat(search.userProfileId()).isEqualTo(USER_ID);
    assertThat(search.limit()).isEqualTo(30);
    assertThat(search.targetLocale()).isEqualTo(Locale.EN);
    assertThat(search.baseLocale()).isEqualTo(Locale.KR);
  }

  @DisplayName("표현 검색 결과가 전혀 없으면 후보 선정에 실패한다.")
  @Test
  void failsWhenSearchReturnsNoCandidateAtAll() {
    when(expressionRecommendationService.searchFreeTalkCandidatesByEmbedding(any()))
        .thenReturn(List.of());

    assertThatThrownBy(() -> selectWith(selectionService(30, 0.6), FIRST_EXCERPT))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AI_GENERATION_FAILED);
  }
}
