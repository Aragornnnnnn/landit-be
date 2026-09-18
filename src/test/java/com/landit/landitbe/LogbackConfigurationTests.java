// Logback 콘솔 로그가 한국 표준시 패턴을 먼저 적용하는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Logback 콘솔 로그가 한국 표준시 패턴을 먼저 적용하는지 검증한다. */
class LogbackConfigurationTests {

  @DisplayName("Logback 기본 설정을 읽기 전에 한국 표준시 패턴을 선언한다.")
  @Test
  void koreanStandardTimePatternIsDeclaredBeforeBootDefaults() throws IOException {
    String configuration =
        new ClassPathResource("logback-spring.xml").getContentAsString(StandardCharsets.UTF_8);

    assertThat(configuration.indexOf("name=\"LOG_DATEFORMAT_PATTERN\""))
        .isLessThan(configuration.indexOf("logging/logback/defaults.xml"));
  }
}
