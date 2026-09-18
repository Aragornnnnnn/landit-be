// 세션 구현 없이 기억 AI 어댑터만으로 local·remote Bean을 구성할 수 있는지 검증한다.

package com.landit.landitbe.feature.memory.client.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingRequest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

/** 기억 AI 구현의 독립적인 조건부 Bean 구성을 검증한다. */
class AiMemoryClientConfigurationTest {
  @DisplayName("세션 설정 없이도 기억 AI 클라이언트 하나만 생성한다.")
  @ParameterizedTest
  @ValueSource(strings = {"", "local", "remote"})
  void createsExactlyOneMemoryClientWithoutSessionConfiguration(String mode) {
    var timeout = Duration.ofSeconds(1);
    var runner =
        new ApplicationContextRunner()
            .withUserConfiguration(LocalAiMemoryClient.class, RemoteAiMemoryClient.class)
            .withBean(JsonMapper.class, JsonMapper::new)
            .withBean(
                AiClientProperties.class,
                () ->
                    new AiClientProperties(
                        "http://localhost", "remote", "test", timeout, timeout, timeout, timeout));
    if (!mode.isEmpty()) {
      runner = runner.withPropertyValues("landit.ai.client-mode=" + mode);
    }
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(AiMemoryClient.class);
          AiMemoryClient client = context.getBean(AiMemoryClient.class);
          assertThat(client)
              .isExactlyInstanceOf(
                  mode.equals("remote") ? RemoteAiMemoryClient.class : LocalAiMemoryClient.class);
          if (!mode.equals("remote")) {
            assertThat(
                    client
                        .embedMemoryQuery(new AiMemoryQueryEmbeddingRequest("weekend"))
                        .embedding())
                .hasSize(1536)
                .startsWith(1.0f, 0.0f);
          }
        });
  }
}
