// 사용자 학습 레벨에 따른 콘텐츠 노출 그룹을 정의한다.

package com.landit.landitbe.feature.content.domain;

/** 사용자 학습 레벨을 시나리오 질문 그룹과 표현 난이도 범위로 변환한다. */
public enum ContentLearningLevel {
  LEVEL_1(1, 1),
  LEVEL_2_TO_3(2, 3),
  LEVEL_4_TO_5(4, 5);

  private final int minimumExpressionDifficulty;
  private final int maximumExpressionDifficulty;

  ContentLearningLevel(int minimumExpressionDifficulty, int maximumExpressionDifficulty) {
    this.minimumExpressionDifficulty = minimumExpressionDifficulty;
    this.maximumExpressionDifficulty = maximumExpressionDifficulty;
  }

  /**
   * 사용자 프로필의 학습 레벨을 콘텐츠 레벨 그룹으로 변환한다.
   *
   * @param userLearningLevel 사용자 프로필의 학습 레벨. 미설정이면 {@code null}
   * @return 질문 그룹과 표현 난이도 범위를 담은 콘텐츠 레벨
   * @throws IllegalArgumentException 학습 레벨이 1부터 5 사이가 아닐 때
   */
  public static ContentLearningLevel from(Integer userLearningLevel) {
    if (userLearningLevel == null) {
      return LEVEL_4_TO_5;
    }
    return switch (userLearningLevel) {
      case 1 -> LEVEL_1;
      case 2, 3 -> LEVEL_2_TO_3;
      case 4, 5 -> LEVEL_4_TO_5;
      default -> throw new IllegalArgumentException("학습 레벨은 1부터 5 사이여야 합니다.");
    };
  }

  /** 현재 콘텐츠 레벨에서 노출할 표현의 최소 난이도를 반환한다. */
  public int minimumExpressionDifficulty() {
    return minimumExpressionDifficulty;
  }

  /**
   * 현재 콘텐츠 레벨에서 노출할 표현의 최대 난이도를 반환한다.
   *
   * @return 노출 가능한 표현의 최대 난이도
   */
  public int maximumExpressionDifficulty() {
    return maximumExpressionDifficulty;
  }

  /** 주어진 표현 난이도가 현재 콘텐츠 레벨에 속하는지 반환한다. */
  public boolean includesExpressionDifficulty(int difficultyLevel) {
    return minimumExpressionDifficulty <= difficultyLevel
        && difficultyLevel <= maximumExpressionDifficulty;
  }
}
