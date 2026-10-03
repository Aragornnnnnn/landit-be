// 스몰톡 후속 질문을 저장하고 조회한다.

package com.landit.landitbe.feature.learning.freetalk.followup.repository;

import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUp;
import com.landit.landitbe.feature.learning.freetalk.followup.dto.FreeTalkFollowUpCandidate;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
   * 같은 사용자의 최근 완료 스몰톡에서 나온 예고 질문을 캐릭터와 함께 읽는다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param since 오래된 질문을 제외할 기준 시각
   * @return 최근 질문부터 정렬된 후보
   */
  @Query(
      """
      select new com.landit.landitbe.feature.learning.freetalk.followup.dto.FreeTalkFollowUpCandidate(
          followUp, session.characterId)
      from FreeTalkFollowUp followUp, FreeTalkSession session, LearningSession learningSession
      where followUp.userProfileId = :userProfileId
        and followUp.freeTalkSessionId = session.id
        and session.learningSessionId = learningSession.id
        and learningSession.status = com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus.COMPLETED
        and followUp.createdAt >= :since
        and followUp.triggerType <> com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUpTriggerType.NONE
        and followUp.askedAt is null
      order by followUp.createdAt desc, followUp.id desc
      """)
  List<FreeTalkFollowUpCandidate> findRecentCandidates(
      @Param("userProfileId") long userProfileId, @Param("since") LocalDateTime since);

  /**
   * 질문의 선점과 사용 상태를 갱신하려고 잠금 조회한다.
   *
   * @param followUpId 질문 ID
   * @return 잠근 질문
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select followUp from FreeTalkFollowUp followUp where followUp.id = :followUpId")
  Optional<FreeTalkFollowUp> findByIdForUpdate(@Param("followUpId") long followUpId);

  /**
   * 실패한 세션이 선점했던 질문을 잠금 조회한다.
   *
   * @param freeTalkSessionId 실패한 프리톡 세션 ID
   * @return 선점한 질문
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<FreeTalkFollowUp> findByClaimedFreeTalkSessionId(Long freeTalkSessionId);

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
