// 서버가 생성한 요청 식별자를 동기 로그와 AI 호출에 연결한다.

package com.landit.landitbe.config.observability;

import com.landit.landitbe.shared.observability.ObservationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 외부 입력을 복사하지 않고 요청마다 안전한 상관관계 ID를 생성한다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestObservationFilter extends OncePerRequestFilter {
  /**
   * 요청 범위에서만 식별자를 보존한다.
   *
   * @param request 요청
   * @param response 응답
   * @param chain 필터 체인
   * @throws ServletException 요청 처리 실패
   * @throws IOException 입출력 실패
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Map<String, String> context = new HashMap<>();
    ObservationContext.capture().keySet().forEach(key -> context.put(key, null));
    context.put("request_id", UUID.randomUUID().toString());
    context.put("http_method", request.getMethod());
    context.put("http_route", "UNKNOWN");
    try (var ignored = ObservationContext.open(context)) {
      try {
        chain.doFilter(request, response);
      } catch (ServletException | IOException | RuntimeException failure) {
        ObservationContext.remember(failure);
        throw failure;
      }
    }
  }
}
