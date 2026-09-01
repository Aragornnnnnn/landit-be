// 학습 수준을 표현 난이도 상한으로 옮기는 규칙을 검증한다.

package com.landit.landitbe.feature.content.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 학습 수준을 표현 난이도 상한으로 옮기는 규칙을 검증한다. */
class ExpressionDifficultyPolicyTest {

  @DisplayName("학습 수준 1~3은 난이도 3 이하 표현만 받는다.")
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void limitsEasyBucketToDifficultyThree(int learningLevel) {
    assertThat(ExpressionDifficultyPolicy.maxDifficultyFor(learningLevel)).isEqualTo(3);
  }

  @DisplayName("학습 수준 4~5는 모든 난이도의 표현을 받는다.")
  @ParameterizedTest
  @ValueSource(ints = {4, 5})
  void allowsEveryDifficultyForAdvancedLearners(int learningLevel) {
    assertThat(ExpressionDifficultyPolicy.maxDifficultyFor(learningLevel)).isEqualTo(5);
  }

  @DisplayName("학습 수준을 모르면 후보를 좁히지 않는다.")
  @Test
  void allowsEveryDifficultyWhenLearningLevelIsUnknown() {
    assertThat(ExpressionDifficultyPolicy.maxDifficultyFor(null)).isEqualTo(5);
  }
}
