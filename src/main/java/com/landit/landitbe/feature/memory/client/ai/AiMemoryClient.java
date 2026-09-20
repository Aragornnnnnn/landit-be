// 장기기억 기능이 사용하는 AI 호출 경계를 정의한다.

package com.landit.landitbe.feature.memory.client.ai;

import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingRequest;
import com.landit.landitbe.feature.memory.retrieval.client.ai.AiMemoryQueryEmbeddingResult;
import com.landit.landitbe.shared.exception.ApiException;

/** 장기기억 기능이 사용하는 AI 호출 경계를 정의한다. */
public interface AiMemoryClient {

  /**
   * 장기기억 검색 query를 임베딩한다.
   *
   * @param request 검색 query
   * @return 1,536차원 query embedding
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiMemoryQueryEmbeddingResult embedMemoryQuery(AiMemoryQueryEmbeddingRequest request);

  /**
   * 완료된 프리톡 대화에서 장기기억 후보를 추출한다.
   *
   * @param request 세션과 대화 히스토리
   * @return 검증된 장기기억 후보 목록
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiMemoryCandidatesResult extractMemoryCandidates(AiMemoryCandidatesRequest request);

  /**
   * 장기기억 후보를 기존 기억과 비교해 상태를 판정한다.
   *
   * @param request 후보와 후보별 비교 대상 기억
   * @return 후보별 ADD, SUPERSEDE 또는 IGNORE 판정
   * @throws ApiException AI 생성에 실패하거나 응답 형식이 올바르지 않을 때
   */
  AiMemoryResolutionResult resolveMemory(AiMemoryResolutionRequest request);
}
