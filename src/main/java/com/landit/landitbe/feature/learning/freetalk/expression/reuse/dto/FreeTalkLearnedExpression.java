// 표현 재사용 판정에 후보로 보낼, 사용자가 이전에 배운 표현 하나를 전달한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto;

import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import java.time.LocalDate;

/**
 * 사용자가 이전에 배운 표현이다. AI에는 표현 ID·원문·뜻만 보내고, 나머지는 재사용 기록에 출처를 남길 때 쓴다.
 *
 * @param expressionId 표현 ID. 예: 812
 * @param text 학습 언어 표현. 예: "grab a coffee"
 * @param meaning 기준 언어 뜻. 예: "커피 한잔하다"
 * @param sourceType 배운 곳. 예: SCENARIO
 * @param scenarioId 배운 시나리오 ID. 스몰톡에서 배웠으면 null. 예: 41
 * @param learnedOn 처음 학습을 마친 날. 예: 2026-09-10
 */
public record FreeTalkLearnedExpression(
    long expressionId,
    String text,
    String meaning,
    FreeTalkExpressionReuseSource sourceType,
    Long scenarioId,
    LocalDate learnedOn) {}
