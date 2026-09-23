// 인증된 사용자 ID가 요청 안에서만 관측 컨텍스트에 남는지 검증한다.

package com.landit.landitbe.feature.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.auth.exception.AuthErrorCode;
import com.landit.landitbe.feature.auth.service.LanditTokenService;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.security.SecurityFailureResponseWriter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthTokenFilterObservationTest {
  private final LanditTokenService tokens = mock(LanditTokenService.class);
  private final UserProfileService profiles = mock(UserProfileService.class);
  private final AuthTokenFilter filter =
      new AuthTokenFilter(tokens, profiles, mock(SecurityFailureResponseWriter.class));

  @AfterEach
  void cleanContext() {
    MDC.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatedIdOverridesCallerHeaderAndIsClearedBeforeNextRequest() throws Exception {
    when(tokens.parseAccessToken("valid")).thenReturn(42L);
    when(profiles.existsActive(42L)).thenReturn(true);
    MockHttpServletRequest request = request("valid");
    request.addHeader("X-Landit-User-Id", "999");
    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (ignoredRequest, ignoredResponse) -> assertThat(MDC.get("user_id")).isEqualTo("42"));
    assertThat(MDC.get("user_id")).isNull();
    filter.doFilter(
        new MockHttpServletRequest(),
        new MockHttpServletResponse(),
        (ignoredRequest, ignoredResponse) -> assertThat(MDC.get("user_id")).isNull());
  }

  @Test
  void failedRequestRestoresOuterContextAndClearsSecurityContext() {
    when(tokens.parseAccessToken("valid")).thenReturn(42L);
    when(profiles.existsActive(42L)).thenReturn(true);
    MDC.put("user_id", "7");
    assertThatThrownBy(
            () ->
                filter.doFilter(
                    request("valid"),
                    new MockHttpServletResponse(),
                    (ignoredRequest, ignoredResponse) -> {
                      assertThat(MDC.get("user_id")).isEqualTo("42");
                      throw new ServletException("failure");
                    }))
        .isInstanceOf(ServletException.class);
    assertThat(MDC.get("user_id")).isEqualTo("7");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void invalidOrInactiveUserDoesNotReachApplicationOrLeaveAnId() throws Exception {
    when(tokens.parseAccessToken("invalid"))
        .thenThrow(new ApiException(AuthErrorCode.INVALID_TOKEN));
    when(tokens.parseAccessToken("inactive")).thenReturn(42L);
    for (String token : new String[] {"invalid", "inactive"}) {
      filter.doFilter(
          request(token),
          new MockHttpServletResponse(),
          (ignoredRequest, ignoredResponse) -> {
            throw new AssertionError("Rejected request reached application");
          });
      assertThat(MDC.get("user_id")).isNull();
    }
  }

  private MockHttpServletRequest request(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);
    return request;
  }
}
