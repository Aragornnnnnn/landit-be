// RevenueCat 웹훅 요청 본문의 최상위 구조를 바인딩한다.

package com.landit.landitbe.feature.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * RevenueCat 웹훅 요청 본문의 최상위 구조를 바인딩한다.
 *
 * @param apiVersion RevenueCat 웹훅 API 버전
 * @param event 구독 이벤트
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatWebhookRequest(
    @JsonProperty("api_version") String apiVersion,
    @JsonProperty("event") @NotNull @Valid RevenueCatWebhookEvent event) {}
