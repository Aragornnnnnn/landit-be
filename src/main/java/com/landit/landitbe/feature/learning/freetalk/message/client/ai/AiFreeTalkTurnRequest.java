// 프리톡 사용자 발화의 AI 응답 생성 요청을 담는다.

package com.landit.landitbe.feature.learning.freetalk.message.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkResponseMode;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkSessionSummary;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import java.util.List;

/**
 * 프리톡 사용자 발화의 AI 응답 생성 요청을 담는다.
 *
 * @param sessionId 프리톡 세션 ID
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param submittedMessageId 처리할 사용자 메시지 ID
 * @param submittedTurnNumber 처리할 사용자 메시지 턴 번호
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param responseMode 응답 생성 방식
 * @param isFirstUserTurn 사용자 선시작의 첫 발화 여부
 * @param topic 선택했거나 추론된 주제
 * @param conversationHistory 누적 대화 메시지
 * @param memoryContext 세션 첫 사용자 발화에 사용할 범위 검증된 장기기억 문맥
 * @param contextPolicyVersion 컨텍스트 정책 버전. 비활성화이면 null
 * @param sessionSummary 요청에 사용할 세션 요약. 없으면 null
 * @param historyIncomplete 전달 이력에 요약으로 보완하지 못한 누락 구간이 있는지 여부
 */
public record AiFreeTalkTurnRequest(
    Long sessionId,
    String characterId,
    Long submittedMessageId,
    int submittedTurnNumber,
    String targetLocale,
    String baseLocale,
    AiFreeTalkResponseMode responseMode,
    boolean isFirstUserTurn,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> conversationHistory,
    List<AiFreeTalkMemoryContext> memoryContext,
    @JsonInclude(JsonInclude.Include.NON_NULL) String contextPolicyVersion,
    @JsonInclude(JsonInclude.Include.NON_NULL) AiFreeTalkSessionSummary sessionSummary,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean historyIncomplete) {

  /**
   * 기존 전체 이력 호출과 호환되는 요청을 생성한다.
   *
   * @param sessionId 프리톡 세션 ID
   * @param characterId 선택한 프리톡 캐릭터 식별자
   * @param submittedMessageId 처리할 사용자 메시지 ID
   * @param submittedTurnNumber 처리할 사용자 메시지 턴 번호
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param responseMode 응답 생성 방식
   * @param isFirstUserTurn 사용자 선시작의 첫 발화 여부
   * @param topic 선택했거나 추론된 주제
   * @param conversationHistory 누적 대화 메시지
   * @param memoryContext 세션 첫 사용자 발화에 사용할 범위 검증된 장기기억 문맥
   */
  public AiFreeTalkTurnRequest(
      Long sessionId,
      String characterId,
      Long submittedMessageId,
      int submittedTurnNumber,
      String targetLocale,
      String baseLocale,
      AiFreeTalkResponseMode responseMode,
      boolean isFirstUserTurn,
      AiFreeTalkTopic topic,
      List<AiConversationHistoryMessage> conversationHistory,
      List<AiFreeTalkMemoryContext> memoryContext) {
    this(
        sessionId,
        characterId,
        submittedMessageId,
        submittedTurnNumber,
        targetLocale,
        baseLocale,
        responseMode,
        isFirstUserTurn,
        topic,
        conversationHistory,
        memoryContext,
        null,
        null,
        false);
  }

  /**
   * 요청 문맥을 방어적으로 복사한다.
   *
   * @param sessionId 프리톡 세션 ID
   * @param characterId 선택한 프리톡 캐릭터 식별자
   * @param submittedMessageId 처리할 사용자 메시지 ID
   * @param submittedTurnNumber 처리할 사용자 메시지 턴 번호
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param responseMode 응답 생성 방식
   * @param isFirstUserTurn 사용자 선시작의 첫 발화 여부
   * @param topic 선택했거나 추론된 주제
   * @param conversationHistory 누적 대화 메시지
   * @param memoryContext 세션 첫 사용자 발화에 사용할 범위 검증된 장기기억 문맥
   * @param contextPolicyVersion 컨텍스트 정책 버전. 비활성화이면 null
   * @param sessionSummary 요청에 사용할 세션 요약. 없으면 null
   * @param historyIncomplete 전달 이력에 요약으로 보완하지 못한 누락 구간이 있는지 여부
   */
  public AiFreeTalkTurnRequest {
    memoryContext = memoryContext == null ? List.of() : List.copyOf(memoryContext);
  }
}
