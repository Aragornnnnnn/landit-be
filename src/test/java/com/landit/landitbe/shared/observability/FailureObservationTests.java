// 실제 SDK의 메모리 전송으로 실패 정책과 민감정보 제거를 검증한다.

package com.landit.landitbe.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.landit.landitbe.shared.client.ai.AiUpstreamException;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import io.sentry.Hint;
import io.sentry.Sentry;
import io.sentry.SentryEnvelope;
import io.sentry.SentryEvent;
import io.sentry.SentryItemType;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
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

  @Test
  void upstreamStatusAndApiCodeSurviveSanitizationAndMdcIsRestored() throws Exception {
    MDC.put("upstream_status", "old");
    ApiException exception =
        ApiException.causedBy(ErrorCode.AI_GENERATION_FAILED, new AiUpstreamException(429));
    FailureObservation.failed("inner_thought", "generation", "result_missing", exception);
    SentryEvent event = events.getFirst();
    assertThat(event.getTag("upstream_status")).isEqualTo("429");
    assertThat(event.getTag("error_code")).isEqualTo("AI_GENERATION_FAILED");
    assertThat(event.getMessage().getFormatted()).contains("inner_thought", "result_missing");
    assertThat(event.getExceptions().getLast().getValue()).contains("result_missing");
    assertThat(event.getExceptions().getFirst().getStacktrace().getFrames()).isNotEmpty();
    assertThat(MDC.get("upstream_status")).isEqualTo("old");
    FailureObservation.failed(
        "storage", "save", "storage_failed", new IllegalStateException("secret-body"));
    assertThat(events.getLast().getTag("upstream_status")).isNull();
    assertThat(serialized()).doesNotContain("secret-body");
  }

  @Test
  void directSdkCaptureAlsoRetainsUpstreamStatus() {
    Sentry.captureException(
        ApiException.causedBy(ErrorCode.AI_RESPONSE_INVALID, new AiUpstreamException(502)));
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getTag("upstream_status")).isEqualTo("502");
    assertThat(events.getFirst().getTag("error_code")).isEqualTo("AI_RESPONSE_INVALID");
  }

  @Test
  void logAndSdkCaptureRetainOnlyAuthenticatedInternalId() throws Exception {
    User scopeUser = new User();
    scopeUser.setId("999");
    scopeUser.setEmail("secret-email");
    scopeUser.setIpAddress("secret-ip");
    scopeUser.setUsername("secret-name");
    Sentry.setUser(scopeUser);
    MDC.put("user_id", "42");
    FailureObservation.failed("feedback", "generation", "result_missing", null);
    Sentry.captureException(new IllegalStateException("secret-body"));
    assertThat(events).hasSize(2);
    assertThat(events).allSatisfy(event -> assertThat(event.getUser().getId()).isEqualTo("42"));
    assertThat(serialized()).doesNotContain("secret-");
    MDC.remove("user_id");
    Sentry.captureException(new IllegalStateException("next-request"));
    assertThat(events.getLast().getUser()).isNull();
  }

  @Test
  void loggingSnapshotKeepsOriginalActorWhenProcessedOnAnotherThread() {
    MDC.put("user_id", "42");
    LoggingEvent log = new LoggingEvent();
    log.setLoggerName("test");
    log.setLevel(ch.qos.logback.classic.Level.ERROR);
    log.setMessage("failure");
    log.setLoggerContext(logger.getLoggerContext());
    log.prepareForDeferredProcessing();
    MDC.put("user_id", "7");
    Sentry.captureEvent(appender.createEvent(log));
    assertThat(events.getFirst().getUser().getId()).isEqualTo("42");
  }

  @Test
  void invalidMdcUserValueIsDropped() {
    MDC.put("user_id", "secret-email");
    Sentry.captureException(new IllegalStateException("failure"));
    assertThat(events.getFirst().getUser()).isNull();
  }

  @Test
  void unrelatedUntaggedLoggersHaveDifferentFingerprints() {
    SentryEvent reservation = untaggedLog("NotificationJobReservationService");
    SentryEvent attachment = untaggedLog("MailboxFeedbackSubmissionService");
    assertThat(reservation.getFingerprints()).isNotEqualTo(attachment.getFingerprints());
    assertThat(reservation.getMessage().getFormatted()).doesNotContain("secret-");
  }

  private SentryEvent untaggedLog(String loggerName) {
    SentryEvent raw = new SentryEvent();
    raw.setLogger(loggerName);
    Message message = new Message();
    message.setFormatted("secret-object-key");
    raw.setMessage(message);
    return SafeSentryAppender.sanitize(raw);
  }

  private String serialized() throws Exception {
    StringWriter writer = new StringWriter();
    for (SentryEvent event : events) {
      options.getSerializer().serialize(event, writer);
    }
    return writer.toString();
  }
}
