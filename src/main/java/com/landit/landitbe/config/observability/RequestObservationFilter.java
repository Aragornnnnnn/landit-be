// 서버가 생성한 요청 식별자를 동기 로그와 AI 호출에 연결한다.

package com.landit.landitbe.config.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 외부 입력을 복사하지 않고 요청마다 안전한 상관관계 ID를 생성한다. */
@Component
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
    String previous = MDC.get("request_id");
    MDC.put("request_id", UUID.randomUUID().toString());
    try {
      chain.doFilter(request, response);
    } finally {
      if (previous == null) {
        MDC.remove("request_id");
      } else {
        MDC.put("request_id", previous);
      }
    }
  }
}
