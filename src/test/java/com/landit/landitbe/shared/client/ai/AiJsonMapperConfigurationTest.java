// AI 응답을 읽는 Spring JsonMapper가 모르는 필드를 무시하는지 검증한다.

package com.landit.landitbe.shared.client.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** AI 서버가 백엔드보다 먼저 배포되어도 응답 역직렬화가 깨지지 않는 전제를 고정한다. */
class AiJsonMapperConfigurationTest {
  @DisplayName("AI 클라이언트에 주입되는 JsonMapper는 응답의 모르는 필드를 오류로 처리하지 않는다.")
  @Test
  void autoConfiguredJsonMapperIgnoresUnknownProperties() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
        .run(
            context -> {
              JsonMapper jsonMapper = context.getBean(JsonMapper.class);

              assertThat(jsonMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                  .isFalse();
              assertThat(jsonMapper.readValue("{\"known\":1,\"addedLater\":2}", Known.class))
                  .isEqualTo(new Known(1));
            });
  }

  private record Known(int known) {}
}
