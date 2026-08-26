// AI 요청에 포함할 누적 대화 히스토리 메시지를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

/**
 * AI 요청에 포함할 누적 대화 히스토리 메시지를 담는다.
 *
 * @param messageId 메시지 ID
 * @param turnNumber 대화 턴 번호
 * @param role 메시지 발화자 역할
 * @param content 메시지 본문
 * @param translatedContent 번역된 메시지 본문
 * @param occurredAt 메시지가 발생한 시간대 포함 시각
 */
public record AiConversationHistoryMessage(
    Long messageId,
    int turnNumber,
    String role,
    String content,
    String translatedContent,
    @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime occurredAt) {

  /** 기존 AI 계약과 호환되는 시간 정보 없는 히스토리 메시지를 생성한다. */
  public AiConversationHistoryMessage(
      Long messageId, int turnNumber, String role, String content, String translatedContent) {
    this(messageId, turnNumber, role, content, translatedContent, null);
  }
}
