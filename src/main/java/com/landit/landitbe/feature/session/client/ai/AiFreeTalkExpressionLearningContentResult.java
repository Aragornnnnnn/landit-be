// 프리톡 신규 표현 학습 데이터 생성 결과를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 신규 표현 학습 데이터 생성 결과를 담는다.
 *
 * @param expressions 생성된 신규 표현 학습 데이터
 */
public record AiFreeTalkExpressionLearningContentResult(
    List<AiFreeTalkExpressionLearningContent> expressions) {}
