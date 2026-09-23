// 프리톡 마지막 AI 메시지 생성 요청을 담는다.

package com.landit.landitbe.feature.learning.freetalk.message.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkSessionSummary;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import java.util.List;

/**
 * 프리톡 마지막 AI 메시지 생성 요청을 담는다.
 *
 * @param sessionId 프리톡 세션 ID
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param submittedMessageId 마지막 사용자 메시지 ID
 * @param submittedTurnNumber 마지막 사용자 메시지 턴 번호
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param closingReason 마무리 메시지 생성 사유
 * @param titleGenerationRequired 사용자 선시작 세션의 제목 생성 필요 여부
 * @param topic 저장된 프리톡 주제
 * @param conversationHistory 누적 대화 메시지
 * @param contextPolicyVersion 컨텍스트 정책 버전. 비활성화이면 null
 * @param sessionSummary 요청에 사용할 세션 요약. 없으면 null
 * @param historyIncomplete 전달 이력에 요약으로 보완하지 못한 누락 구간이 있는지 여부
 */
public record AiFreeTalkClosingRequest(
    Long sessionId,
    String characterId,
    Long submittedMessageId,
    int submittedTurnNumber,
    String targetLocale,
    String baseLocale,
    AiFreeTalkClosingReason closingReason,
    boolean titleGenerationRequired,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> conversationHistory,
    @JsonInclude(JsonInclude.Include.NON_NULL) String contextPolicyVersion,
    @JsonInclude(JsonInclude.Include.NON_NULL) AiFreeTalkSessionSummary sessionSummary,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean historyIncomplete) {

  /**
   * 기존 전체 이력 호출과 호환되는 요청을 생성한다.
   *
   * @param sessionId 프리톡 세션 ID
   * @param characterId 선택한 프리톡 캐릭터 식별자
   * @param submittedMessageId 마지막 사용자 메시지 ID
   * @param submittedTurnNumber 마지막 사용자 메시지 턴 번호
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param closingReason 마무리 메시지 생성 사유
   * @param titleGenerationRequired 사용자 선시작 세션의 제목 생성 필요 여부
   * @param topic 저장된 프리톡 주제
   * @param conversationHistory 누적 대화 메시지
   */
  public AiFreeTalkClosingRequest(
      Long sessionId,
      String characterId,
      Long submittedMessageId,
      int submittedTurnNumber,
      String targetLocale,
      String baseLocale,
      AiFreeTalkClosingReason closingReason,
      boolean titleGenerationRequired,
      AiFreeTalkTopic topic,
      List<AiConversationHistoryMessage> conversationHistory) {
    this(
        sessionId,
        characterId,
        submittedMessageId,
        submittedTurnNumber,
        targetLocale,
        baseLocale,
        closingReason,
        titleGenerationRequired,
        topic,
        conversationHistory,
        null,
        null,
        false);
  }
}
