// 학습 세션 엔티티의 조회와 저장을 담당한다.
package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.session.domain.LearningSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    /** 특정 사용자가 소유한 학습 세션을 조회한다. */
    Optional<LearningSession> findByIdAndUserProfileId(Long id, Long userProfileId);
}
