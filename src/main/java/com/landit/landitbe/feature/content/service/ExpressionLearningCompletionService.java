// 원어민 표현 학습 완료 흐름을 처리한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.learning.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.feature.learning.dto.CompletedExpressionIds;
import com.landit.landitbe.feature.learning.repository.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 원어민 표현 학습 완료 흐름을 처리한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressionLearningCompletionService {

  private static final String LOCKED_EXPRESSION_LOG =
      "표현 학습 완료 실패: 아직 잠긴 표현입니다. userId={}, expressionId={}";

  private final WritingExpressionRepository writingExpressionRepository;
  private final UserProfileService userProfileService;
  private final LearningProgressService learningProgressService;
  private final UserWritingExpressionCompletionRepository expressionCompletionRepository;

  /**
   * 학습 순서에 맞는 활성 표현의 완료 이력을 생성하거나 완료 시각을 갱신한다.
   *
   * @param userId 학습 사용자 ID
   * @param expressionId 완료할 표현 ID
   * @throws ApiException 표현이 없거나 아직 잠겨 있거나 다른 사용자의 전용 표현일 때
   */
  @Transactional
  public void completeLearning(Long userId, Long expressionId) {
    WritingExpression expression =
        writingExpressionRepository
            .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    Long scenarioId = expression.getScenarioId();
    if (expression.isOwnedByAnother(userId)) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    if (scenarioId == null) {
      completeFreeTalkExpression(userId, expressionId);
      return;
    }

    CompletedExpressionIds completedExpressionIds =
        learningProgressService.findCompletedExpressionIds(userId, scenarioId);

    // 이미 완료한 표현은 마지막 완료 시각만 갱신한다.
    if (completedExpressionIds.values().contains(expressionId)) {
      learningProgressService.completeExpression(userId, scenarioId, expressionId);
      return;
    }

    // 시나리오 학습 순서에 따라 잠금 여부를 검증한다.
    if (!isUnlockedExpression(userId, scenarioId, expressionId, completedExpressionIds.values())) {
      log.warn(LOCKED_EXPRESSION_LOG, userId, expressionId);
      throw new ApiException(ErrorCode.EXPRESSION_LOCKED);
    }

    // 잠금이 해제된 표현의 완료 이력을 저장한다.
    learningProgressService.completeExpression(userId, scenarioId, expressionId);
    log.info("expression learning completed: userId={}, expressionId={}", userId, expressionId);
  }

  // 프리톡 표현의 완료 이력을 생성하거나 완료 시각을 갱신한다.
  private void completeFreeTalkExpression(Long userId, Long expressionId) {
    expressionCompletionRepository
        .findByUserProfileIdAndWritingExpressionId(userId, expressionId)
        .ifPresentOrElse(
            UserWritingExpressionCompletion::markCompletedAgain,
            () ->
                expressionCompletionRepository.save(
                    new UserWritingExpressionCompletion(userId, null, expressionId)));
  }

  /** 사용자 로케일과 학습 순서로 표현의 잠금 해제 여부를 판단한다. */
  private boolean isUnlockedExpression(
      Long userId, Long scenarioId, Long expressionId, Set<Long> completedExpressionIds) {
    // 사용자의 학습 언어와 기준 언어를 조회한다.
    UserLocale userLocale = userProfileService.getUserLocale(userId);

    // 사용자 로케일에 맞는 활성 표현을 노출 순서대로 조회한다.
    List<WritingExpression> expressions =
        writingExpressionRepository
            .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
                scenarioId,
                userLocale.targetLocale(),
                userLocale.baseLocale(),
                ActiveStatus.ACTIVE);

    // 가장 앞선 미완료 표현의 ID를 찾는다.
    Optional<Long> firstIncompleteExpressionId =
        expressions.stream()
            .map(WritingExpression::getId)
            .filter(id -> !completedExpressionIds.contains(id))
            .findFirst();

    return firstIncompleteExpressionId.isPresent()
        && firstIncompleteExpressionId.get().equals(expressionId);
  }
}
