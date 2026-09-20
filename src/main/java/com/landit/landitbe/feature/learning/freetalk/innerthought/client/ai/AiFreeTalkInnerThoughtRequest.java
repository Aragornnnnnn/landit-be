// 프리톡 사용자 발화의 비동기 속마음 생성 요청을 담는다.

package com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai;

import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import java.util.List;

/**
 * 프리톡 사용자 발화의 비동기 속마음 생성 요청을 담는다.
 *
 * @param sessionId 프리톡 학습 세션 ID
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param submittedMessageId 속마음 대상 사용자 메시지 ID
 * @param submittedTurnNumber 속마음 대상 사용자 메시지 턴 번호
 * @param targetLocale 학습 대상 언어
 * @param baseLocale 사용자 기준 언어
 * @param topic 현재 프리톡 주제
 * @param conversationHistory 속마음 생성에 사용할 대화 문맥
 * @param memoryContext 턴 교정의 근거로만 쓰는 장기기억 문맥. 최대 3개이며 없으면 빈 목록
 */
public record AiFreeTalkInnerThoughtRequest(
    Long sessionId,
    String characterId,
    Long submittedMessageId,
    int submittedTurnNumber,
    String targetLocale,
    String baseLocale,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> conversationHistory,
    List<AiFreeTalkMemoryContext> memoryContext) {

  /**
   * 기억 문맥은 null 없이 상한 안에서만 전달한다.
   *
   * @throws IllegalArgumentException 기억 문맥이 AI 서버 계약의 상한을 넘을 때
   */
  public AiFreeTalkInnerThoughtRequest {
    memoryContext = memoryContext == null ? List.of() : List.copyOf(memoryContext);
    if (memoryContext.size() > AiFreeTalkMemoryContext.MAX_CONTEXTS) {
      throw new IllegalArgumentException(
          "memoryContext must not exceed " + AiFreeTalkMemoryContext.MAX_CONTEXTS);
    }
  }
}
