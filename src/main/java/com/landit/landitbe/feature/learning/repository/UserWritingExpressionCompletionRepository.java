// 사용자가 완료한 Writing 표현 기록을 사용자와 시나리오 기준으로 조회한다.

package com.landit.landitbe.feature.learning.repository;

import com.landit.landitbe.feature.learning.domain.ExpressionLearningSource;
import com.landit.landitbe.feature.learning.domain.UserWritingExpressionCompletion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자가 완료한 Writing 표현 기록을 사용자와 시나리오 기준으로 조회한다. */
public interface UserWritingExpressionCompletionRepository
    extends JpaRepository<UserWritingExpressionCompletion, Long> {

  /**
   * 사용자가 시나리오에서 특정 경로로 완료한 Writing 표현 기록을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param scenarioId 시나리오 ID
   * @param learningSource 학습 경로
   * @return 조건에 맞는 완료 기록 목록
   */
  List<UserWritingExpressionCompletion> findAllByUserProfileIdAndScenarioIdAndLearningSource(
      Long userProfileId, Long scenarioId, ExpressionLearningSource learningSource);

  /**
   * 사용자가 특정 경로로 표현을 완료한 이력을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param writingExpressionId Writing 표현 ID
   * @param learningSource 학습 경로
   * @return 조건에 맞는 완료 기록
   */
  Optional<UserWritingExpressionCompletion>
      findByUserProfileIdAndWritingExpressionIdAndLearningSource(
          Long userProfileId, Long writingExpressionId, ExpressionLearningSource learningSource);

  /**
   * 사용자가 표현 목록에서 완료한 모든 이력을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param writingExpressionIds Writing 표현 ID 목록
   * @return 학습 경로와 관계없이 조건에 맞는 완료 기록 목록
   */
  List<UserWritingExpressionCompletion> findAllByUserProfileIdAndWritingExpressionIdIn(
      Long userProfileId, Collection<Long> writingExpressionIds);

  /**
   * 사용자가 특정 경로로 완료한 표현 목록을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param writingExpressionIds Writing 표현 ID 목록
   * @param learningSource 학습 경로
   * @return 조건에 맞는 완료 기록 목록
   */
  List<UserWritingExpressionCompletion>
      findAllByUserProfileIdAndWritingExpressionIdInAndLearningSource(
          Long userProfileId,
          Collection<Long> writingExpressionIds,
          ExpressionLearningSource learningSource);
}
