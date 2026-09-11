// 유료 기능 접근 허용 규칙이 도입 여부·프리미엄·대화 완료 조합마다 기대대로 동작하는지 검증한다.

package com.landit.landitbe.feature.subscription.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 유료 기능 접근 허용 규칙이 도입 여부·프리미엄·대화 완료 조합마다 기대대로 동작하는지 검증한다. */
class PremiumAccessTest {

  /** 유료 구독 도입 전에는 모든 기능을 허용한다. */
  @Test
  void allowsEverythingBeforeLaunch() {
    PremiumAccess access = PremiumAccess.beforeLaunch();

    assertThat(access.allowsPremiumOnlyFeature()).isTrue();
    assertThat(access.allowsScenarioConversation()).isTrue();
  }

  /** 프리미엄 사용자는 대화 완료 여부와 관계없이 모든 기능을 허용한다. */
  @Test
  void allowsEverythingForPremium() {
    assertThat(PremiumAccess.afterLaunch(true, true).allowsPremiumOnlyFeature()).isTrue();
    assertThat(PremiumAccess.afterLaunch(true, true).allowsScenarioConversation()).isTrue();
    assertThat(PremiumAccess.afterLaunch(true, false).allowsScenarioConversation()).isTrue();
  }

  /** 비프리미엄 사용자는 프리미엄 전용 기능을 쓸 수 없지만, 대화를 완료하기 전에는 시나리오 대화를 할 수 있다. */
  @Test
  void allowsOnlyScenarioConversationForNonPremiumBeforeCompletion() {
    PremiumAccess access = PremiumAccess.afterLaunch(false, false);

    assertThat(access.allowsPremiumOnlyFeature()).isFalse();
    assertThat(access.allowsScenarioConversation()).isTrue();
  }

  /** 비프리미엄 사용자가 도입 이후 대화를 완료하면 시나리오 대화도 막힌다. */
  @Test
  void blocksScenarioConversationForNonPremiumAfterCompletion() {
    PremiumAccess access = PremiumAccess.afterLaunch(false, true);

    assertThat(access.allowsPremiumOnlyFeature()).isFalse();
    assertThat(access.allowsScenarioConversation()).isFalse();
  }
}
