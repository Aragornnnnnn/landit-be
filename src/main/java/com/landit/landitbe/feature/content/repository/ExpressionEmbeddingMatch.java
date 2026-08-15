// 임베딩 유사도 검색으로 찾은 표현 후보 한 건을 담는다.

package com.landit.landitbe.feature.content.repository;

/**
 * 임베딩 유사도 검색으로 찾은 표현 후보 한 건을 담는다.
 *
 * @param expressionId Writing 표현 ID
 * @param distance 쿼리 임베딩과의 코사인 거리 (0에 가까울수록 유사)
 */
public record ExpressionEmbeddingMatch(Long expressionId, double distance) {}
