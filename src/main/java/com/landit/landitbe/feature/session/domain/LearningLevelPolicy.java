// 세션 평가 점수로 사용자 적용 수준과 승급 연속 횟수를 결정한다.

package com.landit.landitbe.feature.session.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 최초 수준 확정과 두 번 연속 상향 근거에 따른 한 단계 승급을 계산한다. */
public final class LearningLevelPolicy {

  private static final BigDecimal PROMOTION_GAP = new BigDecimal("0.70");
  private static final BigDecimal PROMOTION_CONFIDENCE = new BigDecimal("0.75");

  private LearningLevelPolicy() {}

  /** 이번 평가로 발생한 적용 수준 변경 유형이다. */
  public enum ChangeType {
    INITIALIZED,
    PROMOTED,
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
      boolean modelResult) {
    if (!modelResult) {
      return new Decision(currentLevel, promotionStreak, ChangeType.NOT_APPLIED);
    }
    if (currentLevel == null) {
      int initializedLevel =
          Math.max(1, Math.min(5, assessedScore.setScale(0, RoundingMode.HALF_UP).intValue()));
      return new Decision(initializedLevel, 0, ChangeType.INITIALIZED);
    }
    if (currentLevel < 5
        && assessmentConfidence.compareTo(PROMOTION_CONFIDENCE) >= 0
        && assessedScore.compareTo(BigDecimal.valueOf(currentLevel).add(PROMOTION_GAP)) >= 0) {
      int nextStreak = promotionStreak + 1;
      return nextStreak >= 2
          ? new Decision(currentLevel + 1, 0, ChangeType.PROMOTED)
          : new Decision(currentLevel, nextStreak, ChangeType.UNCHANGED);
    }
    return new Decision(currentLevel, 0, ChangeType.UNCHANGED);
  }
}
