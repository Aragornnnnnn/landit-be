// 학습 순서에 맞는 표현 목록과 시작 콘텐츠를 값으로 조회한다.

package com.landit.landitbe.feature.content.expression.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.expression.domain.WritingExpression;
import com.landit.landitbe.feature.content.expression.domain.WritingExpressionSource;
import com.landit.landitbe.feature.content.expression.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.expression.dto.ExpressionResponse;
import com.landit.landitbe.feature.content.expression.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.content.scenario.service.ScenarioLearningLevelService;
import com.landit.landitbe.feature.content.scenario.service.ScenarioService;
import com.landit.landitbe.feature.profile.learning.dto.UserLocale;
import com.landit.landitbe.feature.profile.learning.service.ProfileLearningService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학습 순서에 맞는 표현 목록과 시작 콘텐츠를 값으로 조회한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressionQueryService {

  private static final String EXPRESSION_NOT_FOUND_LOG =
      "추가 예문 조회 실패: 존재하지 않거나 비활성화된 표현입니다. expressionId={}";
  private final ScenarioService scenarioService;
  private final ProfileLearningService profileLearningService;
  private final WritingExpressionRepository writingExpressionRepository;
  private final ScenarioLearningLevelService scenarioLearningLevelService;

  /**
   * 사용자 locale에 맞는 시나리오 표현을 학습 순서대로 조회하고 콘텐츠를 반환한다.
   *
   * @param userId 표현 목록을 조회할 사용자 ID
   * @param scenarioId 표현이 속한 시나리오 ID
   * @return 학습 순서의 표현 콘텐츠 목록
   * @throws ApiException 시나리오가 존재하지 않을 때
   */
  @Transactional(readOnly = true)
  public List<ExpressionResponse> getScenarioExpressions(Long userId, Long scenarioId) {
    scenarioService.validateExists(scenarioId);

    // 사용자 로케일에 맞는 표현을 로케일별 노출 순서로 조회한다.
    UserLocale userLocale = profileLearningService.getUserLocale(userId);
    ContentLearningLevel contentLevel =
        scenarioLearningLevelService.expressionLevel(userId, scenarioId);
    List<WritingExpression> expressions =
        writingExpressionRepository.findScenarioExpressions(
            scenarioId,
            userLocale.targetLocale(),
            userLocale.baseLocale(),
            contentLevel.minimumExpressionDifficulty(),
            contentLevel.maximumExpressionDifficulty(),
            ActiveStatus.ACTIVE);

    return expressions.stream()
        .map(expression -> ExpressionResponse.from(expression, false, false))
        .toList();
  }

  /**
   * 학습 시작에 앞서 접근 가능한 표현 콘텐츠를 조회한다.
   *
   * @param userId 요청 사용자
   * @param expressionId 표현 ID
   * @return 학습 상태와 음성을 조합하기 전 콘텐츠 값
   * @throws ApiException 표현이 없거나 허용 난이도가 아닐 때
   */
  @Transactional(readOnly = true)
  public ExpressionLearningResponse prepareLearningContent(Long userId, Long expressionId) {
    return ExpressionLearningResponse.from(
        requireAccessibleExpression(userId, expressionId), null, null, false);
  }

  // 사용자가 접근할 수 있는 활성 표현을 조회한다.
  private WritingExpression requireAccessibleExpression(Long userId, Long expressionId) {
    WritingExpression expression =
        writingExpressionRepository
            .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
            .orElseThrow(
                () -> {
                  log.warn(EXPRESSION_NOT_FOUND_LOG, expressionId);
                  return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
                });
    if (expression.getExpressionSource() == WritingExpressionSource.SCENARIO
        && !scenarioLearningLevelService
            .expressionLevel(userId, expression.getScenarioId())
            .includesExpressionDifficulty(expression.getDifficultyLevel())) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    return expression;
  }
}
