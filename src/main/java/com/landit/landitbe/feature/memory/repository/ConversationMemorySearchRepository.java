// 사용자와 캐릭터 범위가 적용된 장기기억 검색 계약을 정의한다.

package com.landit.landitbe.feature.memory.repository;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
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

  /**
   * 후보와 같은 의미 유형·캐릭터 범위의 활성 기억을 exact 코사인 거리로 검색한다.
   *
   * <p>비교 검색은 같은 유형의 활성 기억을 모두 대상으로 한다.
   *
   * @param queryEmbedding 검색 쿼리 1,536차원 임베딩
   * @param userProfileId 검색 대상 사용자 프로필 ID
   * @param characterId 정확히 일치할 캐릭터 ID. PROFILE은 null이다.
   * @param memoryType 비교할 장기기억 의미 유형
   * @param limit 반환할 최대 결과 수
   * @return 거리와 장기기억 ID 순으로 정렬한 비교 대상
   * @throws IllegalArgumentException 입력 임베딩·범위·제한이 유효하지 않은 경우
   */
  List<ConversationMemoryMatch> searchActiveComparable(
      List<Float> queryEmbedding,
      long userProfileId,
      String characterId,
      ConversationMemoryType memoryType,
      int limit);
}
