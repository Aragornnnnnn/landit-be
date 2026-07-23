// 원어민 표현 학습 완료 유스케이스를 처리한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.auth.service.UserLocale;
import com.landit.landitbe.feature.auth.service.UserProfileService;
import com.landit.landitbe.feature.content.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.repository.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 원어민 표현 학습 완료 유스케이스를 처리한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteExpressionLearningUseCase {

  private static final String LOCKED_EXPRESSION_LOG =
      "표현 학습 완료 실패: 아직 잠긴 표현입니다. userId={}, expressionId={}";

  private final WritingExpressionRepository writingExpressionRepository;
  private final UserProfileService userProfileService;
  private final UserWritingExpressionCompletionRepository userWritingExpressionCompletionRepository;

  /**
   * 표현 학습 완료를 기록한다. 표현이 없거나 INACTIVE면 RESOURCE_NOT_FOUND. 이미 완료한 표현 -> lastCompletedAt 갱신 완료 안한 표현
   * -> 2가지 분기를 탐. 1. 잠겨있으면 EXPRESSION_LOCKED 에러, 2. 잠겨있지 않으면 완료 기록 생성후 DB에 저장.
   */
  @Transactional
  public void completeLearning(Long userId, Long expressionId) {
    WritingExpression expression =
        writingExpressionRepository
            .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ApiException(
                        ErrorCode
                            .RESOURCE_NOT_FOUND)); // 해당 표현이 없거나 INACTIVE면 RESOURCE_NOT_FOUND 터트리고
    // 끝.
    Long scenarioId = expression.getScenarioId();

    List<UserWritingExpressionCompletion> completions =
        userWritingExpressionCompletionRepository.findAllByUserProfileIdAndScenarioId(
            userId, scenarioId); // 유저가 해당 시나리오 관련 표현들 중에서 학습 완료한 것들을 리스트로 가져옴.

    // completions 리스트에서, writingExpressionId == expressionId인 걸 찾아서, 첫 번째로 찾은 걸 가져와라 (없으면 없다고 해라)
    Optional<UserWritingExpressionCompletion> existingCompletion =
        completions.stream()
            .filter(completion -> completion.getWritingExpressionId().equals(expressionId))
            .findFirst();

    // 1. 만약에 이미 완료한 표현 리스트에 있는 경우라면, lastCompletedAt만 갱신하고 끝내라.
    if (existingCompletion.isPresent()) {
      existingCompletion.get().markCompletedAgain();
      return;
    }

    // 아직 완료 안한 표현이면, 완료한 표현 id를 이 set에 담는다.
    Set<Long> completedExpressionIds =
        completions.stream()
            .map(UserWritingExpressionCompletion::getWritingExpressionId)
            .collect(Collectors.toSet());

    // 2. lock 여부 확인 -> 만약 잠겨있다면 에러를 터트린다.
    if (!isUnlockedExpression(userId, scenarioId, expressionId, completedExpressionIds)) {
      log.warn(LOCKED_EXPRESSION_LOG, userId, expressionId);
      throw new ApiException(ErrorCode.EXPRESSION_LOCKED);
    }

    // 3. 잠겨있지 않으면 완료 기록을 새로 생성해서 저장한다.
    userWritingExpressionCompletionRepository.save(
        new UserWritingExpressionCompletion(userId, scenarioId, expressionId));
  }

  /** 해당 표현이 지금 학습할 차례가 맞는지(=unlock상태인지) 사용자 locale 기준으로 판정한다. */
  private boolean isUnlockedExpression(
      Long userId, Long scenarioId, Long expressionId, Set<Long> completedExpressionIds) {
    // 사용자의 타겟 언어, 기준 언어를 가져온다.
    UserLocale userLocale = userProfileService.getUserLocale(userId);

    // 사용자 locale 기준으로 시나리오에 속한 활성 표현들을 displayOrder 순서대로 가져온다.
    List<WritingExpression> expressions =
        writingExpressionRepository
            .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
                scenarioId,
                userLocale.targetLocale(),
                userLocale.baseLocale(),
                ActiveStatus.ACTIVE);

    // firstIncompleteExpressionId = 가장 첫번째 미완료 표현의 id
    Optional<Long> firstIncompleteExpressionId =
        expressions.stream()
            .map(WritingExpression::getId)
            .filter(id -> !completedExpressionIds.contains(id))
            .findFirst();

    return firstIncompleteExpressionId.isPresent()
        && firstIncompleteExpressionId.get().equals(expressionId);
  }
}
