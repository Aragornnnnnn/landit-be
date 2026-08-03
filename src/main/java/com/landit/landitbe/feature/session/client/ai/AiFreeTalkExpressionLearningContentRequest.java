// 프리톡 신규 표현 학습 데이터 생성 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 신규 표현 학습 데이터 생성 요청을 담는다.
 *
 * @param sessionId 완료된 프리톡 세션 ID
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param expressions 학습 데이터를 만들 신규 표현 목록
 */
public record AiFreeTalkExpressionLearningContentRequest(
    Long sessionId,
    String targetLocale,
    String baseLocale,
    List<AiFreeTalkLearningExpression> expressions) {}
