// 요청과 작업의 안전한 식별자를 전파하고 예외 발생 당시 문맥을 보존한다.

package com.landit.landitbe.shared.observability;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import org.slf4j.MDC;

/** 사용자 값 없이 허용된 요청·작업 식별자만 관리한다. */
public final class ObservationContext {
  private static final Set<String> KEYS =
      Set.of(
          "request_id",
          "user_id",
          "http_method",
          "http_route",
          "learning_session_id",
          "free_talk_session_id",
          "message_id");
  private static final Set<String> METHODS =
      Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT");
  private static final Map<Throwable, Map<String, String>> FAILURES =
      Collections.synchronizedMap(new WeakHashMap<>());

  private ObservationContext() {}

  /**
   * 허용된 필드의 형식을 검증한다.
   *
   * @param key 관측 키
   * @param value 원본 값
   * @return 안전한 값 또는 null
   */
  public static String validate(String key, String value) {
    if (!KEYS.contains(key) || value == null) {
      return null;
    }
    return switch (key) {
      case "http_method" -> METHODS.contains(value) ? value : "UNKNOWN";
      case "http_route" -> value.matches("[A-Za-z0-9_./{}*:-]{1,256}") ? value : "UNKNOWN";
      case "request_id" -> value.matches("[A-Za-z0-9-]{1,64}") ? value : null;
      default -> ObservationUserId.validate(value);
    };
  }

  /**
   * 비어 있는 값도 포함해 작업 시작 당시 문맥을 저장한다.
   *
   * @return 허용된 키만 포함하는 독립된 snapshot
   */
  public static Map<String, String> capture() {
    Map<String, String> context = new HashMap<>();
    KEYS.forEach(key -> context.put(key, validate(key, MDC.get(key))));
    return context;
  }

  /**
   * 예외가 다른 스레드나 scope에서 보고되어도 발생 당시 문맥을 반환한다.
   *
   * @param failure 보고할 예외
   * @return 예외에 저장한 문맥 또는 현재 문맥
   */
  public static Map<String, String> forFailure(Throwable failure) {
    Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    while (failure != null && seen.size() < 8 && seen.add(failure)) {
      Map<String, String> context = FAILURES.get(failure);
      if (context != null) {
        return new HashMap<>(context);
      }
      failure = failure.getCause();
    }
    return capture();
  }

  /**
   * 작업 범위가 닫히기 전에 예외와 원인에 연결된 문맥을 보존한다.
   *
   * @param failure 발생한 예외
   */
  public static void remember(Throwable failure) {
    FAILURES.putIfAbsent(failure, forFailure(failure));
  }

  /**
   * 저장한 문맥을 적용하고 종료 시 이전 값으로 복원한다.
   *
   * @param context 적용할 허용 키와 값
   * @return 복원 가능한 scope
   */
  public static Scope open(Map<String, String> context) {
    return new Scope(context);
  }

  /**
   * 검증된 작업 대상 문맥에서 값을 계산하며 오류 문맥도 보존한다.
   *
   * @param <T> 반환값 타입
   * @param learningSessionId 학습 세션 ID 또는 null
   * @param freeTalkSessionId 프리톡 세션 ID 또는 null
   * @param messageId 작업 대상 메시지 ID 또는 null
   * @param action 수행할 작업
   * @return 작업의 반환값
   */
  public static <T> T call(
      Long learningSessionId, Long freeTalkSessionId, Long messageId, Supplier<T> action) {
    Map<String, String> domain = new HashMap<>();
    domain.put("learning_session_id", string(learningSessionId));
    domain.put("free_talk_session_id", string(freeTalkSessionId));
    domain.put("message_id", string(messageId));
    try (Scope ignored = open(domain)) {
      try {
        return action.get();
      } catch (RuntimeException failure) {
        remember(failure);
        throw failure;
      }
    }
  }

  /**
   * 검증된 작업 대상 문맥에서 반환값 없는 작업을 실행한다.
   *
   * @param learningSessionId 학습 세션 ID 또는 null
   * @param freeTalkSessionId 프리톡 세션 ID 또는 null
   * @param messageId 작업 대상 메시지 ID 또는 null
   * @param action 수행할 작업
   */
  public static void run(
      Long learningSessionId, Long freeTalkSessionId, Long messageId, Runnable action) {
    call(
        learningSessionId,
        freeTalkSessionId,
        messageId,
        () -> {
          action.run();
          return null;
        });
  }

  private static String string(Long id) {
    return id == null ? null : id.toString();
  }

  /** 변경한 키만 복원하여 외곽 문맥과 다른 라이브러리의 MDC를 보존한다. */
  public static final class Scope implements AutoCloseable {
    private final Map<String, String> previous = new HashMap<>();

    private Scope(Map<String, String> context) {
      context.forEach(
          (key, value) -> {
            if (KEYS.contains(key)) {
              previous.put(key, MDC.get(key));
              put(key, validate(key, value));
            }
          });
    }

    /** 작업 범위 진입 이전 값으로 되돌린다. */
    @Override
    public void close() {
      previous.forEach(Scope::put);
    }

    private static void put(String key, String value) {
      if (value == null) {
        MDC.remove(key);
      } else {
        MDC.put(key, value);
      }
    }
  }
}
