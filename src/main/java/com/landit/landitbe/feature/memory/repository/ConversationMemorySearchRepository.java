// 사용자와 캐릭터 범위가 적용된 장기기억 검색 계약을 정의한다.

package com.landit.landitbe.feature.memory.repository;

import java.util.List;

/** 사용자와 캐릭터 범위를 먼저 적용해 활성 장기기억을 검색한다. */
public interface ConversationMemorySearchRepository {

  /**
   * 활성 장기기억을 exact 코사인 거리 오름차순으로 검색한다.
   *
   * @param userProfileId 검색 대상 사용자 프로필 ID
   * @param characterId 현재 대화 캐릭터 ID
   * @param queryEmbedding 검색 쿼리 1,536차원 임베딩
   * @param limit 반환할 최대 결과 수
   * @return 거리와 장기기억 ID 순으로 정렬한 검색 결과
   * @throws IllegalArgumentException 입력 임베딩·범위·제한이 유효하지 않은 경우
   */
  List<ConversationMemoryMatch> searchActive(
      long userProfileId, String characterId, List<Float> queryEmbedding, int limit);
}
