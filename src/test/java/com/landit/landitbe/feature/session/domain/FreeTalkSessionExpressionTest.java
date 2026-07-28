// 프리톡 세션 표현이 기존 표현 참조와 학습 완료 상태를 직접 관리하는지 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 프리톡 세션 표현의 간소화된 저장 경계를 검증한다. */
class FreeTalkSessionExpressionTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 기존 Writing 표현을 별도 복제 없이 직접 참조한다. */
  @Test
  void referencesExistingWritingExpressionDirectly() {
    FreeTalkSessionExpression expression =
        FreeTalkSessionExpression.existing(
            10L, 20L, 1, "We really hit it off.", "우리는 정말 죽이 잘 맞았어.");

    assertThat(expression.getFreeTalkSessionId()).isEqualTo(10L);
    assertThat(expression.getWritingExpressionId()).isEqualTo(20L);
    assertThat(expression.getSourceType()).isEqualTo(FreeTalkExpressionSourceType.EXISTING);
    assertThat(expression.isCompleted()).isFalse();
  }

  /** 학습 완료 시각을 세션 표현 자체에 기록한다. */
  @Test
  void completesLearningOnSessionExpression() {
    FreeTalkSessionExpression expression =
        FreeTalkSessionExpression.existing(
            10L, 20L, 1, "We really hit it off.", "우리는 정말 죽이 잘 맞았어.");
    LocalDateTime completedAt = LocalDateTime.of(2026, 7, 28, 12, 0);

    expression.complete(completedAt);

    assertThat(expression.isCompleted()).isTrue();
    assertThat(expression.getCompletedAt()).isEqualTo(completedAt);
  }

  /** 신규 표현의 학습 콘텐츠를 세션 표현에 JSON으로 보관한다. */
  @Test
  void storesGeneratedLearningContentOnSessionExpression() throws Exception {
    JsonNode content =
        objectMapper.readTree(
            """
            {
              "targetExpressionText": "hit it off",
              "baseExpressionMeaningText": "죽이 잘 맞다"
            }
            """);

    FreeTalkSessionExpression expression =
        FreeTalkSessionExpression.generated(
            10L, 1, "We really hit it off.", "우리는 정말 죽이 잘 맞았어.", content);

    assertThat(expression.getSourceType()).isEqualTo(FreeTalkExpressionSourceType.NEW);
    assertThat(expression.getWritingExpressionId()).isNull();
    assertThat(expression.getGeneratedContentPayload()).isEqualTo(content);
  }
}
