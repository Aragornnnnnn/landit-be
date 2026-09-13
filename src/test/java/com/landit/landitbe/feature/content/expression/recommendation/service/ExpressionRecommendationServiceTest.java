// 표현 추천 후보 검색 계약을 검증한다.

package com.landit.landitbe.feature.content.expression.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.expression.domain.WritingExpression;
import com.landit.landitbe.feature.content.expression.domain.WritingExpressionSource;
import com.landit.landitbe.feature.content.expression.recommendation.dto.ExpressionEmbeddingMatch;
import com.landit.landitbe.feature.content.expression.recommendation.dto.ExpressionRecommendationCandidate;
import com.landit.landitbe.feature.content.expression.recommendation.dto.FreeTalkCandidateSearch;
import com.landit.landitbe.feature.content.expression.recommendation.repository.ExpressionEmbeddingSearchRepository;
import com.landit.landitbe.feature.content.expression.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.content.scenario.service.ScenarioLearningLevelService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 표현 업무의 조회 계약을 검증한다. */
@ExtendWith(MockitoExtension.class)
class ExpressionRecommendationServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long SCENARIO_ID = 999L;
  private static final Long EXPRESSION_ID = 101L;
  @Mock private WritingExpressionRepository writingExpressionRepository;
  @Mock private ExpressionEmbeddingSearchRepository expressionEmbeddingSearchRepository;
  @Mock private ScenarioLearningLevelService scenarioLearningLevelService;
  @InjectMocks private ExpressionRecommendationService expressionRecommendationService;

  @Test
  void returnsCandidatesByIdsPreservingInputOrder() {
    WritingExpression first = mock(WritingExpression.class);
    when(first.getId()).thenReturn(101L);
    when(first.getTargetExpressionText()).thenReturn("target-101");
    when(first.getBaseExpressionMeaningText()).thenReturn("base-101");
    when(first.getUsageSummary()).thenReturn("제안에 동의할 때 사용");
    WritingExpression second = mock(WritingExpression.class);
    when(second.getId()).thenReturn(102L);
    when(second.getTargetExpressionText()).thenReturn("target-102");
    when(second.getBaseExpressionMeaningText()).thenReturn("base-102");
    when(second.getUsageSummary()).thenReturn("정중하게 거절할 때 사용");
    // 저장소는 순서를 보장하지 않아도 서비스가 입력 ID 순서를 유지해야 한다.
    when(writingExpressionRepository.findPublicExpressionCandidatesByIds(
            eq(List.of(102L, 101L)),
            eq(WritingExpressionSource.FREE_TALK),
            eq(Locale.EN),
            eq(Locale.KR),
            eq(ActiveStatus.ACTIVE)))
        .thenReturn(List.of(first, second));

    List<ExpressionRecommendationCandidate> candidates =
        expressionRecommendationService.getExpressionCandidatesByIds(
            List.of(102L, 101L), Locale.EN, Locale.KR);

    assertThat(candidates)
        .containsExactly(
            new ExpressionRecommendationCandidate(102L, "target-102", "base-102", "정중하게 거절할 때 사용"),
            new ExpressionRecommendationCandidate(101L, "target-101", "base-101", "제안에 동의할 때 사용"));
  }

  @Test
  void delegatesEmbeddingSearchToOwnedRepository() {
    List<ExpressionEmbeddingMatch> matches = List.of(new ExpressionEmbeddingMatch(101L, 0.2));
    FreeTalkCandidateSearch search =
        new FreeTalkCandidateSearch(List.of(1.0f), USER_ID, Locale.EN, Locale.KR, 3, 30);
    when(expressionEmbeddingSearchRepository.searchFreeTalkCandidates(search)).thenReturn(matches);

    List<ExpressionEmbeddingMatch> result =
        expressionRecommendationService.searchFreeTalkCandidatesByEmbedding(search);

    assertThat(result).isEqualTo(matches);
  }

  @Test
  void rejectsExpressionOutsidePublicFreeTalkCandidates() {
    when(writingExpressionRepository.findPublicExpressionCandidateById(
            EXPRESSION_ID,
            WritingExpressionSource.FREE_TALK,
            Locale.EN,
            Locale.KR,
            ActiveStatus.ACTIVE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                expressionRecommendationService.validatePublicFreeTalkExpression(
                    EXPRESSION_ID, Locale.EN, Locale.KR))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
  }

  @Test
  void acceptsPublicFreeTalkExpressionWithSameLocale() {
    WritingExpression expression = mock(WritingExpression.class);
    when(writingExpressionRepository.findPublicExpressionCandidateById(
            EXPRESSION_ID,
            WritingExpressionSource.FREE_TALK,
            Locale.EN,
            Locale.KR,
            ActiveStatus.ACTIVE))
        .thenReturn(Optional.of(expression));

    expressionRecommendationService.validatePublicFreeTalkExpression(
        EXPRESSION_ID, Locale.EN, Locale.KR);
  }
}
