// 시나리오 별점 문구를 시나리오와 별점 기준으로 조회한다.

package com.landit.landitbe.feature.content.scenario.repository;

import com.landit.landitbe.feature.content.scenario.domain.ScenarioStarMessage;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 시나리오 별점 문구를 시나리오와 별점 기준으로 조회한다. */
public interface ScenarioStarMessageRepository extends JpaRepository<ScenarioStarMessage, Long> {

  /** 시나리오와 별점 조합의 문구를 조회한다. */
  Optional<ScenarioStarMessage> findByScenarioIdAndStarRating(
      Long scenarioId, BigDecimal starRating);
}
