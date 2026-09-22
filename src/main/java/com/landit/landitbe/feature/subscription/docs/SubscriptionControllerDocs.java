// 구독 상태와 결제 이력 조회 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.subscription.docs;

import com.landit.landitbe.feature.subscription.dto.PaywallDismissResponse;
import com.landit.landitbe.feature.subscription.dto.UserSubscriptionResponse;
import com.landit.landitbe.feature.subscription.event.dto.SubscriptionEventResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

/** 구독 상태와 결제 이력 조회 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Subscription", description = "구독 상태·결제 이력 API")
public interface SubscriptionControllerDocs {

  /**
   * 최초 페이월 이탈에서만 계정에 5분 할인 기회를 부여한다.
   *
   * @param principal 인증된 사용자
   * @return 진행 중인 promo 또는 promo가 null인 응답
   */
  @Operation(
      summary = "페이월 이탈 할인 부여",
      description =
          "요청 바디 없이 호출합니다. 비구독자의 최초 이탈에서만 5분 할인을 부여합니다."
              + " 중복·동시 요청은 만료 시각을 연장하지 않습니다. 프리미엄·만료 상태는 promo가 null입니다."
              + " newUser는 부여 당시 가입 후 7일 미만 여부이며 이후 유지합니다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "이탈 처리 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ApiResponse<PaywallDismissResponse> dismissPaywall(AuthUserPrincipal principal);

  /**
   * 인증된 사용자의 서버 기준 구독 상태와 페이월 판단 근거를 조회한다.
   *
   * @param principal 인증된 사용자
   * @return 구독 상태, 프리미엄 적용 여부, 무료 체험 여부, 결제 기간 종류, 만료 시각, 도입 이후 대화 완료 여부, 상품 ID, 스토어
   */
  @Operation(
      summary = "사용자 구독 상태 조회",
      description =
          "RevenueCat 웹훅으로 갱신된 서버 기준 구독 상태를 조회합니다. premium이 true면 프리미엄 혜택이 적용 중입니다."
              + " isTrial이 true면 연간 구독의 7일 무료 체험 중이며 expiresAt이 체험 종료 시각입니다. 대시보드에서 부여한"
              + " 프로모션 권한은 periodType이 PROMOTIONAL이고 isTrial은 false입니다."
              + " 시나리오 대화는 구독과 관계없이 무료이고, 무료 사용자의 상세 피드백 잠금은 피드백 응답의"
              + " detailFeedbackLocked로 판단합니다. promo는 이탈 API와 같은 할인 객체이며 조회로 생성하지 않습니다."
              + " price·currency는 최신 실제 결제 금액·통화이며 없으면 null입니다."
              + " productId와 store는 프리미엄이 켜져 있을 때만 값이 있습니다.",
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
