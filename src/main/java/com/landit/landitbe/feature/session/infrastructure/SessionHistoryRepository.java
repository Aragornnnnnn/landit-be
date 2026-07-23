// 세션 히스토리 엔티티의 저장을 담당한다.

package com.landit.landitbe.feature.session.infrastructure;

import com.landit.landitbe.feature.session.domain.SessionHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 세션 히스토리 엔티티의 저장을 담당한다. */
public interface SessionHistoryRepository extends JpaRepository<SessionHistory, Long> {

  /** 학습 세션 ID로 세션 히스토리를 조회한다. */
  Optional<SessionHistory> findByLearningSessionId(Long learningSessionId);
}
