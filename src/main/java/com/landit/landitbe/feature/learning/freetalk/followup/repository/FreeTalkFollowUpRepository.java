// 스몰톡 후속 질문을 저장하고 조회한다.

package com.landit.landitbe.feature.learning.freetalk.followup.repository;

import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 스몰톡 후속 질문을 저장하고 조회한다. */
public interface FreeTalkFollowUpRepository extends JpaRepository<FreeTalkFollowUp, Long> {

  /**
   * 세션이 끝난 뒤 만들어진 후속 질문을 조회한다.
   *
   * @param freeTalkSessionId 프리톡 세션 ID
   * @return 그 세션의 후속 질문. 아직 만들어지지 않았으면 비어 있다
   */
  Optional<FreeTalkFollowUp> findByFreeTalkSessionId(Long freeTalkSessionId);

  /**
   * 사용자가 지금까지 받은 후속 질문들이 근거로 쓴 장기기억 ID를 중복 없이 조회한다.
   *
   * <p>같은 기억으로 만든 질문이 요약에 되풀이되지 않도록 다음 질문 생성에서 빼는 데 쓴다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 이미 질문의 근거로 쓴 장기기억 ID 목록
   */
  @Query(
      """
      select distinct followUp.memoryId from FreeTalkFollowUp followUp
      where followUp.userProfileId = :userProfileId and followUp.memoryId is not null
      order by followUp.memoryId
      """)
  List<Long> findUsedMemoryIds(@Param("userProfileId") long userProfileId);
}
