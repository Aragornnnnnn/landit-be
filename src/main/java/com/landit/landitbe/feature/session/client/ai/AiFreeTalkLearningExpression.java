// 프리톡 신규 표현의 학습 데이터 생성 입력을 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * 프리톡 신규 표현의 학습 데이터 생성 입력을 담는다.
 *
 * @param targetExpressionText 학습 언어 표현
 * @param baseExpressionMeaningText 기준 언어 뜻
 * @param usageSummary 짧은 용법 설명
 */
public record AiFreeTalkLearningExpression(
    String targetExpressionText, String baseExpressionMeaningText, String usageSummary) {}
