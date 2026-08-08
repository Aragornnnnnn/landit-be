// 관리자 시나리오 테스트 목록 조회를 처리한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.dto.AdminScenarioListResponse;
import com.landit.landitbe.feature.content.repository.AdminScenarioListQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 시나리오 테스트 목록 조회를 처리한다. */
@RequiredArgsConstructor
@Service
public class AdminScenarioQueryService {

  private final AdminScenarioListQueryRepository adminScenarioListQueryRepository;

  /**
   * 활성 콘텐츠만 관리자 테스트 목록으로 조회한다.
   *
   * @param userId 관리자 사용자 ID
   * @return 관리자 테스트용 시나리오 목록
   */
  @Transactional(readOnly = true)
  public AdminScenarioListResponse getAdminScenarioList(long userId) {
    return AdminScenarioListResponse.from(
        adminScenarioListQueryRepository.findActiveScenarioList(userId));
  }
}
