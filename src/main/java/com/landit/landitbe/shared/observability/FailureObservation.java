// 기능 실패를 원문 없이 기록하고 같은 예외 객체의 중복 보고를 방지한다.

package com.landit.landitbe.shared.observability;

import io.micrometer.core.instrument.Metrics;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 로그 레벨과 안전한 태그를 통해 단일 Sentry 전송 경로를 사용한다. */
public final class FailureObservation {
  private static final Logger log = LoggerFactory.getLogger(FailureObservation.class);
  private static final Map<Throwable, Boolean> REPORTED =
      Collections.synchronizedMap(new WeakHashMap<>());

  private FailureObservation() {}

  /**
   * 최종 실패 또는 복구로 숨기면 안 되는 결함을 보고한다.
   *
   * @param workflow 기능 이름
   * @param stage 실패 단계
   * @param reason 코드에 정의된 원인 코드
   * @param cause 원인 예외. 메시지와 사용자 값은 복사하지 않는다.
   */
  public static void failed(String workflow, String stage, String reason, Throwable cause) {
    while ((cause instanceof java.util.concurrent.CompletionException
            || cause instanceof java.util.concurrent.ExecutionException)
        && cause.getCause() != null) {
      cause = cause.getCause();
    }
    if (cause != null) {
      synchronized (REPORTED) {
        Set<Throwable> chain = causeChain(cause);
        for (Throwable element : chain) {
          if (REPORTED.containsKey(element)) {
            return;
          }
        }
        for (Throwable element : chain) {
          REPORTED.put(element, Boolean.TRUE);
        }
      }
    }
    record(workflow, stage, reason, "failed", cause);
  }

  /**
   * 정상 복구나 예상 거절을 이벤트 없이 관측한다.
   *
   * @param workflow 기능 이름
   * @param stage 처리 단계
   * @param reason 코드에 정의된 원인 코드
   * @param outcome recovered 또는 expected_rejection. 재시도 중이면 retrying
   */
  public static void observed(String workflow, String stage, String reason, String outcome) {
    record(workflow, stage, reason, outcome, null);
  }

  /**
   * 실패 상태 트랜잭션의 커밋 이후에만 최종 실패를 보고한다.
   *
   * @param workflow 기능 이름
   * @param stage 실패 단계
   * @param reason 원인 코드
   * @param cause 원인 예외
   */
  public static void afterCommit(String workflow, String stage, String reason, Throwable cause) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      failed(workflow, stage, reason, cause);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            failed(workflow, stage, reason, cause);
          }
        });
  }

  static boolean alreadyReported(Throwable cause) {
    if (cause == null) {
      return false;
    }
    synchronized (REPORTED) {
      for (Throwable element : causeChain(cause)) {
        if (REPORTED.containsKey(element)) {
          return true;
        }
      }
    }
    return false;
  }

  private static Set<Throwable> causeChain(Throwable cause) {
    Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    while (cause != null && seen.add(cause)) {
      cause = cause.getCause();
    }
    return seen;
  }

  private static void record(
      String workflow, String stage, String reason, String outcome, Throwable cause) {
    Map<String, String> previous = MDC.getCopyOfContextMap();
    try {
      MDC.put("workflow", workflow);
      MDC.put("failure_stage", stage);
      MDC.put("reason", reason);
      MDC.put("outcome", outcome);
      MDC.put("recovered", Boolean.toString("recovered".equals(outcome)));
      Metrics.counter(
              "landit.failure.outcomes",
              "workflow",
              workflow,
              "failure_stage",
              stage,
              "reason",
              reason,
              "outcome",
              outcome)
          .increment();
      if ("failed".equals(outcome)) {
        log.error(
            "failure_observation workflow={} failure_stage={} reason={} outcome={}",
            workflow,
            stage,
            reason,
            outcome,
            sanitize(cause, Collections.newSetFromMap(new IdentityHashMap<>())));
      } else {
        log.warn(
            "failure_observation workflow={} failure_stage={} reason={} outcome={}",
            workflow,
            stage,
            reason,
            outcome);
      }
    } finally {
      if (previous == null) {
        MDC.clear();
      } else {
        MDC.setContextMap(previous);
      }
    }
  }

  private static final class SanitizedFailure extends RuntimeException {
    private SanitizedFailure(String type) {
      super(type);
    }
  }

  private static Throwable sanitize(Throwable cause, Set<Throwable> seen) {
    if (cause == null || seen.size() >= 8 || !seen.add(cause)) {
      return null;
    }
    RuntimeException safe = new SanitizedFailure(cause.getClass().getName());
    safe.setStackTrace(cause.getStackTrace());
    safe.initCause(sanitize(cause.getCause(), seen));
    return safe;
  }
}
