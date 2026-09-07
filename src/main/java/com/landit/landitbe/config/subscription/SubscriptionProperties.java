// 유료 구독 도입 시점(LANDIT_SUBSCRIPTION_LAUNCHED_AT) 환경변수를 읽어 구독 기능이 쓸 수 있는 값으로 바꾼다.

package com.landit.landitbe.config.subscription;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 유료 구독 도입 시점 환경변수를 읽어 구독 기능이 쓸 수 있는 값으로 바꾼다.
 *
 * <p><b>무슨 파일인가.</b> 서버가 시작될 때 Spring Boot가 {@code application.yml}의 {@code
 * landit.subscription.launched-at} 값(환경변수 {@code LANDIT_SUBSCRIPTION_LAUNCHED_AT})을 이 record에 채워
 * 넣는다. 코드에서는 이 객체를 주입받아 {@link #launchedAtOrEmpty()}로 도입 시점을 꺼내 쓴다. 현재 사용처는 {@code
 * UserSubscriptionService}뿐이다.
 *
 * <p><b>왜 환경변수인가.</b> "유료 구독을 언제부터 붙였는가"는 아직 정해지지 않았고 dev와 prod가 다를 수 있다. 코드에 날짜를 박으면 바꿀 때마다 배포해야
 * 하므로, 출시일에 SSM에 값만 넣으면 되도록 설정으로 뺐다.
 *
 * <p><b>어디에 쓰이는가.</b> {@code GET /api/v1/me/subscription}의 {@code
 * conversationCompletedSinceLaunch}는 "이 시점 이후에 시나리오를 끝까지 완료했는가"로 계산된다. 도입 전에 완료한 기존 사용자를 세지 않기 위한
 * 기준선이다.
 *
 * <p><b>값이 없거나 잘못됐을 때.</b>
 *
 * <ul>
 *   <li>비어 있으면(기본값) 아직 도입 전으로 보고 {@link #launchedAtOrEmpty()}가 빈 값을 돌려준다. 그러면 응답 필드는 항상 false가 되고
 *       {@code UserSubscriptionService}가 서버 시작 시 WARN 로그를 남긴다.
 *   <li>값이 있는데 ISO-8601 offset datetime 형식(예: {@code 2026-09-15T00:00:00+09:00})이 아니면 잘못된 설정을 조용히
 *       넘기지 않고 서버 시작 자체를 실패시킨다.
 * </ul>
 *
 * @param launchedAt 유료 구독 도입 시점 문자열. 예: {@code 2026-09-15T00:00:00+09:00}. 비어 있으면 아직 도입 전
 */
@ConfigurationProperties(prefix = "landit.subscription")
public record SubscriptionProperties(String launchedAt) {

  /**
   * 도입 시점 문자열의 앞뒤 공백을 정리하고, 값이 있으면 서버 시작 시점에 형식을 검증한다.
   *
   * @throws IllegalArgumentException 값이 ISO-8601 offset datetime 형식이 아닐 때
   */
  public SubscriptionProperties {
    launchedAt = launchedAt == null ? "" : launchedAt.trim();
    if (!launchedAt.isBlank()) {
      parse(launchedAt);
    }
  }

  /**
   * 유료 구독 도입 시점을 날짜·시각 객체로 반환한다.
   *
   * @return 도입 시점. 환경변수가 비어 있어 아직 도입 전이면 빈 값
   */
  public Optional<OffsetDateTime> launchedAtOrEmpty() {
    if (launchedAt.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(parse(launchedAt));
  }

  private static OffsetDateTime parse(String value) {
    try {
      return OffsetDateTime.parse(value);
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException(
          "LANDIT_SUBSCRIPTION_LAUNCHED_AT은 ISO-8601 offset datetime이어야 한다. 예:"
              + " 2026-09-15T00:00:00+09:00",
          exception);
    }
  }
}
