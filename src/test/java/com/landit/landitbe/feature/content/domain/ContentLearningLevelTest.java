// 사용자 학습 레벨을 콘텐츠 노출 정책으로 변환하는 테스트

package com.landit.landitbe.feature.content.domain;

import static com.landit.landitbe.feature.content.domain.ContentLearningLevel.LEVEL_1;
import static com.landit.landitbe.feature.content.domain.ContentLearningLevel.LEVEL_2_TO_3;
import static com.landit.landitbe.feature.content.domain.ContentLearningLevel.LEVEL_4_TO_5;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ContentLearningLevelTest {

  @Test
  void mapsNullAndLevelsToQuestionGroupsAndExpressionDifficultyRanges() {
    assertAll(
        () -> assertEquals(LEVEL_1, ContentLearningLevel.from(1)),
        () -> assertEquals(LEVEL_2_TO_3, ContentLearningLevel.from(2)),
        () -> assertEquals(LEVEL_2_TO_3, ContentLearningLevel.from(3)),
        () -> assertEquals(LEVEL_4_TO_5, ContentLearningLevel.from(4)),
        () -> assertEquals(LEVEL_4_TO_5, ContentLearningLevel.from(5)),
        () -> assertEquals(LEVEL_4_TO_5, ContentLearningLevel.from(null)),
        () -> assertEquals(1, ContentLearningLevel.from(1).minimumExpressionDifficulty()),
        () -> assertEquals(1, ContentLearningLevel.from(1).maximumExpressionDifficulty()),
        () -> assertEquals(2, ContentLearningLevel.from(3).minimumExpressionDifficulty()),
        () -> assertEquals(3, ContentLearningLevel.from(3).maximumExpressionDifficulty()),
        () -> assertEquals(4, ContentLearningLevel.from(4).minimumExpressionDifficulty()),
        () -> assertEquals(5, ContentLearningLevel.from(4).maximumExpressionDifficulty()));
  }

  @Test
  void rejectsLearningLevelsOutsideSupportedRange() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> ContentLearningLevel.from(0)),
        () -> assertThrows(IllegalArgumentException.class, () -> ContentLearningLevel.from(6)));
  }
}
