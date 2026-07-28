// 프리톡 세션 표현이 공통 Writing 표현을 연결하는지만 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 프리톡 세션 표현의 공통 Writing 표현 연결을 검증한다. */
class FreeTalkSessionExpressionTest {

  /** 프리톡에서 추천한 Writing 표현과 개인화 예문을 연결한다. */
  @Test
  void linksWritingExpressionToFreeTalkSession() {
    FreeTalkSessionExpression expression =
        FreeTalkSessionExpression.link(10L, 20L, 1, "We really hit it off.", "우리는 정말 죽이 잘 맞았어.");

    assertThat(expression.getFreeTalkSessionId()).isEqualTo(10L);
    assertThat(expression.getWritingExpressionId()).isEqualTo(20L);
    assertThat(expression.getDisplayOrder()).isEqualTo(1);
    assertThat(expression.getPersonalizedExampleText()).isEqualTo("We really hit it off.");
  }
}
