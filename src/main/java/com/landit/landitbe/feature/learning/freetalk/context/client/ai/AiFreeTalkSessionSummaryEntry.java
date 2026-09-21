// AI에 전달할 프리톡 요약 항목과 원문 근거를 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

import java.util.List;

/**
 * 요약 문장의 원문 메시지 근거를 담는다.
 *
 * @param text 요약 항목의 문장
 * @param sourceMessageIds 요약 항목의 근거 원문 메시지 ID 목록
 */
public record AiFreeTalkSessionSummaryEntry(String text, List<Long> sourceMessageIds) {
  /**
   * 요약 항목을 방어적으로 복사한다.
   *
   * @param text 요약 항목의 문장
   * @param sourceMessageIds 요약 항목의 근거 원문 메시지 ID 목록
   */
  public AiFreeTalkSessionSummaryEntry {
    sourceMessageIds = sourceMessageIds == null ? List.of() : List.copyOf(sourceMessageIds);
  }
}
