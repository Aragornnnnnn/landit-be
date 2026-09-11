// 유효한 최신 세션 평가로 기존 사용자 수준을 즉시 대체한다.

package com.landit.landitbe.feature.session.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 충분한 평가 근거가 있으면 기존 설정과 무관하게 평가한 수준을 적용한다. */
public final class LearningLevelPolicy {

  private static final BigDecimal MINIMUM_CONFIDENCE = new BigDecimal("0.75");

  private LearningLevelPolicy() {}

  /** 이번 평가로 발생한 적용 수준 변경 유형이다. */
  public enum ChangeType {
    INITIALIZED,
    PROMOTED,
    DEMOTED,
    UNCHANGED,
    NOT_APPLIED
  }

  /** 평가 적용 후 수준과 승급 연속 횟수다. */
  public record Decision(Integer level, int promotionStreak, ChangeType changeType) {}

  /** 현재 수준과 모델 점수에서 이번 세션 이후 적용 상태를 반환한다. */
  public static Decision apply(
      Integer currentLevel,
      int promotionStreak,
      BigDecimal assessedScore,
      BigDecimal assessmentConfidence,
      boolean sufficientEvidence) {
    if (!sufficientEvidence
        || assessedScore == null
        || assessmentConfidence == null
        || assessmentConfidence.compareTo(MINIMUM_CONFIDENCE) < 0) {
      return new Decision(currentLevel, promotionStreak, ChangeType.NOT_APPLIED);
    }
    int assessedLevel =
        Math.max(1, Math.min(5, assessedScore.setScale(0, RoundingMode.HALF_UP).intValue()));
    ChangeType changeType =
        currentLevel == null
            ? ChangeType.INITIALIZED
            : assessedLevel > currentLevel
                ? ChangeType.PROMOTED
                : assessedLevel < currentLevel ? ChangeType.DEMOTED : ChangeType.UNCHANGED;
    return new Decision(assessedLevel, 0, changeType);
  }
}
