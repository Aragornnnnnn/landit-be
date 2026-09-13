// 업무 간에 전달할 MemoryRetrievalRequest 값을 정의한다.

package com.landit.landitbe.feature.memory.retrieval.dto;

import com.landit.landitbe.feature.memory.retrieval.domain.MemoryRetrievalStage;

/**
 * 장기기억 검색에 필요한 입력을 표현한다.
 *
 * @param sessionId 검색할 프리톡 세션 ID
 * @param userProfileId 검색 대상 사용자 프로필 ID
 * @param characterId 검색할 캐릭터 ID
 * @param stage 검색을 수행하는 세션 시작 단계
 * @param query 임베딩으로 변환할 자연어 검색 질의
 */
public record MemoryRetrievalRequest(
    long sessionId,
    long userProfileId,
    String characterId,
    MemoryRetrievalStage stage,
    String query) {}
