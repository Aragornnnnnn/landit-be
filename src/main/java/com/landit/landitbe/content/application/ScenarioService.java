// 시나리오 도메인의 조회와 존재 검증을 담당한다.
package com.landit.landitbe.content.application;

import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.content.infrastructure.ScenarioRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
