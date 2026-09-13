// 업무 간에 전달할 MemoryRetrievalResult 값을 정의한다.

package com.landit.landitbe.feature.memory.retrieval.dto;

import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.retrieval.domain.MemoryRetrievalStage;
import java.util.List;

/**
 * 검색 결과와 이후 사용 trace 연결 정보를 표현한다.
 *
 * @param sessionId 검색한 프리톡 세션 ID
 * @param stage 검색을 수행한 세션 시작 단계
 * @param contexts AI에 제공할 장기기억 문맥 목록
 * @param claimed 이번 단계의 검색 marker를 선점했는지 여부
 */
public record MemoryRetrievalResult(
    long sessionId,
    MemoryRetrievalStage stage,
    List<AiFreeTalkMemoryContext> contexts,
    boolean claimed) {

  /**
   * 사용 가능한 기억이 없는 검색 결과를 만든다.
   *
   * @param sessionId 세션 ID
   * @param stage 검색 단계
   * @return 빈 검색 결과
   */
  public static MemoryRetrievalResult empty(long sessionId, MemoryRetrievalStage stage) {
    return new MemoryRetrievalResult(sessionId, stage, List.of(), false);
  }

  /**
   * 검색 결과의 사용 문맥을 방어적으로 복사한다.
   *
   * @param sessionId 검색한 프리톡 세션 ID
   * @param stage 검색을 수행한 세션 시작 단계
   * @param contexts AI에 제공할 장기기억 문맥 목록
   * @param claimed 이번 단계의 검색 marker를 선점했는지 여부
   */
  public MemoryRetrievalResult {
    contexts = contexts == null ? List.of() : List.copyOf(contexts);
  }
}
