// 사용자가 완료한 Writing 표현 기록을 사용자와 시나리오 기준으로 조회한다.

package com.landit.landitbe.feature.learning.repository;

import com.landit.landitbe.feature.learning.domain.UserWritingExpressionCompletion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자가 완료한 Writing 표현 기록을 사용자와 시나리오 기준으로 조회한다. */
public interface UserWritingExpressionCompletionRepository
    extends JpaRepository<UserWritingExpressionCompletion, Long> {

  /** 특정 사용자가 특정 시나리오에서 완료한 Writing 표현 기록을 모두 조회한다. */
  List<UserWritingExpressionCompletion> findAllByUserProfileIdAndScenarioId(
      Long userProfileId, Long scenarioId);

  /**
   * 사용자가 특정 표현을 완료한 이력을 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param writingExpressionId 표현 ID
   * @return 완료 이력. 없으면 빈 Optional
   */
  Optional<UserWritingExpressionCompletion> findByUserProfileIdAndWritingExpressionId(
      Long userProfileId, Long writingExpressionId);

  /**
   * 사용자가 특정 표현 목록에서 완료한 이력을 한 번에 조회한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param writingExpressionIds 표현 ID 목록
   * @return 완료 이력 목록
   */
  List<UserWritingExpressionCompletion> findAllByUserProfileIdAndWritingExpressionIdIn(
      Long userProfileId, Collection<Long> writingExpressionIds);
}
