// 시나리오 총평의 직전 교정과 배운 표현 후보를 보관한다.

package com.landit.landitbe.feature.learning.scenario.feedback.dto;

import com.landit.landitbe.feature.learning.expression.progress.domain.ExpressionLearningSource;
import java.time.LocalDate;
import java.util.List;

/**
 * AI 비교와 표현 재사용 판정에 제공할, 저장된 세션 근거다.
 *
 * @param previousSessionDate 직전 완료 시나리오 날짜
 * @param previousMistakes 직전 시나리오에서 실제 교정된 발화
 * @param learnedExpressions 재사용 여부를 판정할 학습 완료 표현
 */
public record ScenarioFeedbackEvidence(
    LocalDate previousSessionDate,
    List<PreviousMistake> previousMistakes,
    List<LearnedExpressionCandidate> learnedExpressions) {

  /**
   * 후보 목록을 불변으로 보관한다.
   *
   * @param previousSessionDate 직전 완료 시나리오 날짜
   * @param previousMistakes 직전 시나리오에서 실제 교정된 발화
   * @param learnedExpressions 재사용 여부를 판정할 학습 완료 표현
   */
  public ScenarioFeedbackEvidence {
    previousMistakes = List.copyOf(previousMistakes);
    learnedExpressions = List.copyOf(learnedExpressions);
  }

  /**
   * 직전 완료 시나리오에서 실제 교정된 한 발화다.
   *
   * @param messageId 교정된 사용자 메시지 ID
   * @param userMessage 교정 당시 사용자 발화
   * @param correctionExpression 실제 교정 표현
   * @param correctionReason 실제 교정 이유
   */
  public record PreviousMistake(
      long messageId, String userMessage, String correctionExpression, String correctionReason) {}

  /**
   * 사용자가 이미 완료한 표현과 표시용 출처 정보다.
   *
   * @param expressionId 학습 표현 ID
   * @param text 학습 표현 원문
   * @param meaning 표현의 기준 언어 뜻
   * @param source 표현을 학습한 기능
   * @param learnedOn 표현을 학습 완료한 날짜
   */
  public record LearnedExpressionCandidate(
      long expressionId,
      String text,
      String meaning,
      ExpressionLearningSource source,
      LocalDate learnedOn) {}
}
