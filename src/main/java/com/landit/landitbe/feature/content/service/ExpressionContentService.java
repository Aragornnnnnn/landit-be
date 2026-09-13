// 다른 기능에 표현 콘텐츠 조회와 활성 표현 잠금 계약을 제공한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.dto.ExpressionLearningContent;
import com.landit.landitbe.feature.content.dto.ExpressionText;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 표현 Entity를 노출하지 않는 콘텐츠 조회·잠금 경계다. */
@Service
@RequiredArgsConstructor
public class ExpressionContentService {
  private final WritingExpressionRepository writingExpressionRepository;

  /**
   * 활성 표현의 학습 정보를 조회한다.
   *
   * @param expressionId 표현 ID
   * @return 학습 정보
   * @throws ApiException 활성 표현이 없을 때
   */
  @Transactional(readOnly = true)
  public ExpressionLearningContent requireLearningContent(Long expressionId) {
    return writingExpressionRepository
        .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
        .map(ExpressionLearningContent::from)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  /**
   * 활성 표현을 잠근다. 잠금은 호출 트랜잭션이 끝날 때까지 유지된다.
   *
   * @param expressionId 표현 ID
   * @throws ApiException 활성 표현이 없을 때
   */
  @Transactional
  public void lockActiveExpression(Long expressionId) {
    writingExpressionRepository
        .findByIdAndStatusForUpdate(expressionId, ActiveStatus.ACTIVE)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  /**
   * 언어와 난이도에 맞는 시나리오 표현 ID를 학습 순서대로 조회한다.
   *
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param contentLevel 콘텐츠 난이도
   * @return 학습 순서의 표현 ID
   */
  @Transactional(readOnly = true)
  public List<Long> findScenarioExpressionIds(
      Long scenarioId, Locale targetLocale, Locale baseLocale, ContentLearningLevel contentLevel) {
    return writingExpressionRepository
        .findScenarioExpressions(
            scenarioId,
            targetLocale,
            baseLocale,
            contentLevel.minimumExpressionDifficulty(),
            contentLevel.maximumExpressionDifficulty(),
            ActiveStatus.ACTIVE)
        .stream()
        .map(WritingExpression::getId)
        .toList();
  }

  /**
   * 표현 본문을 일괄 조회한다. 없는 ID는 결과에서 제외한다.
   *
   * @param expressionIds 표현 ID 목록
   * @return 표현 본문 목록
   */
  @Transactional(readOnly = true)
  public List<ExpressionText> findExpressionTexts(List<Long> expressionIds) {
    return writingExpressionRepository.findAllById(expressionIds).stream()
        .map(ExpressionText::from)
        .toList();
  }
}
