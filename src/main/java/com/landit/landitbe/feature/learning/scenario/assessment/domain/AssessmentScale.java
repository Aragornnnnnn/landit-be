// 평가 척도별 집계 상한과 학습 수준 및 승급 경계를 정의한다.

package com.landit.landitbe.feature.learning.scenario.assessment.domain;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 저장된 평가 버전과 실제 응답의 척도를 구분한다. */
public enum AssessmentScale {
  LEGACY("text-level-v1.3", 5),
  SCORE("text-score-v2.0", 100);

  private final String version;
  private final int maximum;

  AssessmentScale(String version, int maximum) {
    this.version = version;
    this.maximum = maximum;
  }

  /**
   * 요청과 저장에 사용하는 평가 버전을 반환한다.
   *
   * @return 요청 헤더와 저장 이력에 사용하는 평가 버전
   */
  public String version() {
    return version;
  }

  /**
   * 해당 척도의 점수 상한을 반환한다.
   *
   * @return 해당 척도의 점수 상한
   */
  public int maximum() {
    return maximum;
  }

  /**
   * 지원하는 버전을 평가 척도로 변환한다.
   *
   * @param version 명시적으로 지원하는 계약 버전
   * @return 해당 평가 척도
   * @throws IllegalArgumentException 알려지지 않은 버전인 경우
   */
  public static AssessmentScale fromVersion(String version) {
    for (AssessmentScale scale : values()) {
      if (scale.version.equals(version)) {
        return scale;
      }
    }
    throw new IllegalArgumentException("지원하지 않는 평가 버전입니다.");
  }

  /**
   * 질문 난이도의 관찰 상한을 해당 척도로 반환한다.
   *
   * @param group 질문 난이도 그룹
   * @return 해당 척도에서 관찰 가능한 종합 점수 상한
   */
  public BigDecimal observationCap(ContentLearningLevel group) {
    int cap =
        switch (group) {
          case LEVEL_1 -> 2;
          case LEVEL_2_TO_3 -> 4;
          case LEVEL_4_TO_5, DIAGNOSTIC -> 5;
        };
    return BigDecimal.valueOf(this == LEGACY ? cap : cap * 20L);
  }

  /**
   * 해당 척도의 종합 점수를 학습 수준으로 변환한다.
   *
   * @param score 해당 척도의 종합 점수
   * @return 기존 반올림 또는 신규 20점 구간에 따른 학습 수준
   */
  public int levelForScore(BigDecimal score) {
    return this == LEGACY
        ? Math.max(1, Math.min(5, score.setScale(0, RoundingMode.HALF_UP).intValue()))
        : LearningLevelPolicy.levelForScore(score);
  }

  /**
   * 현재 수준에 대한 승급 점수 경계를 반환한다.
   *
   * @param currentLevel 현재 학습 수준
   * @return 연속 승급 신호로 인정할 최소 종합 점수
   */
  public BigDecimal promotionThreshold(int currentLevel) {
    return this == LEGACY
        ? BigDecimal.valueOf(currentLevel).add(new BigDecimal("0.70"))
        : BigDecimal.valueOf(currentLevel * 20L).add(new BigDecimal("4.00"));
  }
}
