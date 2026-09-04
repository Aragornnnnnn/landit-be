// 세션별 텍스트 회화 수준 평가 결과를 저장하고 응답한다.

package com.landit.landitbe.feature.session.domain;

import java.math.BigDecimal;

/** 다섯 영역 점수와 이번 세션의 적용 수준 변경 결과다. */
public record SessionLevelAssessment(
    DomainScore situationPerformance,
    DomainScore grammar,
    DomainScore vocabulary,
    DomainScore discourse,
    DomainScore interactionPragmatics,
    BigDecimal assessedScore,
    int assessedLevel,
    Source source,
    LearningLevelPolicy.ChangeType changeType,
    Integer previousLevel,
    Integer currentLevel,
    Details details,
    String assessmentVersion) {

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
