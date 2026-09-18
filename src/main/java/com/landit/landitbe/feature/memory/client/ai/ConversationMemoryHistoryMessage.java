// 장기기억 생성 AI 요청에 포함할 대화 이력 메시지를 담는다.

package com.landit.landitbe.feature.memory.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

/** 장기기억 생성 AI 요청에 포함할 대화 이력 메시지를 담는다. */
public record ConversationMemoryHistoryMessage(
    Long messageId,
    int turnNumber,
    String role,
    String content,
    String translatedContent,
    @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime occurredAt) {

  /** 시간 정보가 없는 호환용 장기기억 이력 메시지를 만든다. */
  public ConversationMemoryHistoryMessage(
      Long messageId, int turnNumber, String role, String content, String translatedContent) {
    this(messageId, turnNumber, role, content, translatedContent, null);
  }
}
