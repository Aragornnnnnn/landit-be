// 프리톡 맞춤 표현 추천 요청을 담는다.

package com.landit.landitbe.feature.learning.freetalk.expression.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import java.util.List;

/**
 * 프리톡 맞춤 표현 추천 요청을 담는다.
 *
 * <p>배운 표현 후보는 표현 재사용 판정에만 쓰이고 추천에는 쓰이지 않는다. 비어 있으면 요청 JSON에 싣지 않아, 보낼 것이 없는 요청은 이 필드가 생기기 전과 글자
 * 그대로 같다. AI 서버의 이 요청 모델은 모르는 필드를 거부하지 않으므로, 필드를 모르는 구버전 서버는 후보를 무시하고 추천만 돌려준다.
 *
 * @param sessionId 완료된 프리톡 세션 ID
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param conversationHistory 완료된 세션의 누적 대화
 * @param existingExpressions 재사용 가능한 기존 표현 후보
 * @param learnedExpressions 사용자가 이전에 배운 표현 후보. 최대 50개, 표현 ID는 서로 달라야 한다
 */
public record AiFreeTalkExpressionRecommendationsRequest(
    Long sessionId,
    String targetLocale,
    String baseLocale,
    List<AiConversationHistoryMessage> conversationHistory,
    List<AiFreeTalkExistingExpression> existingExpressions,
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<AiFreeTalkLearnedExpression> learnedExpressions) {

  /** AI 서버가 받는 배운 표현 후보 상한이다. */
  public static final int MAX_LEARNED_EXPRESSIONS = 50;

  /**
   * 배운 표현 후보를 null 없이 불변으로 보관한다.
   *
   * @throws IllegalArgumentException 배운 표현 후보가 AI 서버 계약의 상한을 넘거나 표현 ID가 겹칠 때
   */
  public AiFreeTalkExpressionRecommendationsRequest {
    learnedExpressions = learnedExpressions == null ? List.of() : List.copyOf(learnedExpressions);
    requireValidLearnedExpressions(learnedExpressions);
  }

  /** 배운 표현 후보 없이 추천만 요청한다. */
  public AiFreeTalkExpressionRecommendationsRequest(
      Long sessionId,
      String targetLocale,
      String baseLocale,
      List<AiConversationHistoryMessage> conversationHistory,
      List<AiFreeTalkExistingExpression> existingExpressions) {
    this(sessionId, targetLocale, baseLocale, conversationHistory, existingExpressions, List.of());
  }

  /**
   * 배운 표현 후보가 AI 서버 계약을 지키는지 확인한다. 요청을 만들기 전에 미리 확인하려는 쪽도 쓴다.
   *
   * @param learnedExpressions 확인할 배운 표현 후보
   * @throws IllegalArgumentException 상한을 넘거나 표현 ID가 겹칠 때
   */
  public static void requireValidLearnedExpressions(
      List<AiFreeTalkLearnedExpression> learnedExpressions) {
    if (learnedExpressions.size() > MAX_LEARNED_EXPRESSIONS) {
      throw new IllegalArgumentException(
          "learnedExpressions must not exceed " + MAX_LEARNED_EXPRESSIONS);
    }
    if (learnedExpressions.stream()
            .map(AiFreeTalkLearnedExpression::expressionId)
            .distinct()
            .count()
        != learnedExpressions.size()) {
      throw new IllegalArgumentException("learnedExpressions must have unique expressionIds");
    }
  }
}
