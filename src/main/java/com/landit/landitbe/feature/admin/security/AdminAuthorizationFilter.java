// 관리자 API 요청의 사용자 프로필 역할을 확인한다.

package com.landit.landitbe.feature.admin.security;

import com.landit.landitbe.feature.auth.security.AuthFailureResponseWriter;
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 관리자 API 요청의 사용자 프로필 역할을 확인한다. */
@Component
public class AdminAuthorizationFilter extends OncePerRequestFilter {

  private static final String ADMIN_API_PATH = "/api/v1/admin";

  private final UserProfileService userProfileService;
  private final AuthFailureResponseWriter failureResponseWriter;

  /**
   * 사용자 프로필 Service와 접근 거부 응답 작성기를 주입받는다.
   *
   * @param userProfileService 사용자 역할 조회 Service
   * @param failureResponseWriter 접근 거부 응답 작성기
   */
  public AdminAuthorizationFilter(
      UserProfileService userProfileService, AuthFailureResponseWriter failureResponseWriter) {
    this.userProfileService = userProfileService;
    this.failureResponseWriter = failureResponseWriter;
  }

  /**
   * 관리자 API가 아닌 요청은 이 필터의 권한 검사를 건너뛰도록 결정한다.
   *
   * @param request 검사할 HTTP 요청
   * @return 관리자 API 요청이면 {@code false}, 그 외 요청이면 {@code true}
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    return !requestUri.equals(ADMIN_API_PATH) && !requestUri.startsWith(ADMIN_API_PATH + "/");
  }

  /**
   * 인증 사용자의 관리자 허용 여부를 확인하고 허용되지 않은 요청을 차단한다.
   *
   * @param request 검사할 HTTP 요청
   * @param response 접근 거부 응답을 작성할 HTTP 응답
   * @param filterChain 권한 확인 후 요청을 전달할 필터 체인
   * @throws ServletException 다음 필터 처리 중 Servlet 오류가 발생했을 때
   * @throws IOException 응답 작성 또는 다음 필터 처리 중 입출력 오류가 발생했을 때
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof AuthUserPrincipal principal
        && !userProfileService.isAdmin(principal.userId())) {
      failureResponseWriter.write(response, ErrorCode.FORBIDDEN);
      return;
    }
    filterChain.doFilter(request, response);
  }
}
