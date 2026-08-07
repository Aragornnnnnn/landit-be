// Grafana Cloud로 전송할 HTTP 요청과 JVM 메트릭 등록을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Grafana Cloud로 전송할 HTTP 요청과 JVM 메트릭 등록을 검증한다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ObservabilityIntegrationTests.OtlpMetricsSenderTestConfiguration.class)
@TestPropertySource(
    properties = {
      "APP_VERSION=be-v1.2.3",
      "management.otlp.metrics.export.enabled=true",
      "management.otlp.metrics.export.step=1h",
      "management.otlp.metrics.export.url=http://127.0.0.1:4318/v1/metrics"
    })
class ObservabilityIntegrationTests {

  @Autowired private ApplicationContext applicationContext;

  @Autowired private MeterRegistry meterRegistry;

  @Autowired private MockMvc mockMvc;

  @Autowired private WebEndpointsSupplier webEndpointsSupplier;

  @Test
  void otlpMeterRegistryIsConfigured() {
    assertThat(
            applicationContext
                .getEnvironment()
                .getProperty("management.otlp.metrics.export.enabled", Boolean.class))
        .isTrue();
    assertThat(applicationContext.getBeanNamesForType(MeterRegistry.class))
        .anyMatch(beanName -> beanName.toLowerCase().contains("otlp"));
  }

  @Test
  void httpServerRequestMetricRecordsRequestCountLatencyAndStatus() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

    Timer timer =
        meterRegistry
            .find("http.server.requests")
            .tags(
                "method", "GET",
                "uri", "/actuator/health",
                "status", "200",
                "outcome", "SUCCESS")
            .timer();

    assertThat(timer).isNotNull();
    assertThat(timer.count()).isPositive();
    assertThat(timer.totalTime(timer.baseTimeUnit())).isPositive();
  }

  @Test
  void jvmMemoryGcAndThreadMetricsAreRegistered() {
    assertThat(meterRegistry.find("jvm.memory.used").meters()).isNotEmpty();
    assertThat(meterRegistry.find("jvm.gc.max.data.size").meters()).isNotEmpty();
    assertThat(meterRegistry.find("jvm.threads.live").meters()).isNotEmpty();
  }

  @Test
  void hikariConnectionPoolMetricsAreRegistered() {
    assertThat(meterRegistry.find("hikaricp.connections.active").meters()).isNotEmpty();
    assertThat(meterRegistry.find("hikaricp.connections.idle").meters()).isNotEmpty();
    assertThat(meterRegistry.find("hikaricp.connections.pending").meters()).isNotEmpty();
    assertThat(meterRegistry.find("hikaricp.connections.max").meters()).isNotEmpty();
    assertThat(meterRegistry.find("hikaricp.connections.timeout").meters()).isNotEmpty();
  }

  @Test
  void tomcatThreadPoolMetricsAreRegistered() {
    assertThat(meterRegistry.find("tomcat.threads.busy").meters()).isNotEmpty();
    assertThat(meterRegistry.find("tomcat.threads.current").meters()).isNotEmpty();
    assertThat(meterRegistry.find("tomcat.threads.config.max").meters()).isNotEmpty();
  }

  @Test
  void deploymentVersionIsAttachedToMetrics() {
    assertThat(meterRegistry.find("jvm.memory.used").tag("service.version", "be-v1.2.3").meters())
        .isNotEmpty();
  }

  @Test
  void metricActuatorEndpointsAreNotExposed() {
    Set<String> exposedEndpointIds =
        webEndpointsSupplier.getEndpoints().stream()
            .map(endpoint -> endpoint.getEndpointId().toString())
            .collect(Collectors.toSet());

    assertThat(exposedEndpointIds)
        .contains("health", "info")
        .doesNotContain("metrics", "prometheus");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class OtlpMetricsSenderTestConfiguration {

    @Bean
    OtlpMetricsSender otlpMetricsSender() {
      return request -> {};
    }
  }
}
