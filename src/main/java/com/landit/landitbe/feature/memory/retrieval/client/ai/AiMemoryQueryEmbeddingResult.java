// 장기기억 검색 query 임베딩 결과를 담는다.

package com.landit.landitbe.feature.memory.retrieval.client.ai;

import java.util.List;

/** 장기기억 검색 query 임베딩 결과를 담는다. */
public record AiMemoryQueryEmbeddingResult(String embeddingModel, List<Float> embedding) {}
