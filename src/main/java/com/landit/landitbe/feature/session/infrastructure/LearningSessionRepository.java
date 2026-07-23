// 학습 세션 엔티티의 조회와 저장을 담당한다.

package com.landit.landitbe.feature.session.infrastructure;

import com.landit.landitbe.feature.session.domain.LearningSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 학습 세션 엔티티의 조회와 저장을 담당한다. */
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

  /** 특정 사용자가 소유한 학습 세션을 조회한다. */
  Optional<LearningSession> findByIdAndUserProfileId(Long id, Long userProfileId);

  /** 같은 세션에 대한 동시 메시지 제출을 직렬화하며 소유 세션을 조회한다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select learningSession
            from LearningSession learningSession
            where learningSession.id = :id
              and learningSession.userProfileId = :userProfileId
      """)
  Optional<LearningSession> findByIdAndUserProfileIdForUpdate(
      @Param("id") Long id, @Param("userProfileId") Long userProfileId);
}
