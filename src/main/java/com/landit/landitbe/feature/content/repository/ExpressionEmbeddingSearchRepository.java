// 임베딩 벡터로 공용 프리톡 표현 후보를 유사도 순으로 검색한다.

package com.landit.landitbe.feature.content.repository;

import java.util.List;

/** 임베딩 벡터로 공용 프리톡 표현 후보를 유사도 순으로 검색한다. */
public interface ExpressionEmbeddingSearchRepository {

  /**
   * 활성 공용 프리톡 표현에서 쿼리 임베딩과 가까운 후보를 코사인 거리 오름차순으로 검색한다. 사용자가 이미 학습 완료한 표현과 난이도 상한을 넘는 표현은 제외한다.
   *
   * <p>난이도는 거리 정렬보다 먼저 걸러낸다. 거리가 가장 가까운 표현이라도 상한을 넘으면 후보에 들어오지 않는다.
   *
   * @param search 검색 조건
   * @return 코사인 거리 오름차순으로 정렬한 표현 후보 목록
   */
  List<ExpressionEmbeddingMatch> searchFreeTalkCandidates(FreeTalkCandidateSearch search);
}
