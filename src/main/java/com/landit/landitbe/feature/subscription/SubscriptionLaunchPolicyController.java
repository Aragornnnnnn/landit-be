// 기존 관리자 인증 아래에서 구독 공개 정책을 조회하고 변경한다.

package com.landit.landitbe.feature.subscription;

import com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService;
import com.landit.landitbe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** AdminAuthorizationFilter가 보호하는 공개 정책 API다. */
@RestController
@RequiredArgsConstructor
public class SubscriptionLaunchPolicyController {
  private final SubscriptionLaunchPolicyService service;

  /**
   * 현재 공개 정책과 변경 버전을 조회한다.
   *
   * @return 관리자용 정책과 현재 버전
   */
  @GetMapping("/api/v1/admin/subscription-policy")
  public ApiResponse<SubscriptionLaunchPolicyService.Policy> get() {
    return ApiResponse.success(service.current());
  }

  /**
   * 검증된 새 정책을 저장해 이후 요청부터 적용한다.
   *
   * @param change 예상 버전과 새 공개 정책
   * @return 저장한 정책과 갱신 버전
   */
  @PutMapping("/api/v1/admin/subscription-policy")
  public ApiResponse<SubscriptionLaunchPolicyService.Policy> update(
      @RequestBody SubscriptionLaunchPolicyService.Change change) {
    return ApiResponse.success(service.update(change));
  }
}
