// 시나리오 표현 콘텐츠와 완료 이력을 합쳐 학습 진행도를 조회한다.

package com.landit.landitbe.feature.learning.expression.progress.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.expression.service.ExpressionContentService;
import com.landit.landitbe.feature.learning.expression.progress.dto.ExpressionProgress;
import com.landit.landitbe.feature.learning.scenario.level.service.ScenarioLearningLevelService;
import com.landit.landitbe.feature.profile.learning.dto.UserLocale;
import com.landit.landitbe.feature.profile.learning.service.ProfileLearningService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 표현 콘텐츠와 완료 이력을 합쳐 학습 진행도를 조회한다. */
@Service
@RequiredArgsConstructor
public class ExpressionProgressService {

  private final ProfileLearningService profileLearningService;
  private final ScenarioLearningLevelService scenarioLearningLevelService;
  private final ExpressionContentService expressionContentService;
  private final ExpressionCompletionService expressionCompletionService;

  /**
   * 날짜별 시나리오 화면에 표시할 표현 학습 진행도를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 활성 표현 전체 수와 완료 표현 수
   */
  @Transactional(readOnly = true)
  public ExpressionProgress getExpressionProgress(Long userId, Long scenarioId) {
    UserLocale userLocale = profileLearningService.getUserLocale(userId);
    ContentLearningLevel contentLevel =
        scenarioLearningLevelService.expressionLevel(userId, scenarioId);
    List<Long> expressionIds =
        expressionContentService.findScenarioExpressionIds(
            scenarioId, userLocale.targetLocale(), userLocale.baseLocale(), contentLevel);
    Set<Long> completedExpressionIds =
        expressionCompletionService.findCompletedExpressionIds(userId, scenarioId).values();
    int completedExpressionCount =
        (int) expressionIds.stream().filter(completedExpressionIds::contains).count();
    return new ExpressionProgress(expressionIds.size(), completedExpressionCount);
  }
}
