// 사용자별 프리톡 표현 완료 정보를 저장하고 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.UserFreeTalkExpressionCompletion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자별 프리톡 표현 완료 정보를 저장하고 조회한다. */
public interface UserFreeTalkExpressionCompletionRepository
    extends JpaRepository<UserFreeTalkExpressionCompletion, Long> {

  /** 사용자가 완료한 표현 중 요청한 표현 ID에 해당하는 완료 기록을 조회한다. */
  List<UserFreeTalkExpressionCompletion> findByUserProfileIdAndFreeTalkExpressionIdIn(
      Long userProfileId, Collection<Long> freeTalkExpressionIds);

  /** 사용자가 프리톡 공통 표현을 완료했는지 확인한다. */
  boolean existsByUserProfileIdAndFreeTalkExpressionId(
      Long userProfileId, Long freeTalkExpressionId);
}
