// 시나리오 별점 문구를 다른 기능에 조회해 준다.

package com.landit.landitbe.feature.content.scenario.service;

import com.landit.landitbe.feature.content.scenario.domain.ScenarioStarMessage;
import com.landit.landitbe.feature.content.scenario.repository.ScenarioStarMessageRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 별점 문구를 다른 기능에 조회해 준다. */
@RequiredArgsConstructor
@Service
public class ScenarioStarMessageService {

  private final ScenarioStarMessageRepository scenarioStarMessageRepository;

  /**
   * 시나리오와 별점 조합에 지정된 최종 피드백 문구를 조회한다.
   *
   * @param scenarioId 시나리오 ID
   * @param starRating 세션 별점 (1.0, 1.5, 2.0, 2.5, 3.0)
   * @return 지정된 문구. 조합에 등록된 문구가 없으면 빈 Optional
   */
  @Transactional(readOnly = true)
  public Optional<String> findMessage(long scenarioId, BigDecimal starRating) {
    return scenarioStarMessageRepository
        .findByScenarioIdAndStarRating(scenarioId, starRating)
        .map(ScenarioStarMessage::getMessage);
  }
}
