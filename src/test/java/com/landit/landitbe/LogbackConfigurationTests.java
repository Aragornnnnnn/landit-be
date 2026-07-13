// Logback 콘솔 로그가 한국 표준시 패턴을 먼저 적용하는지 검증한다.
package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LogbackConfigurationTests {

    @Test
    void koreanStandardTimePatternIsDeclaredBeforeBootDefaults() throws IOException {
        String configuration = new ClassPathResource("logback-spring.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(configuration.indexOf("name=\"LOG_DATEFORMAT_PATTERN\""))
                .isLessThan(configuration.indexOf("logging/logback/defaults.xml"));
    }
}
