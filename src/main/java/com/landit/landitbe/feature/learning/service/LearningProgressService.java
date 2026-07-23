// 사용자별 시나리오와 표현 학습 진행 상태를 조회하고 변경한다.

package com.landit.landitbe.feature.learning.service;

import com.landit.landitbe.feature.learning.domain.UserScenarioProgress;
import com.landit.landitbe.feature.learning.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.feature.learning.dto.CompletedExpressionIds;
import com.landit.landitbe.feature.learning.repository.UserScenarioProgressRepository;
import com.landit.landitbe.feature.learning.repository.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자별 시나리오와 표현 학습 진행 상태를 조회하고 변경한다. */
@Service
@RequiredArgsConstructor
public class LearningProgressService {

  private final UserScenarioProgressRepository userScenarioProgressRepository;
  private final UserWritingExpressionCompletionRepository expressionCompletionRepository;

  /** 특정 시나리오에서 완료한 표현 엔티티를 기능 내부에서 조회한다. */
  private List<UserWritingExpressionCompletion> findExpressionCompletions(
      Long userId, Long scenarioId) {
    return expressionCompletionRepository.findAllByUserProfileIdAndScenarioId(userId, scenarioId);
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
   * 시나리오 시작 진행도를 생성하거나 최근 시작 시각을 갱신한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 locale
   * @param startedAt 시작 시각
   */
  @Transactional
  public void startScenario(
      Long userId, Long scenarioId, Locale targetLocale, LocalDateTime startedAt) {
    userScenarioProgressRepository
        .findByUserProfileIdAndScenarioIdAndTargetLocale(userId, scenarioId, targetLocale)
        .ifPresentOrElse(
            progress -> progress.markStarted(startedAt),
            () ->
                userScenarioProgressRepository.save(
                    UserScenarioProgress.start(userId, scenarioId, targetLocale, startedAt)));
  }

  /**
   * 시나리오 완료 결과와 최고 성과를 갱신한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param targetLocale 학습 대상 locale
   * @param starRating 별점
   * @param nativeScore 원어민 유사도 점수
   * @param endedAt 종료 시각
   * @throws ApiException 시작 진행도가 없을 때
   */
  @Transactional
  public void completeScenario(
      Long userId,
      Long scenarioId,
      Locale targetLocale,
      BigDecimal starRating,
      int nativeScore,
      LocalDateTime endedAt) {
    UserScenarioProgress progress =
        userScenarioProgressRepository
            .findByUserProfileIdAndScenarioIdAndTargetLocale(userId, scenarioId, targetLocale)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    progress.complete(starRating, nativeScore, endedAt);
  }
}
