// 인증된 사용자의 구독 상태 조회 요청을 처리한다.

package com.landit.landitbe.feature.subscription;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.subscription.docs.SubscriptionControllerDocs;
import com.landit.landitbe.feature.subscription.dto.UserSubscriptionResponse;
import com.landit.landitbe.feature.subscription.service.UserSubscriptionService;
import com.landit.landitbe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증된 사용자의 구독 상태 조회 요청을 처리한다. */
@RestController
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionControllerDocs {

  private final UserSubscriptionService userSubscriptionService;

  /** {@inheritDoc} */
  @Override
  @GetMapping("/api/v1/me/subscription")
  public ApiResponse<UserSubscriptionResponse> getSubscription(
      @AuthenticationPrincipal AuthUserPrincipal principal) {
    return ApiResponse.success(userSubscriptionService.getSubscription(principal.userId()));
  }
}
