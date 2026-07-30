// 관리자 API 요청의 사용자 프로필이 허용 목록에 있는지 확인한다.

package com.landit.landitbe.feature.admin.security;

import com.landit.landitbe.feature.admin.service.AdminAccountService;
import com.landit.landitbe.feature.auth.security.AuthFailureResponseWriter;
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
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

/** 관리자 API 요청의 사용자 프로필이 허용 목록에 있는지 확인한다. */
@Component
public class AdminAuthorizationFilter extends OncePerRequestFilter {

  private static final String ADMIN_API_PATH = "/api/v1/admin";

  private final AdminAccountService adminAccountService;
  private final AuthFailureResponseWriter failureResponseWriter;

  /**
   * 관리자 허용 목록 Service와 접근 거부 응답 작성기를 주입받는다.
   *
   * @param adminAccountService 관리자 허용 목록 조회 Service
   * @param failureResponseWriter 접근 거부 응답 작성기
   */
  public AdminAuthorizationFilter(
      AdminAccountService adminAccountService, AuthFailureResponseWriter failureResponseWriter) {
    this.adminAccountService = adminAccountService;
    this.failureResponseWriter = failureResponseWriter;
  }

  /** {@inheritDoc} */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    return !requestUri.equals(ADMIN_API_PATH) && !requestUri.startsWith(ADMIN_API_PATH + "/");
  }

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof AuthUserPrincipal principal
        && !adminAccountService.isAdmin(principal.userId())) {
      failureResponseWriter.write(response, ErrorCode.FORBIDDEN);
      return;
    }
    filterChain.doFilter(request, response);
  }
}
