// AI 요청에 포함할 누적 대화 히스토리 메시지를 담는다.

package com.landit.landitbe.feature.learning.conversation.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * @param messageSequence BE 내부의 실제 메시지 순서. AI JSON에는 포함하지 않는다.
 */
public record AiConversationHistoryMessage(
    Long messageId,
    int turnNumber,
    String role,
    String content,
    String translatedContent,
    @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime occurredAt,
    @JsonIgnore Integer messageSequence) {

  /**
   * 메시지 순서가 필요 없는 기존 호출부의 계약을 유지한다.
   *
   * @param messageId 메시지 ID
   * @param turnNumber 대화 턴 번호
   * @param role 발화 주체
   * @param content 원문
   * @param translatedContent 번역
   * @param occurredAt 발생 시각
   */
  public AiConversationHistoryMessage(
      Long messageId,
      int turnNumber,
      String role,
      String content,
      String translatedContent,
      OffsetDateTime occurredAt) {
    this(messageId, turnNumber, role, content, translatedContent, occurredAt, null);
  }

  /**
   * 기존 AI 계약과 호환되는 시간 정보 없는 히스토리 메시지를 생성한다.
   *
   * @param messageId 메시지 ID
   * @param turnNumber 대화 턴 번호
   * @param role 메시지 발화자 역할
   * @param content 메시지 본문
   * @param translatedContent 번역된 메시지 본문
   */
  public AiConversationHistoryMessage(
      Long messageId, int turnNumber, String role, String content, String translatedContent) {
    this(messageId, turnNumber, role, content, translatedContent, null);
  }
}
