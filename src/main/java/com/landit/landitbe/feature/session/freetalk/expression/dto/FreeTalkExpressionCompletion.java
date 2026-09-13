// 완료 검증을 통과한 프리톡 추천 표현의 식별자를 전달한다.

package com.landit.landitbe.feature.session.freetalk.expression.dto;

/**
 * 완료 가능한 세션 표현의 식별자다.
 *
 * @param sessionExpressionId 검증된 세션 표현 ID
 */
public record FreeTalkExpressionCompletion(Long sessionExpressionId) {}
