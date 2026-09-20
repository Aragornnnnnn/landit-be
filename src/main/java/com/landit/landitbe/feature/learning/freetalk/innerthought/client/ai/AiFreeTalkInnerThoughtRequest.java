// 프리톡 사용자 발화의 비동기 속마음 생성 요청을 담는다.

package com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkSessionSummary;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
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
    @JsonInclude(JsonInclude.Include.NON_NULL) String contextPolicyVersion,
    @JsonInclude(JsonInclude.Include.NON_NULL) AiFreeTalkSessionSummary sessionSummary,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean historyIncomplete) {

  /** 기존 전체 이력 호출과 호환되는 요청을 생성한다. */
  public AiFreeTalkInnerThoughtRequest(
      Long sessionId,
      String characterId,
      Long submittedMessageId,
      int submittedTurnNumber,
      String targetLocale,
      String baseLocale,
      AiFreeTalkTopic topic,
      List<AiConversationHistoryMessage> conversationHistory) {
    this(
        sessionId,
        characterId,
        submittedMessageId,
        submittedTurnNumber,
        targetLocale,
        baseLocale,
        topic,
        conversationHistory,
        null,
        null,
        false);
  }
}
