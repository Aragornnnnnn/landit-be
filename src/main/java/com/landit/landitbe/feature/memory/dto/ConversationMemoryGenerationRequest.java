// 장기기억 생성을 위해 세션이 전달하는 독립 문맥을 담는다.

package com.landit.landitbe.feature.memory.dto;

import com.landit.landitbe.feature.memory.client.ai.ConversationMemoryHistoryMessage;
import java.util.List;

/** 장기기억 생성에 필요한 세션·사용자 문맥을 기능 간 값으로 전달한다. */
public record ConversationMemoryGenerationRequest(
    long learningSessionId,
    long userProfileId,
    String characterId,
    String targetLocale,
    String baseLocale,
    String timezone,
    List<ConversationMemoryHistoryMessage> history,
    ConversationMemoryFollowUpContext followUpContext) {

  /** 후속 질문 문맥이 없는 장기기억 생성 문맥을 만든다. */
  public ConversationMemoryGenerationRequest(
      long learningSessionId,
      long userProfileId,
      String characterId,
      String targetLocale,
      String baseLocale,
      String timezone,
      List<ConversationMemoryHistoryMessage> history) {
    this(
        learningSessionId,
        userProfileId,
        characterId,
        targetLocale,
        baseLocale,
        timezone,
        history,
        ConversationMemoryFollowUpContext.none());
  }

  /** 장기기억 생성 문맥의 ID·필수 값과 이력 불변식을 검증한다. */
  public ConversationMemoryGenerationRequest {
    if (learningSessionId <= 0 || userProfileId <= 0) {
      throw new IllegalArgumentException("장기기억 생성 문맥 ID가 유효하지 않습니다.");
    }
    if (characterId == null || characterId.isBlank()) {
      throw new IllegalArgumentException("장기기억 생성 캐릭터가 필요합니다.");
    }
    if (targetLocale == null || baseLocale == null || timezone == null || history == null) {
      throw new IllegalArgumentException("장기기억 생성 문맥이 유효하지 않습니다.");
    }
    history = List.copyOf(history);
    followUpContext =
        followUpContext == null ? ConversationMemoryFollowUpContext.none() : followUpContext;
  }
}
