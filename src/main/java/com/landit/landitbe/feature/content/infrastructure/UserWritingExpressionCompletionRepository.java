// 사용자가 완료한 Writing 표현 기록을 사용자와 시나리오 기준으로 조회한다.

package com.landit.landitbe.feature.content.infrastructure;

import com.landit.landitbe.feature.content.domain.UserWritingExpressionCompletion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자가 완료한 Writing 표현 기록을 사용자와 시나리오 기준으로 조회한다. */
public interface UserWritingExpressionCompletionRepository
    extends JpaRepository<UserWritingExpressionCompletion, Long> {

  /** 특정 사용자가 특정 시나리오에서 완료한 Writing 표현 기록을 모두 조회한다. */
  List<UserWritingExpressionCompletion> findAllByUserProfileIdAndScenarioId(
      Long userProfileId, Long scenarioId);
}
