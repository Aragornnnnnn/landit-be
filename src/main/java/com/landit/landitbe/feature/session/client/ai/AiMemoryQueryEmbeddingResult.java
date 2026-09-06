// 장기기억 검색 query embedding 결과를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 장기기억 검색 query embedding 결과를 표현한다.
 *
 * @param embeddingModel embedding 생성에 사용한 모델 식별자
 * @param embedding 1,536차원 embedding
 */
public record AiMemoryQueryEmbeddingResult(String embeddingModel, List<Float> embedding) {}
