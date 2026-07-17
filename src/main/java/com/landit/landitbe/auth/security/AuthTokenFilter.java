// Bearer access token을 검증해 SecurityContext에 인증 주체를 저장한다.

package com.landit.landitbe.auth.security;

import com.landit.landitbe.auth.application.LanditTokenService;
import com.landit.landitbe.auth.domain.UserProfileStatus;
import com.landit.landitbe.auth.infrastructure.UserProfileRepository;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bearer access token을 검증해 SecurityContext에 인증 주체를 저장한다. */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final LanditTokenService tokenService;
  private final UserProfileRepository userProfileRepository;
  private final AuthFailureResponseWriter failureResponseWriter;

  /** 동작을 수행한다. */
  public AuthTokenFilter(
      LanditTokenService tokenService,
      UserProfileRepository userProfileRepository,
      AuthFailureResponseWriter failureResponseWriter) {
    this.tokenService = tokenService;
    this.userProfileRepository = userProfileRepository;
    this.failureResponseWriter = failureResponseWriter;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    Long userId;
    try {
      userId = tokenService.parseAccessToken(authorization.substring(BEARER_PREFIX.length()));
    } catch (ApiException exception) {
      SecurityContextHolder.clearContext();
      failureResponseWriter.write(response, exception.getErrorCode());
      return;
    }
    if (!userProfileRepository.existsByIdAndStatus(userId, UserProfileStatus.ACTIVE)) {
      SecurityContextHolder.clearContext();
      failureResponseWriter.write(response, ErrorCode.INVALID_TOKEN);
      return;
    }

    AuthUserPrincipal principal = new AuthUserPrincipal(userId);
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    SecurityContextHolder.setContext(securityContext);
    try {
      filterChain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
