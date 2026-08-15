// 임베딩 벡터로 공용 프리톡 표현 후보를 유사도 순으로 검색한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.shared.domain.Locale;
import java.util.List;

/** 임베딩 벡터로 공용 프리톡 표현 후보를 유사도 순으로 검색한다. */
public interface ExpressionEmbeddingSearchRepository {

  /**
   * 활성 공용 프리톡 표현에서 쿼리 임베딩과 가까운 후보를 코사인 거리 오름차순으로 검색한다. 사용자가 이미 학습 완료한 표현은 제외한다.
   *
   * @param embedding 쿼리 임베딩 벡터 (1,536차원)
   * @param userProfileId 학습 완료 표현을 제외할 사용자 ID
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param limit 최대 후보 수
   * @return 코사인 거리 오름차순으로 정렬한 표현 후보 목록
   */
  List<ExpressionEmbeddingMatch> searchFreeTalkCandidates(
      List<Float> embedding, long userProfileId, Locale targetLocale, Locale baseLocale, int limit);
}
