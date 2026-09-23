// Grafana Cloud로 전송할 HTTP 요청과 JVM 메트릭 등록을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.Metric;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.util.ReflectionTestUtils;
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

  @Autowired private CapturingMetricsSender metricsSender;

  @Autowired private OtlpMeterRegistry otlpMeterRegistry;

  @DisplayName("OTLP 메트릭 레지스트리가 구성된다.")
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

  @DisplayName("HTTP 요청 메트릭에 요청 수와 지연 시간 및 응답 상태를 기록한다.")
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

  @DisplayName("HTTP 히스토그램은 설정된 버킷만 사용하며 느린 요청도 보존한다.")
  @Test
  void exportedHttpHistogramPreservesSlowRequestsWithOnlyConfiguredBuckets() {
    Timer timer =
        otlpMeterRegistry.timer(
            "http.server.requests", "uri", "/metrics-budget-test", "status", "200");
    timer.record(Duration.ofMillis(100));
    timer.record(Duration.ofSeconds(121));
    metricsSender.requests.clear();

    ReflectionTestUtils.invokeMethod(otlpMeterRegistry, "publish");

    var points =
        metricsSender.requests.stream()
            .flatMap(request -> request.getResourceMetricsList().stream())
            .flatMap(resource -> resource.getScopeMetricsList().stream())
            .flatMap(scope -> scope.getMetricsList().stream())
            .filter(metric -> metric.getName().equals("http.server.requests"))
            .map(Metric::getHistogram)
            .flatMap(histogram -> histogram.getDataPointsList().stream())
            .filter(
                point ->
                    point.getAttributesList().stream()
                        .anyMatch(
                            attribute ->
                                attribute.getKey().equals("uri")
                                    && attribute
                                        .getValue()
                                        .getStringValue()
                                        .equals("/metrics-budget-test")))
            .toList();

    assertThat(points).hasSize(1);
    var point = points.getFirst();
    assertThat(point.getExplicitBoundsList())
        .containsExactly(
            50.0, 100.0, 200.0, 300.0, 500.0, 750.0, 1000.0, 2000.0, 3000.0, 5000.0, 10000.0,
            20000.0, 30000.0, 60000.0, 120000.0);
    assertThat(point.getBucketCountsList()).hasSize(16);
    assertThat(point.getBucketCounts(15)).isEqualTo(1);
    assertThat(point.getCount()).isEqualTo(2);
    assertThat(point.getSum()).isEqualTo(121100.0);
  }

  @DisplayName("사용하지 않는 요청 및 Repository 메트릭을 등록하지 않는다.")
  @Test
  void unusedRequestAndRepositoryMetersAreNotRegistered() {
    meterRegistry.more().longTaskTimer("http.server.requests.active").start().stop();
    meterRegistry.timer("spring.data.repository.invocations").record(Duration.ofMillis(10));

    assertThat(meterRegistry.find("http.server.requests.active").meters()).isEmpty();
    assertThat(meterRegistry.find("spring.data.repository.invocations").meters()).isEmpty();
  }

  @DisplayName("JVM 메모리와 GC 및 스레드 메트릭을 등록한다.")
  @Test
  void jvmMemoryGcAndThreadMetricsAreRegistered() {
    assertThat(meterRegistry.find("jvm.memory.used").meters()).isNotEmpty();
    assertThat(meterRegistry.find("jvm.gc.max.data.size").meters()).isNotEmpty();
    assertThat(meterRegistry.find("jvm.threads.live").meters()).isNotEmpty();
  }

  @DisplayName("Hikari 커넥션 풀 메트릭을 등록한다.")
  @Test
  void hikariConnectionPoolMetricsAreRegistered() {
    assertThat(meterRegistry.find("hikaricp.connections.active").meters()).isNotEmpty();
    assertThat(meterRegistry.find("hikaricp.connections.idle").meters()).isNotEmpty();
    assertThat(meterRegistry.find("hikaricp.connections.pending").meters()).isNotEmpty();
    assertThat(meterRegistry.find("hikaricp.connections.max").meters()).isNotEmpty();
    assertThat(meterRegistry.find("hikaricp.connections.timeout").meters()).isNotEmpty();
  }

  @DisplayName("Tomcat 스레드 풀 메트릭을 등록한다.")
  @Test
  void tomcatThreadPoolMetricsAreRegistered() {
    assertThat(meterRegistry.find("tomcat.threads.busy").meters()).isNotEmpty();
    assertThat(meterRegistry.find("tomcat.threads.current").meters()).isNotEmpty();
    assertThat(meterRegistry.find("tomcat.threads.config.max").meters()).isNotEmpty();
  }

  @DisplayName("메트릭에 배포 버전을 첨부한다.")
  @Test
  void deploymentVersionIsAttachedToMetrics() {
    assertThat(meterRegistry.find("jvm.memory.used").tag("service.version", "be-v1.2.3").meters())
        .isNotEmpty();
  }

  @DisplayName("메트릭 조회용 Actuator 엔드포인트를 외부에 노출하지 않는다.")
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
    CapturingMetricsSender otlpMetricsSender() {
      return new CapturingMetricsSender();
    }
  }

  static class CapturingMetricsSender implements OtlpMetricsSender {

    final List<ExportMetricsServiceRequest> requests = new CopyOnWriteArrayList<>();

    @Override
    public void send(Request request) throws Exception {
      requests.add(ExportMetricsServiceRequest.parseFrom(request.getMetricsData()));
    }
  }
}
