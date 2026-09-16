// 유료 기능 접근 허용 규칙이 도입 여부·프리미엄 조합마다 기대대로 동작하는지 검증한다.

package com.landit.landitbe.feature.subscription.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 유료 기능 접근 허용 규칙이 도입 여부·프리미엄 조합마다 기대대로 동작하는지 검증한다. */
class PremiumAccessTest {

  /** 유료 구독 도입 전에는 모든 기능을 허용한다. */
  @Test
  void allowsEverythingBeforeLaunch() {
    PremiumAccess access = PremiumAccess.beforeLaunch();

    assertThat(access.launched()).isFalse();
    assertThat(access.allowsPremiumOnlyFeature()).isTrue();
  }

  /** 프리미엄 사용자는 프리미엄 전용 기능을 허용한다. */
  @Test
  void allowsPremiumOnlyFeatureForPremium() {
    assertThat(PremiumAccess.afterLaunch(true).allowsPremiumOnlyFeature()).isTrue();
  }

  /** 비프리미엄 사용자는 도입 후 프리미엄 전용 기능을 쓸 수 없다. */
  @Test
  void blocksPremiumOnlyFeatureForNonPremiumAfterLaunch() {
    PremiumAccess access = PremiumAccess.afterLaunch(false);

    assertThat(access.launched()).isTrue();
    assertThat(access.allowsPremiumOnlyFeature()).isFalse();
  }
}
