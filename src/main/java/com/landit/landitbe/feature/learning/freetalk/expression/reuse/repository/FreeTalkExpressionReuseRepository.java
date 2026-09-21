// 스몰톡에서 배운 표현을 다시 쓴 기록을 저장하고 조회한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.repository;

import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스몰톡에서 배운 표현을 다시 쓴 기록을 저장하고 조회한다. */
public interface FreeTalkExpressionReuseRepository
    extends JpaRepository<FreeTalkExpressionReuse, Long> {

  /**
   * 세션 하나의 재사용 기록을 저장된 순서로 조회한다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   * @return 그 세션에서 배운 표현을 다시 쓴 기록
   */
  List<FreeTalkExpressionReuse> findByFreeTalkSessionIdOrderByIdAsc(Long freeTalkSessionId);

  /**
   * 세션에 재사용 기록이 이미 있는지 확인한다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   * @return 하나라도 있으면 true
   */
  boolean existsByFreeTalkSessionId(Long freeTalkSessionId);
}
