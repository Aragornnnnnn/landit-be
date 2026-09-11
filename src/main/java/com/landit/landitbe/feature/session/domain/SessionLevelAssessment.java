// 세션별 텍스트 회화 수준 평가 결과를 저장하고 응답한다.

package com.landit.landitbe.feature.session.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** 다섯 영역 점수와 이번 세션의 적용 수준 변경 결과다. */
public record SessionLevelAssessment(
    DomainScore situationPerformance,
    DomainScore grammar,
    DomainScore vocabulary,
    DomainScore discourse,
    DomainScore interactionPragmatics,
    BigDecimal assessedScore,
    Integer assessedLevel,
    boolean sufficientEvidence,
    Source source,
    LearningLevelPolicy.ChangeType changeType,
    Integer previousLevel,
    Integer currentLevel,
    Details details,
    String assessmentVersion) {

  /** 측정값이 없으면 당시 적용 수준 또는 기본값을 결과 화면의 예비 수준으로 제공한다. */
  @JsonProperty("displayLevel")
  public int displayLevel() {
    return assessedLevel != null ? assessedLevel : currentLevel != null ? currentLevel : 3;
  }

  /** 한 평가 영역의 점수와 근거 충족 비율이다. */
  public record DomainScore(BigDecimal score, BigDecimal confidence) {}

  /** 사용자에게 보여줄 선택 설명이다. */
  public record Details(String strength, String improvement) {}

  /** 평가 결과를 만든 출처다. */
  public enum Source {
    MODEL,
    FALLBACK
  }
}
