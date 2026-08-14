// 후보 선정의 병합, 임계값, 최상위 유지, 실패 조건을 단위 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.content.ExpressionSearchProperties;
import com.landit.landitbe.feature.content.repository.ExpressionEmbeddingMatch;
import com.landit.landitbe.feature.content.service.ExpressionQueryService;
import com.landit.landitbe.feature.session.client.ai.AiConversationExcerpt;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 후보 선정의 병합, 임계값, 최상위 유지, 실패 조건을 단위 검증한다. */
@ExtendWith(MockitoExtension.class)
class ExpressionCandidateSelectionServiceTest {

  private static final long USER_ID = 7L;
  private static final AiConversationExcerpt FIRST_EXCERPT =
      new AiConversationExcerpt("That's easy for me.", List.of(1.0f));
  private static final AiConversationExcerpt SECOND_EXCERPT =
      new AiConversationExcerpt("I cook every day.", List.of(0.5f));

  @Mock private ExpressionQueryService expressionQueryService;

  private ExpressionCandidateSelectionService selectionService(
      int maxCandidates, double distanceThreshold) {
    return new ExpressionCandidateSelectionService(
        expressionQueryService,
        new ExpressionSearchProperties("in-memory", maxCandidates, distanceThreshold));
  }

  @Test
  void mergesExcerptResultsWithMinimumDistanceAndSortsAscending() {
    when(expressionQueryService.searchFreeTalkCandidatesByEmbedding(
            eq(FIRST_EXCERPT.embedding()), anyLong(), any(), any(), anyInt()))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.5), new ExpressionEmbeddingMatch(202L, 0.3)));
    when(expressionQueryService.searchFreeTalkCandidatesByEmbedding(
            eq(SECOND_EXCERPT.embedding()), anyLong(), any(), any(), anyInt()))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.1), // 같은 표현이 더 가까운 거리로 재등장 → 병합
                new ExpressionEmbeddingMatch(203L, 0.4)));

    List<Long> candidateIds =
        selectionService(30, 0.6)
            .selectCandidateIds(
                List.of(FIRST_EXCERPT, SECOND_EXCERPT), USER_ID, Locale.EN, Locale.KR);

    assertThat(candidateIds).containsExactly(201L, 202L, 203L);
  }

  @Test
  void filtersCandidatesOverDistanceThreshold() {
    when(expressionQueryService.searchFreeTalkCandidatesByEmbedding(
            any(), anyLong(), any(), any(), anyInt()))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.2),
                new ExpressionEmbeddingMatch(202L, 0.61),
                new ExpressionEmbeddingMatch(203L, 0.9)));

    List<Long> candidateIds =
        selectionService(30, 0.6)
            .selectCandidateIds(List.of(FIRST_EXCERPT), USER_ID, Locale.EN, Locale.KR);

    assertThat(candidateIds).containsExactly(201L);
  }

  @Test
  void keepsClosestCandidateWhenNoneMeetsThreshold() {
    when(expressionQueryService.searchFreeTalkCandidatesByEmbedding(
            any(), anyLong(), any(), any(), anyInt()))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.8), new ExpressionEmbeddingMatch(202L, 0.7)));

    List<Long> candidateIds =
        selectionService(30, 0.6)
            .selectCandidateIds(List.of(FIRST_EXCERPT), USER_ID, Locale.EN, Locale.KR);

    assertThat(candidateIds).containsExactly(202L);
  }

  @Test
  void limitsPassingCandidatesToMaxCandidates() {
    when(expressionQueryService.searchFreeTalkCandidatesByEmbedding(
            any(), anyLong(), any(), any(), anyInt()))
        .thenReturn(
            List.of(
                new ExpressionEmbeddingMatch(201L, 0.1),
                new ExpressionEmbeddingMatch(202L, 0.2),
                new ExpressionEmbeddingMatch(203L, 0.3)));

    List<Long> candidateIds =
        selectionService(2, 0.6)
            .selectCandidateIds(List.of(FIRST_EXCERPT), USER_ID, Locale.EN, Locale.KR);

    assertThat(candidateIds).containsExactly(201L, 202L);
  }

  @Test
  void failsWhenSearchReturnsNoCandidateAtAll() {
    when(expressionQueryService.searchFreeTalkCandidatesByEmbedding(
            any(), anyLong(), any(), any(), anyInt()))
        .thenReturn(List.of());

    assertThatThrownBy(
            () ->
                selectionService(30, 0.6)
                    .selectCandidateIds(List.of(FIRST_EXCERPT), USER_ID, Locale.EN, Locale.KR))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AI_GENERATION_FAILED);
  }
}
