// 프리톡 사용자 발화의 비동기 속마음 생성 요청을 담는다.

package com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkSessionSummary;
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
 * @param memoryContext 턴 교정의 근거로만 쓰는 장기기억 문맥. 최대 3개. 비어 있으면 요청 JSON에 싣지 않는다. 이 필드를 모르는 구버전 AI
 *     서버(`extra="forbid"`)가 요청 전체를 거부해 속마음까지 실패시키는 일을, 보낼 기억이 없는 요청에서는 피하기 위함이다
 * @param contextPolicyVersion 컨텍스트 정책 버전. 비활성화이면 null
 * @param sessionSummary 요청에 사용할 세션 요약. 없으면 null
 * @param historyIncomplete 전달 이력에 요약으로 보완하지 못한 누락 구간이 있는지 여부
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
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<AiFreeTalkMemoryContext> memoryContext,
    @JsonInclude(JsonInclude.Include.NON_NULL) String contextPolicyVersion,
    @JsonInclude(JsonInclude.Include.NON_NULL) AiFreeTalkSessionSummary sessionSummary,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean historyIncomplete) {

  /**
   * 기존 전체 이력 호출과 호환되는 요청을 생성한다.
   *
   * @param sessionId 프리톡 학습 세션 ID
   * @param characterId 선택한 프리톡 캐릭터 식별자
   * @param submittedMessageId 속마음 대상 사용자 메시지 ID
   * @param submittedTurnNumber 속마음 대상 사용자 메시지 턴 번호
   * @param targetLocale 학습 대상 언어
   * @param baseLocale 사용자 기준 언어
   * @param topic 현재 프리톡 주제
   * @param conversationHistory 속마음 생성에 사용할 대화 문맥
   * @param memoryContext 턴 교정의 근거로 사용할 장기기억 문맥
   * @throws IllegalArgumentException 기억 문맥이 AI 서버 계약의 상한을 넘을 때
   */
  public AiFreeTalkInnerThoughtRequest(
      Long sessionId,
      String characterId,
      Long submittedMessageId,
      int submittedTurnNumber,
      String targetLocale,
      String baseLocale,
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
        topic,
        conversationHistory,
        memoryContext,
        null,
        null,
        false);
  }

  /**
   * 기억 문맥은 null 없이 상한 안에서만 전달한다.
   *
   * @param sessionId 프리톡 학습 세션 ID
   * @param characterId 선택한 프리톡 캐릭터 식별자
   * @param submittedMessageId 속마음 대상 사용자 메시지 ID
   * @param submittedTurnNumber 속마음 대상 사용자 메시지 턴 번호
   * @param targetLocale 학습 대상 언어
   * @param baseLocale 사용자 기준 언어
   * @param topic 현재 프리톡 주제
   * @param conversationHistory 속마음 생성에 사용할 대화 문맥
   * @param memoryContext 턴 교정의 근거로 사용할 장기기억 문맥
   * @param contextPolicyVersion 컨텍스트 정책 버전. 비활성화이면 null
   * @param sessionSummary 요청에 사용할 세션 요약. 없으면 null
   * @param historyIncomplete 전달 이력에 요약으로 보완하지 못한 누락 구간이 있는지 여부
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
