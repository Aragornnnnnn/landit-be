// RevenueCat 웹훅 수신에 필요한 설정을 바인딩한다.

package com.landit.landitbe.config.subscription;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RevenueCat 웹훅 수신에 필요한 설정을 바인딩한다.
 *
 * @param webhookAuthorization RevenueCat 대시보드에 등록한 Authorization 헤더 값. 비어 있으면 모든 웹훅을 거절한다.
 */
@ConfigurationProperties(prefix = "landit.subscription.revenuecat")
public record RevenueCatProperties(String webhookAuthorization, Boolean applySandboxEvents) {

  /** 구버전 설정은 기존과 같이 샌드박스 이벤트를 반영한다. */
  public RevenueCatProperties(String webhookAuthorization) {
    this(webhookAuthorization, true);
  }

  /** Authorization 헤더 값의 앞뒤 공백을 제거하고 null을 빈 문자열로 정규화한다. */
  @org.springframework.boot.context.properties.bind.ConstructorBinding
  public RevenueCatProperties {
    applySandboxEvents = applySandboxEvents == null || applySandboxEvents;
    webhookAuthorization = webhookAuthorization == null ? "" : webhookAuthorization.trim();
  }

  /**
   * 웹훅 Authorization 헤더 값이 설정됐는지 확인한다.
   *
   * @return 값이 비어 있지 않으면 {@code true}
   */
  public boolean hasWebhookAuthorization() {
    return !webhookAuthorization.isBlank();
  }
}
