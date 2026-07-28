// 지난 프리톡 목록의 페이지 응답을 표현한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.ExpressionLearningStatus;
import java.time.LocalDateTime;
import java.util.List;

/** 지난 프리톡 목록의 페이지 응답을 표현한다. */
public record FreeTalkSessionListResponse(List<Item> items, int page, int size, boolean hasNext) {

  /** 목록의 프리톡 세션 요약이다. */
  public record Item(
      Long sessionId,
      String title,
      LocalDateTime startedAt,
      LocalDateTime completedAt,
      long userSpeakingDurationMs,
      ExpressionGenerationStatus expressionGenerationStatus,
      ExpressionLearningStatus expressionLearningStatus,
      int expressionCount,
      int completedExpressionCount) {}
}
