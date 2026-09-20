// AI에 전달할 프리톡 세션 요약 내용을 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

import java.util.List;

/** 프리톡 세션의 주제와 미완료 맥락을 보관한다. */
public record AiFreeTalkSessionSummaryContent(
    String topic,
    List<AiFreeTalkSessionSummaryEntry> userStatements,
    List<AiFreeTalkSessionSummaryEntry> openThreads,
    List<AiFreeTalkSessionSummaryEntry> interactionContext) {
  /** 목록을 방어적으로 복사한다. */
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
