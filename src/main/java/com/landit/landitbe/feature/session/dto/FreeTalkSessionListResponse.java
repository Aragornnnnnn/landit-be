// 지난 프리톡 목록의 페이지 응답을 표현한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.ExpressionLearningStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 지난 프리톡 목록의 페이지 응답을 표현한다.
 *
 * @param items 완료된 프리톡 요약 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param hasNext 다음 페이지 존재 여부
 */
public record FreeTalkSessionListResponse(List<Item> items, int page, int size, boolean hasNext) {

  /**
   * 목록의 프리톡 세션 요약이다.
   *
   * @param sessionId 프리톡 학습 세션 ID
   * @param title 프리톡 제목
   * @param characterId 선택한 프리톡 캐릭터 식별자
   * @param startedAt 세션 시작 시각
   * @param completedAt 세션 완료 시각
   * @param userSpeakingDurationMs 세션의 사용자 발화 시간 합계
   * @param expressionGenerationStatus 맞춤 표현 생성 상태
   * @param expressionLearningStatus 맞춤 표현 학습 상태
   * @param expressionCount 연결된 맞춤 표현 수
   * @param completedExpressionCount 완료한 맞춤 표현 수
   */
  public record Item(
      Long sessionId,
      String title,
      String characterId,
      LocalDateTime startedAt,
      LocalDateTime completedAt,
      long userSpeakingDurationMs,
      ExpressionGenerationStatus expressionGenerationStatus,
      ExpressionLearningStatus expressionLearningStatus,
      int expressionCount,
      int completedExpressionCount) {}
}
