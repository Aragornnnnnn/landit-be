// 모든 Sentry 오류 이벤트에서 사용자 값과 시크릿을 제거한다.

package com.landit.landitbe.shared.observability;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.logback.SentryAppender;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import java.util.Map;
import java.util.Set;

/** Logback 및 SDK 자동 오류를 전송 직전에 동일하게 정제한다. */
public class SafeSentryAppender extends SentryAppender {
  private static final Set<String> TAGS =
      Set.of(
          "workflow", "failure_stage", "reason", "outcome", "recovered", "attempt", "request_id");

  /**
   * SDK 초기화 전에 모든 오류 수집 경로의 필터를 등록한다.
   *
   * @param options Logback이 구성한 Sentry 옵션
   */
  @Override
  public void setOptions(SentryOptions options) {
    options.setBeforeSend((event, hint) -> sanitize(event));
    options.setBeforeBreadcrumb((breadcrumb, hint) -> null);
    options.setSendDefaultPii(false);
    super.setOptions(options);
  }

  /**
   * 관측 태그를 MDC에서 이벤트로 복사한다.
   *
   * @param loggingEvent 원본 로그
   * @return SDK 처리 전 오류 이벤트
   */
  @Override
  protected SentryEvent createEvent(ILoggingEvent loggingEvent) {
    SentryEvent event = super.createEvent(loggingEvent);
    loggingEvent
        .getMDCPropertyMap()
        .forEach(
            (key, value) -> {
              if (TAGS.contains(key)) {
                event.setTag(key, value);
              }
            });
    return event;
  }

  /**
   * 메시지 대신 원인 타입·스택·안전한 관측 태그를 보존한다.
   *
   * @param event SDK가 조립한 오류 이벤트
   * @return 전송할 정제 이벤트 또는 정상 복구/거절이면 null
   */
  public static SentryEvent sanitize(SentryEvent event) {
    if (FailureObservation.alreadyReported(event.getThrowable())) {
      return null;
    }
    String outcome = event.getTag("outcome");
    if (Set.of("expected_rejection", "recovered", "retrying")
        .contains(outcome == null ? "" : outcome)) {
      return null;
    }
    SentryEvent safe = new SentryEvent(event.getTimestamp());
    safe.setEventId(event.getEventId());
    safe.setLevel(event.getLevel());
    safe.setRelease(event.getRelease());
    safe.setEnvironment(event.getEnvironment());
    safe.setPlatform(event.getPlatform());
    safe.setLogger(event.getLogger());
    safe.setSdk(event.getSdk());
    Map<String, String> tags = event.getTags();
    if (tags != null) {
      tags.forEach(
          (key, value) -> {
            if (TAGS.contains(key)) {
              safe.setTag(key, value);
            }
          });
    }
    safe.setTag("outcome", "failed");
    Message message = new Message();
    message.setFormatted("functional_failure");
    safe.setMessage(message);
    if (event.getExceptions() != null) {
      for (SentryException exception : event.getExceptions()) {
        if (exception.getType() != null && exception.getType().endsWith("SanitizedFailure")) {
          exception.setType(exception.getValue());
          exception.setModule(null);
        }
        exception.setValue("[Filtered]");
        exception.setUnknown(null);
        exception.setMechanism(null);
        if (exception.getStacktrace() != null && exception.getStacktrace().getFrames() != null) {
          exception
              .getStacktrace()
              .getFrames()
              .forEach(
                  frame -> {
                    frame.setVars(null);
                    frame.setPreContext(null);
                    frame.setPostContext(null);
                    frame.setContextLine(null);
                    frame.setUnknown(null);
                  });
        }
      }
      safe.setExceptions(event.getExceptions());
    }
    if (safe.getExceptions() == null || safe.getExceptions().isEmpty()) {
      safe.setFingerprints(
          java.util.List.of(
              "functional_failure",
              safe.getTag("workflow") == null ? "unclassified" : safe.getTag("workflow"),
              safe.getTag("failure_stage") == null ? "execution" : safe.getTag("failure_stage"),
              safe.getTag("reason") == null ? "unexpected" : safe.getTag("reason")));
    }
    return safe;
  }
}
