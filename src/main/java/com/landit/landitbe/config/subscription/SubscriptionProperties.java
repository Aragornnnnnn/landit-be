// 유료 구독 도입 시점(LANDIT_SUBSCRIPTION_LAUNCHED_AT) 설정을 담는다.

package com.landit.landitbe.config.subscription;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 유료 구독 도입 시점을 담는 설정이다.
 *
 * <p>환경변수 {@code LANDIT_SUBSCRIPTION_LAUNCHED_AT}을 서버 시작 시 읽어 보관하며, 이 시점 이후에 시나리오를 완료했는지가 {@code
 * conversationCompletedSinceLaunch}와 유료 기능 잠금의 기준이 된다. 도입 시점은 dev·prod가 다를 수 있어 코드가 아닌 설정으로 둔다.
 *
 * <ul>
 *   <li>값 예시: {@code 2026-09-15T00:00:00+09:00} (ISO-8601, 시간대 포함)
 *   <li>비어 있으면 아직 도입 전으로 보고 {@link #launchedAtOrEmpty()}가 빈 값을 돌려준다. 이때 완료 여부는 항상 false고 유료 잠금도
 *       꺼진다.
 *   <li>형식이 틀리면 서버 시작에 실패한다.
 * </ul>
 *
 * @param launchedAt 유료 구독 도입 시점 문자열. 비어 있으면 도입 전
 */
@ConfigurationProperties(prefix = "landit.subscription")
public record SubscriptionProperties(String launchedAt) {

  /**
   * 앞뒤 공백을 정리하고, 값이 있으면 형식을 검증한다.
   *
   * @throws IllegalArgumentException ISO-8601 offset datetime 형식이 아닐 때
   */
  public SubscriptionProperties {
    launchedAt = launchedAt == null ? "" : launchedAt.trim();
    if (!launchedAt.isBlank()) {
      parse(launchedAt);
    }
  }

  /**
   * 유료 구독 도입 시점을 반환한다.
   *
   * @return 도입 시점. 설정되지 않았으면 빈 값
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
