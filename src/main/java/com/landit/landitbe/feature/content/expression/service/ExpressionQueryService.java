// 지정된 언어와 난이도로 표현 콘텐츠를 조회한다.

package com.landit.landitbe.feature.content.expression.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.expression.dto.ExpressionLearningMaterial;
import com.landit.landitbe.feature.content.expression.dto.ExpressionResponse;
import com.landit.landitbe.feature.content.expression.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.profile.learning.dto.UserLocale;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 지정된 언어와 난이도로 표현 콘텐츠를 조회한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressionQueryService {
  private static final String EXPRESSION_NOT_FOUND_LOG =
      "추가 예문 조회 실패: 존재하지 않거나 비활성화된 표현입니다. expressionId={}";
  private final WritingExpressionRepository writingExpressionRepository;

  /**
   * 학습 쪽에서 결정한 언어와 난이도로 표현을 조회한다.
   *
   * @param scenarioId 시나리오 ID
   * @param userLocale 조회할 언어 조합
   * @param contentLevel 조회할 콘텐츠 수준
   * @return 학습 순서의 표현 콘텐츠
   */
  @Transactional(readOnly = true)
  public List<ExpressionResponse> getScenarioExpressions(
      Long scenarioId, UserLocale userLocale, ContentLearningLevel contentLevel) {
    return writingExpressionRepository
        .findScenarioExpressions(
            scenarioId,
            userLocale.targetLocale(),
            userLocale.baseLocale(),
            contentLevel.minimumExpressionDifficulty(),
            contentLevel.maximumExpressionDifficulty(),
            ActiveStatus.ACTIVE)
        .stream()
        .map(expression -> ExpressionResponse.from(expression, false, false))
        .toList();
  }

  /**
   * 활성 표현의 학습 검증과 응답 구성에 필요한 콘텐츠를 한 번에 조회한다.
   *
   * @param expressionId 표현 ID
   * @return 사용자 상태를 포함하지 않는 콘텐츠 값
   * @throws ApiException 표현이 없거나 비활성일 때
   */
  @Transactional(readOnly = true)
  public ExpressionLearningMaterial requireLearningMaterial(Long expressionId) {
    return writingExpressionRepository
        .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
        .map(ExpressionLearningMaterial::from)
        .orElseThrow(
            () -> {
              log.warn(EXPRESSION_NOT_FOUND_LOG, expressionId);
              return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
            });
  }
}
