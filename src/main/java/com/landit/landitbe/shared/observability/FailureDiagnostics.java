// 예외 체인에서 원문 없는 오류 코드와 외부 HTTP 상태를 추출한다.

package com.landit.landitbe.shared.observability;

import com.landit.landitbe.shared.client.ai.AiUpstreamException;
import com.landit.landitbe.shared.exception.ApiException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Sentry 전송 전과 원인 예외 정제 전에 같은 안전한 진단 정보를 추출한다. */
final class FailureDiagnostics {
  private FailureDiagnostics() {}

  static Map<String, String> tags(Throwable cause) {
    Map<String, String> result = new LinkedHashMap<>();
    Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    while (cause != null && seen.size() < 8 && seen.add(cause)) {
      if (cause instanceof ApiException api && api.getErrorCode() instanceof Enum<?>) {
        result.putIfAbsent("error_code", api.getErrorCode().name());
      }
      if (cause instanceof AiUpstreamException upstream) {
        result.putIfAbsent("upstream_status", Integer.toString(upstream.statusCode()));
      }
      cause = cause.getCause();
    }
    return result;
  }
}
