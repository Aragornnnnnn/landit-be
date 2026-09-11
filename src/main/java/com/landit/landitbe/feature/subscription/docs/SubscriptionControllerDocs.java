// 구독 상태와 결제 이력 조회 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.subscription.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.subscription.dto.SubscriptionEventResponse;
import com.landit.landitbe.feature.subscription.dto.UserSubscriptionResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

/** 구독 상태와 결제 이력 조회 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Subscription", description = "구독 상태·결제 이력 API")
public interface SubscriptionControllerDocs {

  /**
   * 인증된 사용자의 서버 기준 구독 상태와 페이월 판단 근거를 조회한다.
   *
   * @param principal 인증된 사용자
   * @return 구독 상태, 프리미엄 적용 여부, 결제 기간 종류, 만료 시각, 도입 이후 대화 완료 여부, 상품 ID, 스토어
   */
  @Operation(
      summary = "사용자 구독 상태 조회",
      description =
          "RevenueCat 웹훅으로 갱신된 서버 기준 구독 상태를 조회합니다. premium이 true면 프리미엄 혜택이 적용 중이고,"
              + " conversationCompletedSinceLaunch && !premium 이면 앱은 페이월을 보여줍니다. productId와 store는"
              + " 프리미엄이 켜져 있을 때만 값이 있습니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<UserSubscriptionResponse> getSubscription(AuthUserPrincipal principal);

  /**
   * 인증된 사용자의 구독 결제 이력을 최근 순으로 조회한다.
   *
   * @param principal 인증된 사용자
   * @return 발생 시각 내림차순 결제 이력 최대 50개
   */
  @Operation(
      summary = "사용자 구독 결제 이력 조회",
      description =
          "RevenueCat 웹훅으로 저장한 결제 이력을 occurredAt 내림차순으로 최근 50개까지 조회합니다. 페이지는 없습니다."
              + " 체험·프로모션·해지처럼 결제가 없는 이벤트는 price가 0이고, environment가 SANDBOX면 테스트 결제입니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<List<SubscriptionEventResponse>> getSubscriptionEvents(AuthUserPrincipal principal);
}
