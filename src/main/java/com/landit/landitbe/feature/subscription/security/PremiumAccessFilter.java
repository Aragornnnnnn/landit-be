// 유료 기능 API 요청을 구독 상태와 무료 대화 완료 여부로 제한한다.

package com.landit.landitbe.feature.subscription.security;

import com.landit.landitbe.feature.auth.security.AuthFailureResponseWriter;
import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.subscription.dto.PremiumAccess;
import com.landit.landitbe.feature.subscription.exception.SubscriptionErrorCode;
import com.landit.landitbe.feature.subscription.service.UserSubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 유료 기능 API 요청을 구독 상태와 무료 대화 완료 여부로 제한한다.
 *
 * <p>인증 필터 뒤에서 동작하며, 아래 두 목록에 해당하는 요청만 검사한다. 경로 비교는 시큐리티 설정의 {@code requestMatchers}와 같은 {@link
 * PathPatternRequestMatcher}를 써서, 컨트롤러 매핑과 동일하게 디코딩된 경로로 판단한다(인코딩·매트릭스 변수로 우회할 수 없다). 허용 규칙은 {@link
 * PremiumAccess}가 정한다. 인증되지 않은 요청은 건드리지 않고 넘겨 시큐리티 설정이 401을 돌려주게 한다.
 *
 * <ul>
 *   <li>프리미엄 전용: 프리톡 시작과 진행 중 동작(메시지·종료 결정·표현 재생성), 표현 학습 시작·추가 예문·완료, 발음 평가
 *   <li>무료 대화 범위: 시나리오 세션 시작, 세션 메시지 전송
 * </ul>
 *
 * <p>피드백 조회, 속마음 조회, 세션 종료, 레벨 평가 조회처럼 완료한 세션의 결과를 보는 API와 표현 목록·프리톡 주제·지난 프리톡 조회, 마이페이지·스트릭· 메일함은
 * 제한하지 않는다. 잠긴 사용자는 새 세션을 만들 수 없으므로 결과 보기 API는 이미 만든 세션에만 닿는다.
 */
@Component
public class PremiumAccessFilter extends OncePerRequestFilter {

  private static final List<RequestMatcher> PREMIUM_ONLY_PATHS =
      List.of(
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/free-talk/sessions"),
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/free-talk/sessions/*/**"),
          PathPatternRequestMatcher.pathPattern(
              HttpMethod.GET, "/api/v1/expressions/*/learning-start"),
          PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/expressions/*/practice"),
          PathPatternRequestMatcher.pathPattern(
              HttpMethod.POST, "/api/v1/expressions/*/learning-finish"),
          PathPatternRequestMatcher.pathPattern(
              HttpMethod.POST, "/api/v1/expressions/*/pronunciation/**"));

  private static final List<RequestMatcher> SCENARIO_CONVERSATION_PATHS =
      List.of(
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/scenarios/*/sessions"),
          PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/sessions/*/messages"));

  private final UserSubscriptionService userSubscriptionService;
  private final AuthFailureResponseWriter failureResponseWriter;

  /**
   * 구독 상태 평가 Service와 접근 거부 응답 작성기를 주입받는다.
   *
   * @param userSubscriptionService 구독·대화 완료 상태 평가 Service
   * @param failureResponseWriter 접근 거부 응답 작성기
   */
  public PremiumAccessFilter(
      UserSubscriptionService userSubscriptionService,
      AuthFailureResponseWriter failureResponseWriter) {
    this.userSubscriptionService = userSubscriptionService;
    this.failureResponseWriter = failureResponseWriter;
  }

  /**
   * 유료 제한 대상 경로가 아닌 요청은 이 필터를 건너뛴다.
   *
   * @param request 검사할 HTTP 요청
   * @return 제한 대상 경로가 아니면 {@code true}
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !matchesAny(PREMIUM_ONLY_PATHS, request)
        && !matchesAny(SCENARIO_CONVERSATION_PATHS, request);
  }

  /**
   * 인증 사용자의 구독 상태를 평가해 허용되지 않은 유료 기능 요청을 403으로 차단한다.
   *
   * @param request 검사할 HTTP 요청
   * @param response 접근 거부 응답을 작성할 HTTP 응답
   * @param filterChain 허용 시 요청을 전달할 필터 체인
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
        && !isAllowed(request, userSubscriptionService.evaluateAccess(principal.userId()))) {
      SubscriptionErrorCode errorCode = SubscriptionErrorCode.PREMIUM_REQUIRED;
      failureResponseWriter.write(
          response, errorCode.getStatus(), errorCode.name(), errorCode.getMessage());
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean isAllowed(HttpServletRequest request, PremiumAccess access) {
    if (matchesAny(PREMIUM_ONLY_PATHS, request)) {
      return access.allowsPremiumOnlyFeature();
    }
    return access.allowsScenarioConversation();
  }

  private static boolean matchesAny(List<RequestMatcher> matchers, HttpServletRequest request) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(request));
  }
}
