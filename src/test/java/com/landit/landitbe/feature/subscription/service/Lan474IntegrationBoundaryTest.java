// 내부 AI 인증과 샌드박스 이벤트가 운영 구독 상태를 잘못 변경하지 않는지 검증한다.

package com.landit.landitbe.feature.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.config.subscription.RevenueCatProperties;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.subscription.dto.RevenueCatWebhookRequest;
import com.landit.landitbe.feature.subscription.exception.SubscriptionException;
import com.landit.landitbe.feature.subscription.repository.SubscriptionEventRepository;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** 경계 설정을 실제 요청 객체와 이벤트 역직렬화로 검증한다. */
class Lan474IntegrationBoundaryTest {
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

  @Test
  void sandboxPurchaseAndTransferDoNotReadOrWriteSubscriptionState() {
    var profiles = mock(UserProfileService.class);
    var events = mock(SubscriptionEventRepository.class);
    var service =
        new RevenueCatWebhookService(
            new RevenueCatProperties("test-auth", false), profiles, events, Clock.systemUTC());
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
    verifyNoInteractions(profiles, events);
  }
}
