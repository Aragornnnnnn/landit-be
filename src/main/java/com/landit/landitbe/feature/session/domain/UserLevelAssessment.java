// 사용자별 세션 텍스트 수준 평가 이력을 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 한 학습 세션에서 계산한 영역별 수준과 적용 수준 변경 이력을 저장한다. */
@Getter
@Entity
@Table(name = "user_level_assessment")
public class UserLevelAssessment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "learning_session_id", nullable = false, unique = true)
  private Long learningSessionId;

  @Column(name = "situation_performance_score", precision = 3, scale = 2)
  private BigDecimal situationPerformanceScore;

  @Column(name = "situation_performance_confidence", nullable = false, precision = 3, scale = 2)
  private BigDecimal situationPerformanceConfidence;

  @Column(name = "grammar_score", precision = 3, scale = 2)
  private BigDecimal grammarScore;

  @Column(name = "grammar_confidence", nullable = false, precision = 3, scale = 2)
  private BigDecimal grammarConfidence;

  @Column(name = "vocabulary_score", precision = 3, scale = 2)
  private BigDecimal vocabularyScore;

  @Column(name = "vocabulary_confidence", nullable = false, precision = 3, scale = 2)
  private BigDecimal vocabularyConfidence;

  @Column(name = "discourse_score", precision = 3, scale = 2)
  private BigDecimal discourseScore;

  @Column(name = "discourse_confidence", nullable = false, precision = 3, scale = 2)
  private BigDecimal discourseConfidence;

  @Column(name = "interaction_pragmatics_score", precision = 3, scale = 2)
  private BigDecimal interactionPragmaticsScore;

  @Column(name = "interaction_pragmatics_confidence", nullable = false, precision = 3, scale = 2)
  private BigDecimal interactionPragmaticsConfidence;

  @Column(name = "assessed_score", precision = 3, scale = 2)
  private BigDecimal assessedScore;

  @Column(name = "assessed_level")
  private Integer assessedLevel;

  @Column(name = "sufficient_evidence", nullable = false)
  private boolean sufficientEvidence;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 20)
  private SessionLevelAssessment.Source source;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false, length = 20)
  private LearningLevelPolicy.ChangeType changeType;

  @Column(name = "previous_level")
  private Integer previousLevel;

  @Column(name = "current_level")
  private Integer currentLevel;

  @Column(name = "promotion_streak_after", nullable = false)
  private int promotionStreakAfter;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "core_payload", columnDefinition = "jsonb")
  private JsonNode corePayload;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details_payload", columnDefinition = "jsonb")
  private JsonNode detailsPayload;

  @Column(name = "assessment_version", nullable = false, length = 50)
  private String assessmentVersion;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserLevelAssessment() {}

  private UserLevelAssessment(
      Long userProfileId,
      Long learningSessionId,
      SessionLevelAssessment assessment,
      int promotionStreakAfter,
      JsonNode corePayload,
      JsonNode detailsPayload) {
    this.userProfileId = userProfileId;
    this.learningSessionId = learningSessionId;
    this.situationPerformanceScore = assessment.situationPerformance().score();
    this.situationPerformanceConfidence = assessment.situationPerformance().confidence();
    this.grammarScore = assessment.grammar().score();
    this.grammarConfidence = assessment.grammar().confidence();
    this.vocabularyScore = assessment.vocabulary().score();
    this.vocabularyConfidence = assessment.vocabulary().confidence();
    this.discourseScore = assessment.discourse().score();
    this.discourseConfidence = assessment.discourse().confidence();
    this.interactionPragmaticsScore = assessment.interactionPragmatics().score();
    this.interactionPragmaticsConfidence = assessment.interactionPragmatics().confidence();
    this.assessedScore = assessment.assessedScore();
    this.assessedLevel = assessment.assessedLevel();
    this.sufficientEvidence = assessment.sufficientEvidence();
    this.source = assessment.source();
    this.changeType = assessment.changeType();
    this.previousLevel = assessment.previousLevel();
    this.currentLevel = assessment.currentLevel();
    this.promotionStreakAfter = promotionStreakAfter;
    this.corePayload = corePayload;
    this.detailsPayload = detailsPayload;
    this.assessmentVersion = assessment.assessmentVersion();
  }

  /**
   * 계산이 끝난 세션 수준 평가 이력을 생성한다.
   *
   * @param userProfileId 평가 대상 사용자 ID
   * @param learningSessionId 평가한 학습 세션 ID
   * @param assessment 계산된 수준 평가
   * @param promotionStreakAfter 평가 적용 후 승급 연속 횟수
   * @param corePayload 검증된 질문별 Core 평가
   * @param detailsPayload 선택 설명
   * @return 저장할 사용자 수준 평가 이력
   */
  public static UserLevelAssessment completed(
      Long userProfileId,
      Long learningSessionId,
      SessionLevelAssessment assessment,
      int promotionStreakAfter,
      JsonNode corePayload,
      JsonNode detailsPayload) {
    return new UserLevelAssessment(
        userProfileId,
        learningSessionId,
        assessment,
        promotionStreakAfter,
        corePayload,
        detailsPayload);
  }

  /**
   * 저장된 평가 이력을 API 응답용 수준 평가로 변환한다.
   *
   * @return 세션 수준 평가 응답 값
   */
  public SessionLevelAssessment toAssessment() {
    return new SessionLevelAssessment(
        domain(situationPerformanceScore, situationPerformanceConfidence),
        domain(grammarScore, grammarConfidence),
        domain(vocabularyScore, vocabularyConfidence),
        domain(discourseScore, discourseConfidence),
        domain(interactionPragmaticsScore, interactionPragmaticsConfidence),
        assessedScore,
        assessedLevel,
        sufficientEvidence,
        source,
        changeType,
        previousLevel,
        currentLevel,
        details(),
        assessmentVersion);
  }

  private SessionLevelAssessment.DomainScore domain(BigDecimal score, BigDecimal confidence) {
    return new SessionLevelAssessment.DomainScore(score, confidence);
  }

  private SessionLevelAssessment.Details details() {
    if (detailsPayload == null) {
      return null;
    }
    String strength = detailsPayload.path("strength").asText(null);
    String improvement = detailsPayload.path("improvement").asText(null);
    return strength == null || improvement == null
        ? null
        : new SessionLevelAssessment.Details(strength, improvement);
  }
}
