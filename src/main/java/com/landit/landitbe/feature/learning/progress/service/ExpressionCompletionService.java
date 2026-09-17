// 학습 출처별 표현 완료 이력을 조회하고 변경한다.

package com.landit.landitbe.feature.learning.progress.service;

import com.landit.landitbe.feature.learning.progress.domain.ExpressionLearningSource;
import com.landit.landitbe.feature.learning.progress.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.feature.learning.progress.dto.CompletedExpressionIds;
import com.landit.landitbe.feature.learning.progress.repository.UserWritingExpressionCompletionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학습 출처별 표현 완료 이력을 조회하고 변경한다. */
@Service
@RequiredArgsConstructor
public class ExpressionCompletionService {

  private final UserWritingExpressionCompletionRepository expressionCompletionRepository;

  /** 특정 시나리오에서 완료한 표현 엔티티를 기능 내부에서 조회한다. */
  private List<UserWritingExpressionCompletion> findExpressionCompletions(
      Long userId, Long scenarioId) {
    return expressionCompletionRepository.findAllByUserProfileIdAndScenarioIdAndLearningSource(
        userId, scenarioId, ExpressionLearningSource.SCENARIO);
  }

  /**
   * 사용자가 특정 시나리오에서 완료한 표현 ID를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 완료한 표현 ID 집합
   */
  @Transactional(readOnly = true)
  public CompletedExpressionIds findCompletedExpressionIds(Long userId, Long scenarioId) {
    return CompletedExpressionIds.from(findExpressionCompletions(userId, scenarioId));
  }

  /**
   * 사용자가 표현을 완료한 적이 있는지 학습 경로와 관계없이 확인한다.
   *
   * <p>표현은 {@code expression_source}로 시나리오·프리톡 중 한쪽에만 속하므로 완료 이력도 자기 경로로만 남는다. 경로를 나눠 조회하지 않고 존재
   * 여부만 확인한다. 시나리오 표현이라면 시나리오 목록의 완료 여부({@link #findCompletedExpressionIds})와 같은 값이다.
   *
   * @param userId 사용자 ID
   * @param expressionId 표현 ID
   * @return 완료 이력이 있으면 true
   */
  @Transactional(readOnly = true)
  public boolean hasCompletedExpression(Long userId, Long expressionId) {
    return expressionCompletionRepository.existsByUserProfileIdAndWritingExpressionId(
        userId, expressionId);
  }

  /**
   * 표현을 처음 완료하거나 기존 완료 시각을 갱신한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param expressionId 표현 ID
   */
  @Transactional
  public void completeExpression(Long userId, Long scenarioId, Long expressionId) {
    findExpressionCompletions(userId, scenarioId).stream()
        .filter(completion -> completion.getWritingExpressionId().equals(expressionId))
        .findFirst()
        .ifPresentOrElse(
            UserWritingExpressionCompletion::markCompletedAgain,
            () ->
                expressionCompletionRepository.save(
                    new UserWritingExpressionCompletion(userId, scenarioId, expressionId)));
  }

  /**
   * 프리톡 출처의 표현 완료 이력을 생성하거나 마지막 완료 시각을 갱신한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 표현이 연결된 시나리오 ID. 프리톡 개인 표현이면 null
   * @param expressionId 표현 ID
   */
  @Transactional
  public void completeFreeTalkExpression(Long userId, Long scenarioId, Long expressionId) {
    expressionCompletionRepository
        .findByUserProfileIdAndWritingExpressionIdAndLearningSource(
            userId, expressionId, ExpressionLearningSource.FREE_TALK)
        .ifPresentOrElse(
            UserWritingExpressionCompletion::markCompletedAgain,
            () ->
                expressionCompletionRepository.save(
                    new UserWritingExpressionCompletion(
                        userId, scenarioId, expressionId, ExpressionLearningSource.FREE_TALK)));
  }
}
