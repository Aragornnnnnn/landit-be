// 스몰톡 총평을 저장하고 조회한다.

package com.landit.landitbe.feature.learning.freetalk.summary.repository;

import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkSessionSummary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 스몰톡 총평을 저장하고 조회한다. 총평은 세션마다 하나이며 삽입만 하고 고치지 않는다. */
@Repository
public interface FreeTalkSessionSummaryRepository
    extends JpaRepository<FreeTalkSessionSummary, Long> {

  /**
   * 세션의 총평을 조회한다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   * @return 저장된 총평. 아직 계산 전이면 비어 있다
   */
  Optional<FreeTalkSessionSummary> findByFreeTalkSessionId(Long freeTalkSessionId);
}
