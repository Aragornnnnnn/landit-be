// develop 환경의 관리자 시나리오 테스트 세션 시작을 처리한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.dto.SessionStartResponse;
import com.landit.landitbe.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 개발 환경의 관리자 시나리오 테스트 세션 시작을 처리한다. */
@Profile("develop")
@RequiredArgsConstructor
@Service
public class AdminScenarioSessionStartService {

  private final ScenarioSessionStartService scenarioSessionStartService;

  /**
   * 일반 사용자 진행 제한 없이 활성 시나리오의 테스트 세션을 시작한다.
   *
   * @param userId 관리자 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 생성된 세션과 첫 메시지 정보
   * @throws ApiException 사용자가 비활성이거나 콘텐츠가 비활성 상태이거나 시작 데이터가 유효하지 않을 때
   */
  @Transactional
  public SessionStartResponse startScenarioSession(long userId, long scenarioId) {
    return scenarioSessionStartService.startScenarioSessionWithoutProgression(userId, scenarioId);
  }
}
