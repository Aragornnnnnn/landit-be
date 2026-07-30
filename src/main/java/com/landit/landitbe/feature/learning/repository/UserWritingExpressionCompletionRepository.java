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

  /** 특정 사용자가 특정 시나리오에서 완료한 Writing 표현 기록을 모두 조회한다. */
  List<UserWritingExpressionCompletion> findAllByUserProfileIdAndScenarioIdAndLearningSource(
      Long userProfileId, Long scenarioId, ExpressionLearningSource learningSource);

  /** 사용자가 특정 표현을 완료한 이력을 조회한다. */
  Optional<UserWritingExpressionCompletion>
      findByUserProfileIdAndWritingExpressionIdAndLearningSource(
          Long userProfileId, Long writingExpressionId, ExpressionLearningSource learningSource);

  /** 사용자가 특정 표현 목록에서 완료한 이력을 한 번에 조회한다. */
  List<UserWritingExpressionCompletion> findAllByUserProfileIdAndWritingExpressionIdIn(
      Long userProfileId, Collection<Long> writingExpressionIds);

  /** 특정 출처에서 완료한 표현 목록을 한 번에 조회한다. */
  List<UserWritingExpressionCompletion>
      findAllByUserProfileIdAndWritingExpressionIdInAndLearningSource(
          Long userProfileId,
          Collection<Long> writingExpressionIds,
          ExpressionLearningSource learningSource);
}
