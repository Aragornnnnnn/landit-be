// 프리톡 세션 표현이 공통 Writing 표현을 연결하는지만 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 프리톡 세션 표현의 공통 Writing 표현 연결을 검증한다. */
class FreeTalkSessionExpressionTest {

  /** 프리톡에서 추천한 Writing 표현만 연결한다. */
  @Test
  void linksWritingExpressionToFreeTalkSession() {
    FreeTalkSessionExpression expression = FreeTalkSessionExpression.link(10L, 20L, 1);

    assertThat(expression.getFreeTalkSessionId()).isEqualTo(10L);
    assertThat(expression.getWritingExpressionId()).isEqualTo(20L);
    assertThat(expression.getDisplayOrder()).isEqualTo(1);
    assertThat(expression.getCompletedAt()).isNull();
  }

  /** 같은 프리톡 표현을 다시 완료해도 현재 세션의 최초 완료 시각을 유지한다. */
  @Test
  void completesExpressionOncePerSession() {
    FreeTalkSessionExpression expression = FreeTalkSessionExpression.link(10L, 20L, 1);

    expression.complete();
    var firstCompletedAt = expression.getCompletedAt();
    expression.complete();

    assertThat(firstCompletedAt).isNotNull();
    assertThat(expression.getCompletedAt()).isEqualTo(firstCompletedAt);
  }
}
