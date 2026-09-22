// 페이월 이탈 요청 결과를 할인 기회 객체로 전달한다.

package com.landit.landitbe.feature.subscription.dto;

import com.landit.landitbe.feature.profile.subscription.dto.DiscountOffer;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 페이월 이탈 결과를 전달한다.
 *
 * @param promo 진행 중인 할인. 자격이 없거나 만료됐으면 null
 */
public record PaywallDismissResponse(
    @Schema(description = "진행 중인 할인. 프리미엄이거나 만료됐으면 null", nullable = true) DiscountOffer promo) {}
