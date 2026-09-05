// RevenueCat이 보내는 구독 이벤트 웹훅을 수신한다.

package com.landit.landitbe.feature.subscription;

import com.landit.landitbe.feature.subscription.dto.RevenueCatWebhookRequest;
import com.landit.landitbe.feature.subscription.service.RevenueCatWebhookService;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * RevenueCat이 보내는 구독 이벤트 웹훅을 수신한다.
 *
 * <p>앱 클라이언트가 호출하는 API가 아니므로 OpenAPI 문서에서 숨긴다.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class RevenueCatWebhookController {

  private final RevenueCatWebhookService revenueCatWebhookService;

  /**
   * RevenueCat 웹훅 이벤트를 받아 사용자 구독 상태에 반영한다.
   *
   * <p>RevenueCat은 200이 아닌 응답을 실패로 보고 최대 5회 재시도한다.
   *
   * @param authorization RevenueCat 대시보드에 등록한 Authorization 헤더 값
   * @param request 웹훅 요청 본문
   * @return 처리 성공 응답
   */
  @PostMapping("/webhooks/revenuecat")
  public ApiResponse<Void> receiveRevenueCatWebhook(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @Valid @RequestBody RevenueCatWebhookRequest request) {
    revenueCatWebhookService.handle(authorization, request);
    return ApiResponse.success(null);
  }
}
