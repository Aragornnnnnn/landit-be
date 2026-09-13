// 유료 구독 도입 시점 설정의 정규화와 형식 검증을 확인한다.

package com.landit.landitbe.config.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** 유료 구독 도입 시점 설정의 정규화와 형식 검증을 확인한다. */
class SubscriptionPropertiesTest {

  /** 비어 있거나 null인 도입 시점은 설정되지 않은 것으로 본다. */
  @Test
  void treatsBlankLaunchedAtAsUnset() {
    assertThat(new SubscriptionProperties(null).launchedAtOrEmpty()).isEmpty();
    assertThat(new SubscriptionProperties("   ").launchedAtOrEmpty()).isEmpty();
  }

  /** ISO-8601 offset datetime은 앞뒤 공백을 제거하고 파싱한다. */
  @Test
  void parsesIsoOffsetDateTime() {
    SubscriptionProperties properties = new SubscriptionProperties(" 2026-09-15T00:00:00+09:00 ");

    assertThat(properties.launchedAtOrEmpty())
        .contains(OffsetDateTime.parse("2026-09-15T00:00:00+09:00"));
  }

  /** 형식이 잘못된 값은 조용히 무시하지 않고 생성 시점에 실패한다. */
  @Test
  void rejectsMalformedLaunchedAt() {
    assertThatThrownBy(() -> new SubscriptionProperties("2026-09-15"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("LANDIT_SUBSCRIPTION_LAUNCHED_AT");
  }
}
