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

  /**
   * 오프닝·턴·속마음 요청 하나에 실을 수 있는 장기기억 문맥 수. AI 서버 계약의 상한과 같다.
   *
   * <p>기억 후보 요청에 싣는 기존 기억의 상한은 따로 있다.
   */
  public static final int MAX_CONTEXTS = 3;

  /** 유효기간 없이 제공하는 기존 장기기억 문맥을 만든다. */
  public AiFreeTalkMemoryContext(Long memoryId, ConversationMemoryType memoryType, String content) {
    this(memoryId, memoryType, content, null, null, null);
  }
}
