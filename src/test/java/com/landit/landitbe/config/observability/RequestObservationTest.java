// 실제 MVC 경로 확정과 인증 이전 요청 문맥의 격리 및 복원을 검증한다.

package com.landit.landitbe.config.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.landit.landitbe.shared.observability.ObservationContext;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

class RequestObservationTest {
  @AfterEach
  void clear() {
    MDC.clear();
  }

  @Test
  void handlerSeesRouteTemplateBeforeReportingAndFilterRestoresContext() throws Exception {
    InterceptorRegistry registry = mock(InterceptorRegistry.class);
    new ObservationWebConfiguration().addInterceptors(registry);
    ArgumentCaptor<HandlerInterceptor> interceptor =
        ArgumentCaptor.forClass(HandlerInterceptor.class);
    verify(registry).addInterceptor(interceptor.capture());
    RouteController controller = new RouteController();
    var mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .addInterceptors(interceptor.getValue())
            .addFilters(new RequestObservationFilter())
            .build();
    MDC.put("learning_session_id", "99");
    mvc.perform(get("/api/sessions/secret-path?token=secret-token"));
    assertThat(controller.context.get("http_route")).isEqualTo("/api/sessions/{sessionId}");
    assertThat(controller.context.get("http_method")).isEqualTo("GET");
    assertThat(controller.context.get("request_id")).isNotBlank();
    assertThat(controller.context.get("learning_session_id")).isNull();
    assertThat(controller.context.toString()).doesNotContain("secret-");
    assertThat(MDC.get("learning_session_id")).isEqualTo("99");
    assertThat(MDC.get("http_route")).isNull();
  }

  @Test
  void preAuthenticationOrUnmatchedRequestHasNoRawPathOrInheritedUser() throws Exception {
    MDC.put("user_id", "42");
    MockHttpServletRequest request = new MockHttpServletRequest("SECRET", "/secret-path");
    new RequestObservationFilter()
        .doFilter(
            request,
            new MockHttpServletResponse(),
            (req, res) -> {
              assertThat(MDC.get("http_method")).isEqualTo("UNKNOWN");
              assertThat(MDC.get("http_route")).isEqualTo("UNKNOWN");
              assertThat(MDC.get("user_id")).isNull();
            });
    assertThat(MDC.get("user_id")).isEqualTo("42");
  }

  @RestController
  static class RouteController {
    private Map<String, String> context;

    @GetMapping("/api/sessions/{sessionId}")
    String call() {
      context = ObservationContext.capture();
      return "ok";
    }
  }
}
