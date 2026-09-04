// RevenueCat 웹훅 본문의 event 객체에서 구독 상태 갱신에 필요한 필드만 바인딩한다.

package com.landit.landitbe.feature.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * RevenueCat 웹훅 본문의 event 객체에서 구독 상태 갱신에 필요한 필드만 바인딩한다.
 *
 * <p>RevenueCat은 snake_case 키를 보내므로 각 구성 요소에 JSON 키를 명시한다.
 *
 * @param id RevenueCat 이벤트 고유 ID
 * @param type 이벤트 타입. 예: INITIAL_PURCHASE, RENEWAL, CANCELLATION, EXPIRATION
 * @param appUserId 이벤트 시점의 App User ID. 앱에서 Landit 사용자 ID로 설정한다
 * @param originalAppUserId 처음 사용한 App User ID
 * @param aliases 지금까지 사용한 모든 App User ID
 * @param productId 구독 상품 ID
 * @param environment SANDBOX 또는 PRODUCTION
 * @param cancelReason CANCELLATION 이벤트의 해지 사유. 환불이면 CUSTOMER_SUPPORT
 * @param expirationReason EXPIRATION 이벤트의 만료 사유
 * @param expirationAtMs 구독 만료 시각(epoch ms)
 * @param eventTimestampMs 이벤트 생성 시각(epoch ms)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatWebhookEvent(
    @JsonProperty("id") String id,
    @JsonProperty("type") @NotBlank String type,
    @JsonProperty("app_user_id") String appUserId,
    @JsonProperty("original_app_user_id") String originalAppUserId,
    @JsonProperty("aliases") List<String> aliases,
    @JsonProperty("product_id") String productId,
    @JsonProperty("environment") String environment,
    @JsonProperty("cancel_reason") String cancelReason,
    @JsonProperty("expiration_reason") String expirationReason,
    @JsonProperty("expiration_at_ms") Long expirationAtMs,
    @JsonProperty("event_timestamp_ms") Long eventTimestampMs) {}
