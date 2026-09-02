// 대화 임베딩으로 추천 LLM에 전달할 표현 후보를 추린다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.config.content.ExpressionSearchProperties;
import com.landit.landitbe.feature.content.repository.ExpressionEmbeddingMatch;
import com.landit.landitbe.feature.content.repository.FreeTalkCandidateSearch;
import com.landit.landitbe.feature.content.service.ExpressionQueryService;
import com.landit.landitbe.feature.session.client.ai.AiConversationExcerpt;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 대화 임베딩으로 추천 LLM에 전달할 표현 후보를 추린다. */
@RequiredArgsConstructor
@Service
public class ExpressionCandidateSelectionService {

  private final ExpressionQueryService expressionQueryService;
  private final ExpressionSearchProperties properties;

  /**
   * 추출 발화 임베딩별 유사도 검색 결과를 병합해 후보 표현 ID를 코사인 거리 오름차순으로 선정한다.
   *
   * <p>임계값을 통과한 후보가 없으면 빈손을 막기 위해 거리 최상위 한 건을 후보로 유지한다.
   *
   * @param excerpts 대화에서 추출된 핵심 발화와 임베딩 목록
   * @param userProfileId 학습 완료 표현을 제외할 사용자 ID
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param maxDifficultyLevel 노출할 표현 난이도의 상한
   * @return 코사인 거리 오름차순의 후보 표현 ID 목록
   * @throws ApiException 검색할 수 있는 공용 표현 후보가 하나도 없을 때
   */
  public List<Long> selectCandidateIds(
      List<AiConversationExcerpt> excerpts,
      long userProfileId,
      Locale targetLocale,
      Locale baseLocale,
      int maxDifficultyLevel) {
    // 같은 표현이 여러 추출 발화에 걸리면 가장 가까운 거리 하나로 병합한다.
    Map<Long, Double> bestDistanceByExpressionId = new HashMap<>();
    for (AiConversationExcerpt excerpt : excerpts) {
      for (ExpressionEmbeddingMatch match :
          expressionQueryService.searchFreeTalkCandidatesByEmbedding(
              new FreeTalkCandidateSearch(
                  excerpt.embedding(),
                  userProfileId,
                  targetLocale,
                  baseLocale,
                  maxDifficultyLevel,
                  properties.maxCandidates()))) {
        bestDistanceByExpressionId.merge(match.expressionId(), match.distance(), Math::min);
      }
    }
    if (bestDistanceByExpressionId.isEmpty()) {
      // 공용 표현 풀에서 후보를 전혀 찾지 못한 상태는 재시도할 수 있게 실패로 전환한다.
      throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
    }

    List<Map.Entry<Long, Double>> orderedByDistance =
        bestDistanceByExpressionId.entrySet().stream()
            .sorted(
                Comparator.comparingDouble(Map.Entry<Long, Double>::getValue)
                    .thenComparing(Map.Entry::getKey))
            .toList();
    List<Long> passingIds =
        orderedByDistance.stream()
            .filter(entry -> entry.getValue() <= properties.distanceThreshold())
            .limit(properties.maxCandidates())
            .map(Map.Entry::getKey)
            .toList();
    if (passingIds.isEmpty()) {
      // 임계값을 통과한 후보가 없어도 최상위 한 건은 제공한다.
      return List.of(orderedByDistance.getFirst().getKey());
    }
    return passingIds;
  }
}
