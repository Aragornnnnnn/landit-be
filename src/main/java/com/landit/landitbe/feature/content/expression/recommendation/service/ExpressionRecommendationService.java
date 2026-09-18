// 프리톡에서 재사용할 공용 표현 후보를 검색하고 검증한다.

package com.landit.landitbe.feature.content.expression.recommendation.service;

import com.landit.landitbe.feature.content.expression.domain.WritingExpression;
import com.landit.landitbe.feature.content.expression.domain.WritingExpressionSource;
import com.landit.landitbe.feature.content.expression.recommendation.dto.ExpressionEmbeddingMatch;
import com.landit.landitbe.feature.content.expression.recommendation.dto.ExpressionRecommendationCandidate;
import com.landit.landitbe.feature.content.expression.recommendation.dto.FreeTalkCandidateSearch;
import com.landit.landitbe.feature.content.expression.recommendation.repository.ExpressionEmbeddingSearchRepository;
import com.landit.landitbe.feature.content.expression.repository.WritingExpressionRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡에서 재사용할 공용 표현 후보를 검색하고 검증한다. */
@Service
@RequiredArgsConstructor
public class ExpressionRecommendationService {
  private final WritingExpressionRepository writingExpressionRepository;
  private final ExpressionEmbeddingSearchRepository expressionEmbeddingSearchRepository;

  /**
   * 프리톡 세션에 연결할 기존 표현이 공용 후보 조건을 만족하는지 검증한다.
   *
   * @param expressionId 프리톡 세션에 연결할 표현 ID
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @throws ApiException 공용 프리톡 후보가 아니거나 활성 상태가 아닐 때
   */
  @Transactional(readOnly = true)
  public void validatePublicFreeTalkExpression(
      Long expressionId, Locale targetLocale, Locale baseLocale) {
    writingExpressionRepository
        .findPublicExpressionCandidateById(
            expressionId,
            WritingExpressionSource.FREE_TALK,
            targetLocale,
            baseLocale,
            ActiveStatus.ACTIVE)
        .orElseThrow(() -> new ApiException(ErrorCode.AI_RESPONSE_INVALID));
  }

  /**
   * 프리톡 AI가 재사용할 공용 활성 표현 후보를 ID 목록으로 조회한다. 결과는 입력 ID 순서를 유지한다.
   *
   * @param expressionIds 유사도 순으로 정렬된 표현 ID 목록
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @return 입력 순서를 유지한 공용 활성 표현 후보 목록
   */
  @Transactional(readOnly = true)
  public List<ExpressionRecommendationCandidate> getExpressionCandidatesByIds(
      List<Long> expressionIds, Locale targetLocale, Locale baseLocale) {
    Map<Long, WritingExpression> expressionsById =
        writingExpressionRepository
            .findPublicExpressionCandidatesByIds(
                expressionIds,
                WritingExpressionSource.FREE_TALK,
                targetLocale,
                baseLocale,
                ActiveStatus.ACTIVE)
            .stream()
            .collect(Collectors.toMap(WritingExpression::getId, expression -> expression));
    return expressionIds.stream()
        .map(expressionsById::get)
        .filter(Objects::nonNull)
        .map(
            expression ->
                new ExpressionRecommendationCandidate(
                    expression.getId(),
                    expression.getTargetExpressionText(),
                    expression.getBaseExpressionMeaningText(),
                    expression.getUsageSummary()))
        .toList();
  }

  /**
   * 임베딩 벡터로 공용 프리톡 표현 후보를 코사인 거리 오름차순으로 검색한다. 사용자가 이미 학습 완료한 표현과 난이도 상한을 넘는 표현은 제외한다.
   *
   * @param search 검색 조건
   * @return 코사인 거리 오름차순의 표현 후보 목록
   */
  @Transactional(readOnly = true)
  public List<ExpressionEmbeddingMatch> searchFreeTalkCandidatesByEmbedding(
      FreeTalkCandidateSearch search) {
    return expressionEmbeddingSearchRepository.searchFreeTalkCandidates(search);
  }
}
