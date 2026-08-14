// 표현 임베딩 유사도 검색 설정 값을 바인딩한다.

package com.landit.landitbe.config.content;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 표현 임베딩 유사도 검색 설정 값을 바인딩한다.
 *
 * @param mode 검색 실행 모드 (pgvector 또는 in-memory)
 * @param maxCandidates 추천 LLM에 전달할 최대 후보 수
 * @param distanceThreshold 후보로 인정할 최대 코사인 거리. 배포 후 실측으로 조정하는 튜닝 값이다.
 */
@ConfigurationProperties(prefix = "landit.expression-search")
public record ExpressionSearchProperties(
    String mode, Integer maxCandidates, Double distanceThreshold) {

  /** 비어 있는 검색 모드와 튜닝 값을 기본값으로 정규화한다. */
  public ExpressionSearchProperties {
    if (mode == null || mode.isBlank()) {
      mode = "in-memory";
    }
    if (maxCandidates == null || maxCandidates <= 0) {
      maxCandidates = 30;
    }
    // 코사인 거리 유효 범위(0~2)를 벗어나면 기본값으로 정규화한다. 0.0은 유효한 임계값이라 유지한다.
    if (distanceThreshold == null
        || distanceThreshold.isNaN()
        || distanceThreshold < 0
        || distanceThreshold > 2) {
      distanceThreshold = 0.6;
    }
  }
}
