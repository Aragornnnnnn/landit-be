// 사용자 수준을 적용해 표현 목록과 시작·연습 콘텐츠를 선택한다.

package com.landit.landitbe.feature.learning.expression.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.expression.domain.WritingExpressionSource;
import com.landit.landitbe.feature.content.expression.dto.ExpressionLearningMaterial;
import com.landit.landitbe.feature.content.expression.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.expression.dto.ExpressionResponse;
import com.landit.landitbe.feature.content.expression.practice.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.expression.practice.service.ExpressionPracticeService;
import com.landit.landitbe.feature.content.expression.service.ExpressionQueryService;
import com.landit.landitbe.feature.content.scenario.service.ScenarioService;
import com.landit.landitbe.feature.learning.scenario.level.service.ScenarioLearningLevelService;
import com.landit.landitbe.feature.profile.learning.service.ProfileLearningService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 수준을 적용해 표현 목록과 시작·연습 콘텐츠를 선택한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpressionLearningContentService {
  private final ExpressionQueryService expressionQueryService;
  private final ScenarioService scenarioService;
  private final ProfileLearningService profileLearningService;
  private final ScenarioLearningLevelService scenarioLearningLevelService;
  private final ExpressionPracticeService expressionPracticeService;

  /**
   * 사용자의 언어와 복습 수준에 맞는 시나리오 표현을 선택한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 학습 순서의 표현 콘텐츠
   * @throws ApiException 시나리오가 존재하지 않을 때
   */
  public List<ExpressionResponse> getScenarioExpressions(Long userId, Long scenarioId) {
    scenarioService.validateExists(scenarioId);
    var locale = profileLearningService.getUserLocale(userId);
    ContentLearningLevel level = scenarioLearningLevelService.expressionLevel(userId, scenarioId);
    return expressionQueryService.getScenarioExpressions(scenarioId, locale, level);
  }

  /**
   * 학습을 시작할 수 있는 표현의 콘텐츠를 제공한다.
   *
   * @param userId 사용자 ID
   * @param expressionId 표현 ID
   * @return 학습 상태를 조립하기 전 콘텐츠
   * @throws ApiException 표현이 없거나 허용 난이도가 아닐 때
   */
  public ExpressionLearningResponse prepareLearningContent(Long userId, Long expressionId) {
    return requireAccessibleMaterial(userId, expressionId).detail();
  }

  /**
   * 접근 가능한 표현의 추가 연습 문제를 구성한다.
   *
   * @param userId 사용자 ID
   * @param expressionId 표현 ID
   * @return 추가 예문과 작문 문제
   * @throws ApiException 표현 접근 또는 예문 콘텐츠가 유효하지 않을 때
   */
  public ExpressionPracticeResponse getExtraPracticeExamples(Long userId, Long expressionId) {
    var material = requireAccessibleMaterial(userId, expressionId);
    var detail = material.detail();
    return expressionPracticeService.buildPracticeResponse(
        expressionId,
        detail.targetExpressionText(),
        detail.baseExpressionMeaningText(),
        detail.usageDescription(),
        material.practiceExamplesPayload());
  }

  private ExpressionLearningMaterial requireAccessibleMaterial(Long userId, Long expressionId) {
    var material = expressionQueryService.requireLearningMaterial(expressionId);
    var content = material.content();
    if (content.expressionSource() == WritingExpressionSource.SCENARIO
        && !scenarioLearningLevelService
            .expressionLevel(userId, content.scenarioId())
            .includesExpressionDifficulty(content.difficultyLevel())) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    return material;
  }
}
