// 사용자의 구독 결제 이력 한 건을 API 응답으로 제공한다.

package com.landit.landitbe.feature.subscription.dto;

import com.landit.landitbe.feature.profile.domain.SubscriptionPeriodType;
import com.landit.landitbe.feature.profile.domain.SubscriptionStore;
import com.landit.landitbe.feature.subscription.domain.SubscriptionEvent;
import com.landit.landitbe.feature.subscription.domain.SubscriptionEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 사용자의 구독 결제 이력 한 건을 API 응답으로 제공한다.
 *
 * @param eventId RevenueCat 이벤트 ID. 중복 웹훅 방지 키
 * @param type 이벤트 타입
 * @param productId 구독 상품 ID
 * @param periodType 결제 기간 종류
 * @param price 결제 통화 기준 금액. 체험·프로모션·해지처럼 결제가 없는 이벤트는 0
 * @param currency ISO 4217 통화 코드. 없으면 {@code null}
 * @param store 결제한 스토어
 * @param environment SANDBOX(테스트 결제) 또는 PRODUCTION(실제 결제)
 * @param cancelReason CANCELLATION 이벤트의 해지 사유. 환불이면 CUSTOMER_SUPPORT. 없으면 {@code null}
 * @param occurredAt 결제 또는 이벤트 발생 시각
 * @param expiresAt 구독 만료 시각. 없으면 {@code null}
 */
@Schema(description = "구독 결제 이력")
public record SubscriptionEventResponse(
    @Schema(
            description = "RevenueCat 이벤트 ID. 중복 웹훅 방지 키",
            example = "d3f1a2b4-5c6d-7e8f-9a0b-1c2d3e4f5a6b")
        String eventId,
    @Schema(
            description =
                "이벤트 타입. INITIAL_PURCHASE(첫 결제 또는 체험 시작), RENEWAL(갱신, 체험 끝 첫 결제 포함),"
                    + " CANCELLATION(해지 예약·환불), UNCANCELLATION(해지 취소), EXPIRATION(만료),"
                    + " BILLING_ISSUE(결제 실패), PRODUCT_CHANGE(플랜 변경), TRANSFER(다른 계정에서 구독을 넘겨받음)",
            example = "RENEWAL")
        SubscriptionEventType type,
    @Schema(description = "구독 상품 ID", example = "com.saynow.app.premium.yearly") String productId,
    @Schema(
            description = "결제 기간 종류. TRIAL, INTRO, NORMAL, PROMOTIONAL, PREPAID. 알 수 없으면 null",
            example = "NORMAL")
        SubscriptionPeriodType periodType,
    @Schema(description = "결제 통화 기준 금액. 체험·프로모션·해지 등 결제 없는 이벤트는 0", example = "58500")
        BigDecimal price,
    @Schema(description = "ISO 4217 통화 코드. 없으면 null", example = "KRW") String currency,
    @Schema(description = "결제한 스토어. APP_STORE, PLAY_STORE 등. 알 수 없으면 null", example = "APP_STORE")
        SubscriptionStore store,
    @Schema(description = "SANDBOX(테스트 결제) 또는 PRODUCTION(실제 결제)", example = "PRODUCTION")
        String environment,
    @Schema(
            description = "해지 사유. 환불이면 CUSTOMER_SUPPORT. CANCELLATION 외에는 null",
            example = "UNSUBSCRIBE")
        String cancelReason,
    @Schema(description = "결제 또는 이벤트 발생 시각", example = "2026-09-10T03:12:00")
        LocalDateTime occurredAt,
    @Schema(description = "구독 만료 시각. 없으면 null", example = "2027-09-10T03:12:00")
        LocalDateTime expiresAt) {

  /**
   * 저장된 결제 이력을 응답으로 변환한다.
   *
   * @param event 저장된 결제 이력
   * @return 결제 이력 응답
   */
  public static SubscriptionEventResponse from(SubscriptionEvent event) {
    return new SubscriptionEventResponse(
        event.getEventId(),
        event.getType(),
        event.getProductId(),
        event.getPeriodType(),
        toResponsePrice(event.getPrice()),
        event.getCurrency(),
        event.getStore(),
        event.getEnvironment(),
        event.getCancelReason(),
        event.getOccurredAt(),
        event.getExpiresAt());
  }

  /** 결제 없는 이벤트는 0으로 내리고, 58500.0000처럼 저장된 값은 58500으로 정리해 지수 표기 없이 내린다. */
  private static BigDecimal toResponsePrice(BigDecimal price) {
    if (price == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal stripped = price.stripTrailingZeros();
    return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
  }
}
