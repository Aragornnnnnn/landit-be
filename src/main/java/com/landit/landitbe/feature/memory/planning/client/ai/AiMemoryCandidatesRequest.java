// 장기기억 후보 추출 AI 요청을 담는다.

package com.landit.landitbe.feature.memory.planning.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.client.ai.ConversationMemoryHistoryMessage;
import java.util.List;

/**
 * 장기기억 후보 추출에 필요한 프리톡 세션 문맥을 담는다.
 *
 * <p>뒤의 세 값은 다음 스몰톡 후속 질문 생성에만 쓰이고 후보 추출에는 쓰이지 않는다. 비어 있으면 요청 JSON에 싣지 않는다. 이 필드들을 모르는 구버전 AI
 * 서버(`extra="forbid"`)가 보낼 것이 없는 요청까지 거부하지 않게 하기 위함이다.
 *
 * @param sessionId 프리톡 학습 세션 ID
 * @param characterId 대화 캐릭터 ID
 * @param targetLocale 학습 대상 언어
 * @param baseLocale 사용자 기준 언어
 * @param timezone 날짜 해석에 쓸 IANA 시간대
 * @param conversationHistory 후보를 추출할 대화 이력
 * @param existingMemories 후속 질문의 근거로 쓸 수 있는 사용자의 기존 장기기억. 최대 20개
 * @param askedMemoryIds 이미 후속 질문의 근거로 쓴 장기기억 ID. AI 서버가 질문 생성에서 뺀다
 * @param sessionEndedBy 세션이 어떻게 끝났는지(USER_CONFIRMED, TIME_LIMIT_REACHED). 모르면 null
 */
public record AiMemoryCandidatesRequest(
    Long sessionId,
    String characterId,
    String targetLocale,
    String baseLocale,
    String timezone,
    List<ConversationMemoryHistoryMessage> conversationHistory,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<AiFreeTalkMemoryContext> existingMemories,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Long> askedMemoryIds,
    @JsonInclude(JsonInclude.Include.NON_NULL) String sessionEndedBy) {

  /** AI 서버가 받는 기존 장기기억 상한이다. */
  public static final int MAX_EXISTING_MEMORIES = 20;

  /**
   * 목록을 null 없이 불변으로 보관한다.
   *
   * @throws IllegalArgumentException 기존 장기기억이 AI 서버 계약의 상한을 넘을 때
   */
  public AiMemoryCandidatesRequest {
    existingMemories = existingMemories == null ? List.of() : List.copyOf(existingMemories);
    askedMemoryIds = askedMemoryIds == null ? List.of() : List.copyOf(askedMemoryIds);
    if (existingMemories.size() > MAX_EXISTING_MEMORIES) {
      throw new IllegalArgumentException(
          "existingMemories must not exceed " + MAX_EXISTING_MEMORIES);
    }
  }

  /** 후속 질문 문맥 없이 후보 추출만 요청한다. */
  public AiMemoryCandidatesRequest(
      Long sessionId,
      String characterId,
      String targetLocale,
      String baseLocale,
      String timezone,
      List<ConversationMemoryHistoryMessage> conversationHistory) {
    this(
        sessionId,
        characterId,
        targetLocale,
        baseLocale,
        timezone,
        conversationHistory,
        List.of(),
        List.of(),
        null);
  }
}
