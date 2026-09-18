// 테스트 환경에서 OTLP 외부 전송이 비활성화되는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/** 테스트 환경에서 OTLP 외부 전송이 비활성화되는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class OtlpExportDisabledIntegrationTests {

  @Autowired private Environment environment;

  @Autowired private ApplicationContext applicationContext;

  @DisplayName("테스트 프로필에서는 OTLP 메트릭 전송을 비활성화한다.")
  @Test
  void testProfileDisablesOtlpMetricsExport() {
    assertThat(environment.getProperty("management.otlp.metrics.export.enabled", Boolean.class))
        .isFalse();
    assertThat(applicationContext.getBeanNamesForType(MeterRegistry.class))
        .noneMatch(beanName -> beanName.toLowerCase().contains("otlp"));
  }
}
