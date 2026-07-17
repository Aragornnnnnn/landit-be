// 시나리오 세션에만 필요한 정보를 저장한다.

package com.landit.landitbe.session.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 시나리오 세션에만 필요한 정보를 저장한다. */
@Entity
@Table(name = "scenario_session")
public class ScenarioSession extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "learning_session_id", nullable = false)
  private Long learningSessionId;

  @Column(name = "scenario_language_variant_id", nullable = false)
  private Long scenarioLanguageVariantId;

  @Column(name = "user_opening_instruction_snapshot", columnDefinition = "text")
  private String userOpeningInstructionSnapshot;

  @Enumerated(EnumType.STRING)
  @Column(name = "goal_completion_status", length = 20)
  private GoalCompletionStatus goalCompletionStatus;

  /** 동작을 수행한다. */
  protected ScenarioSession() {}

  private ScenarioSession(
      Long learningSessionId,
      Long scenarioLanguageVariantId,
      String userOpeningInstructionSnapshot,
      GoalCompletionStatus goalCompletionStatus) {
    this.learningSessionId = learningSessionId;
    this.scenarioLanguageVariantId = scenarioLanguageVariantId;
    this.userOpeningInstructionSnapshot = userOpeningInstructionSnapshot;
    this.goalCompletionStatus = goalCompletionStatus;
  }

  /** 새 시나리오 세션 보조 정보를 생성한다. */
  public static ScenarioSession start(
      Long learningSessionId,
      Long scenarioLanguageVariantId,
      String userOpeningInstructionSnapshot) {
    return new ScenarioSession(
        learningSessionId,
        scenarioLanguageVariantId,
        userOpeningInstructionSnapshot,
        GoalCompletionStatus.NOT_STARTED);
  }

  /** AI가 판단한 시나리오 목표 달성 상태를 갱신한다. */
  public void updateGoalCompletionStatus(GoalCompletionStatus goalCompletionStatus) {
    this.goalCompletionStatus = goalCompletionStatus;
  }
}
