// 프리톡 사용자 발화의 비동기 속마음 생성 요청을 담는다.

package com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
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
 * @param watchPatterns 직전 스몰톡에서 교정받아 이번 턴에서 지켜볼 실수 패턴. 턴 교정에만 쓴다. 최대 3개이며 서로 달라야 한다. 비어 있으면 요청
 *     JSON에 싣지 않는다(이유는 memoryContext와 같다). 그래서 이 필드를 보내는 BE는 AI 서버가 먼저 배포된 뒤에 나가야 한다. 직전 스몰톡에서 교정받은
 *     사용자가 하나라도 있으면 구버전 AI는 그 요청을 거부해 속마음·교정이 모두 실패한다
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
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FreeTalkMistakePattern> watchPatterns) {

  /** AI 서버가 받는 지켜볼 실수 패턴 상한이다. */
  public static final int MAX_WATCH_PATTERNS = 3;

  /**
   * 기억 문맥과 지켜볼 패턴은 null 없이 상한 안에서만 전달한다.
   *
   * @throws IllegalArgumentException 기억 문맥이나 지켜볼 패턴이 AI 서버 계약의 상한을 넘거나 패턴이 겹칠 때
   */
  public AiFreeTalkInnerThoughtRequest {
    memoryContext = memoryContext == null ? List.of() : List.copyOf(memoryContext);
    if (memoryContext.size() > AiFreeTalkMemoryContext.MAX_CONTEXTS) {
      throw new IllegalArgumentException(
          "memoryContext must not exceed " + AiFreeTalkMemoryContext.MAX_CONTEXTS);
    }
    watchPatterns = watchPatterns == null ? List.of() : List.copyOf(watchPatterns);
    if (watchPatterns.size() > MAX_WATCH_PATTERNS) {
      throw new IllegalArgumentException("watchPatterns must not exceed " + MAX_WATCH_PATTERNS);
    }
    if (watchPatterns.stream().distinct().count() != watchPatterns.size()) {
      throw new IllegalArgumentException("watchPatterns must be unique");
    }
  }

  /** 지켜볼 실수 패턴 없이 요청을 만든다. */
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
        List.of());
  }

  /**
   * 제출한 발화의 원문을 대화 문맥에서 찾는다. 응답의 구절·사용례가 원문에 있는지 확인할 때 쓴다.
   *
   * @return 제출한 발화의 원문. 문맥에 없으면 null
   */
  public String submittedContent() {
    if (conversationHistory == null || submittedMessageId == null) {
      return null;
    }
    return conversationHistory.stream()
        .filter(message -> submittedMessageId.equals(message.messageId()))
        .map(AiConversationHistoryMessage::content)
        .findFirst()
        .orElse(null);
  }
}
