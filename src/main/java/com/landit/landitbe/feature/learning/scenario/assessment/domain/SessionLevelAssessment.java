// 세션별 텍스트 회화 수준 평가 결과를 저장하고 응답한다.

package com.landit.landitbe.feature.learning.scenario.assessment.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** 다섯 영역 점수와 이번 세션의 적용 수준 변경 결과다. */
public record SessionLevelAssessment(
    DomainScore situationPerformance,
    DomainScore grammar,
    DomainScore vocabulary,
    DomainScore discourse,
    DomainScore interactionPragmatics,
    @Schema(
            description = "종합 점수. scoreMax로 척도를 구분하며 미평가 시 null",
            minimum = "1",
            maximum = "100",
            nullable = true)
        BigDecimal assessedScore,
    @Schema(description = "종합 점수에 대응하는 학습 레벨", minimum = "1", maximum = "5", nullable = true)
        Integer assessedLevel,
    boolean sufficientEvidence,
    Source source,
    LearningLevelPolicy.ChangeType changeType,
    Integer previousLevel,
    Integer currentLevel,
    Details details,
    String assessmentVersion) {

  /**
   * 저장 당시 평가 버전의 점수 상한을 반환한다. 과거 점수는 환산하지 않는다.
   *
   * @return 신규 100점 평가이면 100, 기존 5점 평가이면 5
   */
  @JsonProperty("scoreMax")
  @Schema(
      description = "영역별 점수와 종합 점수의 상한. 과거 평가는 5, 신규 평가는 100",
      allowableValues = {"5", "100"})
  public int scoreMax() {
    return "text-score-v2.0".equals(assessmentVersion) ? 100 : 5;
  }

  /** 측정값이 없으면 당시 적용 수준 또는 기본값을 결과 화면의 예비 수준으로 제공한다. */
  @JsonProperty("displayLevel")
  public int displayLevel() {
    return assessedLevel != null ? assessedLevel : currentLevel != null ? currentLevel : 3;
  }

  /** 한 평가 영역의 점수와 근거 충족 비율이다. */
  public record DomainScore(
      @Schema(
              description = "영역별 점수. 상위 scoreMax 척도를 따르며 미관찰 시 null",
              minimum = "1",
              maximum = "100",
              nullable = true)
          BigDecimal score,
      @Schema(description = "관찰 근거 비율. 정답 확률이 아님", minimum = "0", maximum = "1")
          BigDecimal confidence) {}

  /** 사용자에게 보여줄 선택 설명이다. */
  public record Details(String strength, String improvement) {}

  /** 평가 결과를 만든 출처다. */
  public enum Source {
    MODEL,
    FALLBACK
  }
}
