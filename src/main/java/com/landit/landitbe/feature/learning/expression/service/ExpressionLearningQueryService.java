// 표현 콘텐츠에 사용자별 완료 이력과 순서 잠금을 적용한다.

package com.landit.landitbe.feature.learning.expression.service;

import com.landit.landitbe.feature.content.expression.dto.ExpressionResponse;
import com.landit.landitbe.feature.learning.expression.progress.service.ExpressionCompletionService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 표현 콘텐츠에 사용자별 완료 이력과 순서 잠금을 적용한다. */
@Service
@RequiredArgsConstructor
public class ExpressionLearningQueryService {

  private final ExpressionLearningContentService expressionQueryService;
  private final ExpressionCompletionService expressionCompletionService;

  /**
   * 시나리오 표현 콘텐츠에 사용자의 완료와 순서 잠금을 적용한다.
   *
   * @param userId 학습 사용자
   * @param scenarioId 시나리오 ID
   * @return 완료와 잠금 상태가 반영된 표현 목록
   */
  @Transactional(readOnly = true)
  public List<ExpressionResponse> getExpressionsPerScenario(Long userId, Long scenarioId) {
    List<ExpressionResponse> expressions =
        expressionQueryService.getScenarioExpressions(userId, scenarioId);
    Set<Long> completedIds =
        expressionCompletionService.findCompletedExpressionIds(userId, scenarioId).values();
    Long firstIncompleteId =
        expressions.stream()
            .map(ExpressionResponse::expressionId)
            .filter(id -> !completedIds.contains(id))
            .findFirst()
            .orElse(null);
    return expressions.stream()
        .map(
            expression -> {
              boolean completed = completedIds.contains(expression.expressionId());
              return new ExpressionResponse(
                  expression.expressionId(),
                  expression.displayOrder(),
                  expression.targetExpressionText(),
                  expression.baseExpressionMeaningText(),
                  completed,
                  !completed && !expression.expressionId().equals(firstIncompleteId));
            })
        .toList();
  }
}
