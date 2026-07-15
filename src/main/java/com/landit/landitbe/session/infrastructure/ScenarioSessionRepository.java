// 시나리오 세션 보조 엔티티의 저장을 담당한다.
package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.session.domain.ScenarioSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioSessionRepository extends JpaRepository<ScenarioSession, Long> {

  /** 학습 세션 ID로 시나리오 세션 보조 정보를 조회한다. */
  Optional<ScenarioSession> findByLearningSessionId(Long learningSessionId);
}
