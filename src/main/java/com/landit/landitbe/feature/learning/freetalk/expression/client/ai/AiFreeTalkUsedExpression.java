// AI가 이번 대화에서 사용자가 다시 썼다고 판정한 배운 표현 하나를 담는다.

package com.landit.landitbe.feature.learning.freetalk.expression.client.ai;

/**
 * AI가 이번 대화에서 사용자가 다시 썼다고 판정한 배운 표현이다. AI의 주장일 뿐이라 저장하기 전에 후보·발화·원문과 맞는지 다시 확인해야 한다.
 *
 * @param expressionId 다시 쓴 표현 ID. 요청의 배운 표현 후보 중 하나여야 한다. 예: 812
 * @param messageId 표현을 쓴 사용자 발화 ID. 예: 5504
 * @param matchedText 발화 원문에서 표현에 해당하는 부분. 예: "grabbed a coffee"
 */
public record AiFreeTalkUsedExpression(Long expressionId, Long messageId, String matchedText) {}
