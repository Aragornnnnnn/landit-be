// 시나리오 도메인의 조회와 존재 검증을 담당한다.

package com.landit.landitbe.feature.content.application;

import com.landit.landitbe.feature.content.infrastructure.ScenarioRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 도메인의 조회와 존재 검증을 담당한다. */
@Service
@AllArgsConstructor
public class ScenarioService {

  private final ScenarioRepository scenarioRepository;

  /** 시나리오가 존재하지 않으면 SCENARIO_NOT_FOUND 예외를 던진다. */
  @Transactional(readOnly = true)
  public void validateExists(Long scenarioId) {
    if (!scenarioRepository.existsById(scenarioId)) {
      throw new ApiException(ErrorCode.SCENARIO_NOT_FOUND);
    }
  }
}
