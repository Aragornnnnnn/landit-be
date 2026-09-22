// 프리톡 AI에 제공할 장기기억 문맥을 담는다.

package com.landit.landitbe.feature.memory.client.ai;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.time.LocalDateTime;

/** 프리톡 AI에 제공할 범위가 검증된 장기기억 문맥을 담는다. */
public record AiFreeTalkMemoryContext(
    Long memoryId,
    ConversationMemoryType memoryType,
    String content,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    LocalDateTime observedAt) {

  /** 한 번의 AI 요청에 실을 수 있는 장기기억 문맥 수. AI 서버 계약의 상한과 같다. */
  public static final int MAX_CONTEXTS = 3;

  /** 유효기간 없이 제공하는 기존 장기기억 문맥을 만든다. */
  public AiFreeTalkMemoryContext(Long memoryId, ConversationMemoryType memoryType, String content) {
    this(memoryId, memoryType, content, null, null, null);
  }
}
