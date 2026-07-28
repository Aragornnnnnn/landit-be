// 프리톡 세션 엔티티를 저장하고 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 프리톡 세션 엔티티를 저장하고 조회한다. */
public interface FreeTalkSessionRepository extends JpaRepository<FreeTalkSession, Long> {

  /** 학습 세션에 연결된 프리톡 세션을 조회한다. */
  Optional<FreeTalkSession> findByLearningSessionId(Long learningSessionId);

  /** 학습 세션에 연결된 프리톡 세션을 상태 변경용으로 잠금 조회한다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select session from FreeTalkSession session "
          + "where session.learningSessionId = :learningSessionId")
  Optional<FreeTalkSession> findByLearningSessionIdForUpdate(
      @Param("learningSessionId") Long learningSessionId);

  /** 사용자가 소유한 완료 프리톡을 완료 시각 내림차순으로 조회한다. */
  @Query(
      """
          select freeTalkSession
          from FreeTalkSession freeTalkSession, LearningSession learningSession
          where learningSession.id = freeTalkSession.learningSessionId
            and learningSession.userProfileId = :userProfileId
            and learningSession.status = com.landit.landitbe.feature.session.domain.LearningSessionStatus.COMPLETED
            and freeTalkSession.conversationStatus = com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus.COMPLETED
          order by learningSession.endedAt desc
      """)
  Page<FreeTalkSession> findCompletedByUserProfileId(
      @Param("userProfileId") Long userProfileId, Pageable pageable);
}
