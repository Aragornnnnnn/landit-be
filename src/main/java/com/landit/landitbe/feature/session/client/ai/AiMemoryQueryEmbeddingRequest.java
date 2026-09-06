// 장기기억 검색 query embedding 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * 장기기억 검색 query embedding 요청을 표현한다.
 *
 * @param query 검색에 사용할 자연어 query
 */
public record AiMemoryQueryEmbeddingRequest(String query) {}
