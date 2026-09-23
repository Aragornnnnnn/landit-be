// 내부 인증이 설정된 AI 요청에만 검증된 사용자 ID를 전달하는지 검증한다.

package com.landit.landitbe.config.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class AiClientPropertiesObservationTest {
  @AfterEach
  void cleanContext() {
    MDC.clear();
  }

  @Test
  void authenticatedAiRequestCarriesRequestAndUserIds() {
    MDC.put("request_id", "request-1");
    MDC.put("user_id", "42");
    var headers = authorizedRequest("test-internal-token").headers();
    assertThat(headers.firstValue("X-Request-Id")).contains("request-1");
    assertThat(headers.firstValue("X-Landit-User-Id")).contains("42");
  }

  @Test
  void unauthenticatedAiRequestOmitsUserId() {
    MDC.put("request_id", "request-1");
    MDC.put("user_id", "42");
    var headers = authorizedRequest("").headers();
    assertThat(headers.firstValue("X-Request-Id")).contains("request-1");
    assertThat(headers.firstValue("X-Landit-User-Id")).isEmpty();
  }

  @Test
  void absentOrInvalidIdIsNeverSent() {
    assertThat(authorizedRequest("test").headers().firstValue("X-Landit-User-Id")).isEmpty();
    for (String value : new String[] {"secret-email", "0", "-1", "01", "9223372036854775808"}) {
      MDC.put("user_id", value);
      assertThat(authorizedRequest("test").headers().firstValue("X-Landit-User-Id")).isEmpty();
    }
  }

  private HttpRequest authorizedRequest(String token) {
    Duration timeout = Duration.ofSeconds(10);
    AiClientProperties properties =
        new AiClientProperties(
            "http://localhost", "remote", "test", timeout, timeout, timeout, timeout, token);
    return properties
        .authorize(HttpRequest.newBuilder(URI.create("http://localhost/test")))
        .build();
  }
}
