// 실제 SDK의 메모리 전송으로 실패 정책과 민감정보 제거를 검증한다.

package com.landit.landitbe.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import io.sentry.Hint;
import io.sentry.Sentry;
import io.sentry.SentryEnvelope;
import io.sentry.SentryEvent;
import io.sentry.SentryItemType;
import io.sentry.SentryOptions;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import io.sentry.transport.ITransport;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class FailureObservationTests {
  private final List<SentryEvent> events = new ArrayList<>();
  private final SentryOptions options = new SentryOptions();
  private final SafeSentryAppender appender = new SafeSentryAppender();
  private final Logger logger = (Logger) LoggerFactory.getLogger(FailureObservation.class);
  private boolean additive;

  @BeforeEach
  void startMemoryTransport() throws Exception {
    Sentry.close();
    ITransport transport = mock(ITransport.class);
    doAnswer(
            invocation -> {
              SentryEnvelope envelope = invocation.getArgument(0);
              for (var item : envelope.getItems()) {
                if (item.getHeader().getType() == SentryItemType.Event) {
                  events.add(item.getEvent(options.getSerializer()));
                }
              }
              return null;
            })
        .when(transport)
        .send(any(SentryEnvelope.class), any(Hint.class));
    options.setDsn("https://public@example.invalid/1");
    options.setTransportFactory((ignoredOptions, ignoredRequest) -> transport);
    options.setEnableUncaughtExceptionHandler(false);
    options.setEnableShutdownHook(false);
    options.setSendClientReports(false);
    appender.setContext(logger.getLoggerContext());
    appender.setOptions(options);
    appender.start();
    additive = logger.isAdditive();
    logger.setAdditive(false);
    logger.addAppender(appender);
  }

  @AfterEach
  void stopMemoryTransport() {
    logger.detachAppender(appender);
    logger.setAdditive(additive);
    appender.stop();
    Sentry.close();
    MDC.clear();
  }

  @Test
  void recoveredAndExpectedRejectionRetainMetricsWithoutEvents() {
    FailureObservation.observed("closing", "validation", "safe_fallback", "recovered");
    FailureObservation.observed("api", "authentication", "invalid_token", "expected_rejection");
    assertThat(events).isEmpty();
  }

  @Test
  void reportsOriginalCauseOnceAndKeepsIndependentConnectionFailure() throws Exception {
    User user = new User();
    user.setEmail("secret-email");
    Sentry.setUser(user);
    Sentry.setExtra("body", "secret-body");
    Sentry.addBreadcrumb("secret-breadcrumb");
    MDC.put("request_id", "dff24113-5b57-43fc-87ec-a1f06fba9941");
    RuntimeException failure =
        new RuntimeException("secret-message", new IllegalStateException("secret-key"));
    FailureObservation.failed("memory", "persistence", "storage_failed", failure);
    FailureObservation.failed("memory", "persistence", "storage_failed", failure);
    Sentry.captureException(failure);
    FailureObservation.failed(
        "ai_connection",
        "request",
        "connection_failed",
        new java.net.ConnectException("secret-url"));
    assertThat(events).hasSize(2);
    assertThat(events.getFirst().getTag("request_id")).isEqualTo(MDC.get("request_id"));
    assertThat(events.getFirst().getExceptions())
        .extracting(value -> value.getType())
        .contains("java.lang.IllegalStateException", "java.lang.RuntimeException");
    assertThat(events.getFirst().getExceptions().getFirst().getStacktrace().getFrames())
        .isNotEmpty();
    assertThat(serialized()).doesNotContain("secret-");
  }

  @Test
  void wrapperAndCauseShareOneFailureEventInEitherReportingOrder() {
    RuntimeException firstCause = new RuntimeException("secret-first");
    RuntimeException firstWrapper = new RuntimeException("secret-wrapper", firstCause);
    FailureObservation.failed("memory", "persistence", "storage_failed", firstCause);
    FailureObservation.failed("memory", "persistence", "storage_failed", firstWrapper);

    RuntimeException secondCause = new RuntimeException("secret-second");
    RuntimeException secondWrapper = new RuntimeException("secret-wrapper", secondCause);
    FailureObservation.failed("memory", "persistence", "storage_failed", secondWrapper);
    FailureObservation.failed("memory", "persistence", "storage_failed", secondCause);

    assertThat(events).hasSize(2);
  }

  @Test
  void automaticSdkErrorsAreSanitizedWithoutSuppressingClientExceptionClasses() throws Exception {
    SentryEvent raw = new SentryEvent(new IllegalArgumentException("secret-request"));
    Request request = new Request();
    request.setData("secret-user-text");
    raw.setRequest(request);
    raw.setExtra("token", "secret-token");
    Sentry.captureEvent(raw);
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getRequest()).isNull();
    assertThat(events.getFirst().getExceptions())
        .extracting(value -> value.getType())
        .contains("IllegalArgumentException");
    assertThat(serialized()).doesNotContain("secret-");
  }

  @Test
  void failuresWithoutThrowableHaveSeparateWorkflowFingerprintsAndRestoreMdc() {
    MDC.put("outcome", "previous");
    FailureObservation.failed("message_feedback", "generation", "attempts_exhausted", null);
    FailureObservation.failed("level_assessment", "result", "fallback_result", null);
    assertThat(events).hasSize(2);
    assertThat(events.get(0).getFingerprints()).isNotEqualTo(events.get(1).getFingerprints());
    assertThat(MDC.get("outcome")).isEqualTo("previous");
  }

  private String serialized() throws Exception {
    StringWriter writer = new StringWriter();
    for (SentryEvent event : events) {
      options.getSerializer().serialize(event, writer);
    }
    return writer.toString();
  }
}
