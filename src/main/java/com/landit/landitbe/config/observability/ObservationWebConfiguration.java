// 핸들러 실행 전에 원문 URL 대신 확정된 라우트 패턴을 관측 문맥에 넣는다.

package com.landit.landitbe.config.observability;

import com.landit.landitbe.shared.observability.ObservationContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 컨트롤러와 예외 핸들러가 동일한 라우트 패턴으로 오류를 보고하게 한다. */
@Configuration
public class ObservationWebConfiguration implements WebMvcConfigurer {
  /**
   * 라우트 선택 직후에 실행하는 관측 인터셉터를 등록한다.
   *
   * @param registry MVC 인터셉터 등록부
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(
        new HandlerInterceptor() {
          @Override
          public boolean preHandle(
              HttpServletRequest request, HttpServletResponse response, Object handler) {
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            MDC.put(
                "http_route",
                ObservationContext.validate(
                    "http_route", pattern instanceof String route ? route : "UNKNOWN"));
            return true;
          }
        });
  }
}
