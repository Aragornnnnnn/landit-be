// 기본 설정에서 dev 전용 푸시 테스트 API가 생성되지 않는지 검증한다.

package com.landit.landitbe.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** 기본 설정에서 dev 전용 푸시 테스트 API가 생성되지 않는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class PushNotificationTestApiDisabledIntegrationTests {

  @Autowired private ApplicationContext applicationContext;

  /** 테스트 API 활성화 설정이 없으면 Controller Bean을 생성하지 않는다. */
  @Test
  void doesNotCreateTestApiControllerByDefault() {
    assertThat(applicationContext.getBeansOfType(PushNotificationTestController.class)).isEmpty();
  }
}
