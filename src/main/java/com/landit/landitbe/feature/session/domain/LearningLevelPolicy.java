// 최초 평가 수준 확정과 이후 두 번 연속 상향 근거에 따른 한 단계 승급을 계산한다.

package com.landit.landitbe.feature.session.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 최초 유효 평가만 기존 선택 수준을 대체하고 이후에는 승급 정책을 적용한다. */
public final class LearningLevelPolicy {

  private static final BigDecimal MINIMUM_CONFIDENCE = new BigDecimal("0.75");
  private static final BigDecimal PROMOTION_GAP = new BigDecimal("0.70");

  private LearningLevelPolicy() {}

  /** 이번 평가로 발생한 적용 수준 변경 유형이다. */
  public enum ChangeType {
    INITIALIZED,
    PROMOTED,
    /** 과거 평가 조회 호환을 위해 보존하며 새 평가에서는 반환하지 않는다. */
    DEMOTED,
    UNCHANGED,
    NOT_APPLIED
  }

  /** 평가 적용 후 수준과 승급 연속 횟수다. */
  public record Decision(Integer level, int promotionStreak, ChangeType changeType) {}

  /**
   * 최초 확정 여부와 모델 점수에서 이번 세션 이후 적용 상태를 반환한다.
   *
   * @param currentLevel 기존 적용 수준 또는 자가선택 수준
   * @param promotionStreak 기존 연속 승급 신호 횟수
   * @param assessedScore 관찰 상한을 반영한 종합 점수
   * @param assessmentConfidence 가중 관찰 비율
   * @param sufficientEvidence 모든 평가 영역의 근거가 충분한지 여부
   * @param levelInitialized 평가로 수준을 이미 확정한 이력이 있는지 여부
   * @return 적용 수준, 연속 승급 신호 및 변경 유형
   */
  public static Decision apply(
      Integer currentLevel,
      int promotionStreak,
      BigDecimal assessedScore,
      BigDecimal assessmentConfidence,
      boolean sufficientEvidence,
      boolean levelInitialized) {
    if (!sufficientEvidence
        || assessedScore == null
        || assessmentConfidence == null
        || assessmentConfidence.compareTo(MINIMUM_CONFIDENCE) < 0) {
      return new Decision(currentLevel, promotionStreak, ChangeType.NOT_APPLIED);
    }
    if (!levelInitialized || currentLevel == null) {
      int initializedLevel =
          Math.max(1, Math.min(5, assessedScore.setScale(0, RoundingMode.HALF_UP).intValue()));
      return new Decision(initializedLevel, 0, ChangeType.INITIALIZED);
    }
    if (currentLevel < 5
        && assessedScore.compareTo(BigDecimal.valueOf(currentLevel).add(PROMOTION_GAP)) >= 0) {
      int nextStreak = promotionStreak + 1;
      return nextStreak >= 2
          ? new Decision(currentLevel + 1, 0, ChangeType.PROMOTED)
          : new Decision(currentLevel, nextStreak, ChangeType.UNCHANGED);
    }
    return new Decision(currentLevel, 0, ChangeType.UNCHANGED);
  }
}
