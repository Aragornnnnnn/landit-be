// 내부 AI 인증과 샌드박스 이벤트가 운영 구독 상태를 잘못 변경하지 않는지 검증한다.

package com.landit.landitbe.feature.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.config.subscription.RevenueCatProperties;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.subscription.event.dto.RevenueCatWebhookRequest;
import com.landit.landitbe.feature.subscription.event.repository.SubscriptionEventRepository;
import com.landit.landitbe.feature.subscription.event.service.RevenueCatWebhookService;
import com.landit.landitbe.feature.subscription.exception.SubscriptionException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** 경계 설정을 실제 요청 객체와 이벤트 역직렬화로 검증한다. */
class Lan474IntegrationBoundaryTest {
  @DisplayName("내부 인증 토큰은 요청에 전송하되 설정 로그에는 노출하지 않는다.")
  @Test
  void internalTokenIsSentAndExcludedFromConfigurationLogs() {
    var timeout = Duration.ofSeconds(30);
    var properties =
        new AiClientProperties(
            "http://localhost",
            "remote",
            "test",
            timeout,
            timeout,
            timeout,
            timeout,
            "internal-test-token");
    var request =
        properties
            .authorize(HttpRequest.newBuilder(URI.create("http://localhost/api/v1/test")))
            .build();
    assertThat(request.headers().firstValue("X-Landit-Internal-Token"))
        .contains("internal-test-token");
    assertThat(properties.toString()).doesNotContain("internal-test-token");
  }

  @DisplayName("샌드박스 구매와 계정 이전 이벤트는 구독 상태를 조회하거나 변경하지 않는다.")
  @Test
  void sandboxPurchaseAndTransferDoNotReadOrWriteSubscriptionState() {
    var profiles = mock(UserProfileService.class);
    var subscriptionProfiles =
        mock(
            com.landit.landitbe.feature.profile.subscription.service.ProfileSubscriptionService
                .class);
    var events = mock(SubscriptionEventRepository.class);
    var service =
        new RevenueCatWebhookService(
            new RevenueCatProperties("test-auth", false),
            profiles,
            subscriptionProfiles,
            events,
            Clock.systemUTC(),
            mock(org.springframework.context.ApplicationEventPublisher.class));
    for (String type : new String[] {"INITIAL_PURCHASE", "TRANSFER"}) {
      var request =
          new JsonMapper()
              .readValue(
                  """
                  {"api_version":"1.0","event":{"id":"sandbox-test","type":"%s",
                    "environment":"SANDBOX","app_user_id":"1",
                    "transferred_from":["1"],"transferred_to":["2"]}}
                  """
                      .formatted(type),
                  RevenueCatWebhookRequest.class);
      service.handle("test-auth", request);
      assertThatThrownBy(() -> service.handle("wrong-auth", request))
          .isInstanceOf(SubscriptionException.class);
    }
    verifyNoInteractions(profiles, subscriptionProfiles, events);
  }
}
