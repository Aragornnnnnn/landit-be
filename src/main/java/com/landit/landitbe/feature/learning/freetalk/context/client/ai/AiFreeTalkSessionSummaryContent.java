// AI에 전달할 프리톡 세션 요약 내용을 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

import java.util.List;

/**
 * 프리톡 세션의 주제와 미완료 맥락을 보관한다.
 *
 * @param topic 대화의 주요 주제
 * @param userStatements 사용자 원문에 근거한 진술
 * @param openThreads 아직 끝나지 않은 대화 주제
 * @param interactionContext 이후 대화에 필요한 상호작용 맥락
 */
public record AiFreeTalkSessionSummaryContent(
    String topic,
    List<AiFreeTalkSessionSummaryEntry> userStatements,
    List<AiFreeTalkSessionSummaryEntry> openThreads,
    List<AiFreeTalkSessionSummaryEntry> interactionContext) {
  /**
   * 목록을 방어적으로 복사한다.
   *
   * @param topic 대화의 주요 주제
   * @param userStatements 사용자 원문에 근거한 진술
   * @param openThreads 아직 끝나지 않은 대화 주제
   * @param interactionContext 이후 대화에 필요한 상호작용 맥락
   */
  public AiFreeTalkSessionSummaryContent {
    userStatements = copy(userStatements);
    openThreads = copy(openThreads);
    interactionContext = copy(interactionContext);
  }

  private static List<AiFreeTalkSessionSummaryEntry> copy(
      List<AiFreeTalkSessionSummaryEntry> entries) {
    return entries == null ? List.of() : List.copyOf(entries);
  }
}
