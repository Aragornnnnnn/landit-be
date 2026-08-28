// 프리톡 AI에 전달할 범위 검증된 장기기억 문맥을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.time.LocalDateTime;

/**
 * 프리톡 시작 시 AI에 전달할 장기기억 문맥을 표현한다.
 *
 * @param memoryId 장기기억 식별자
 * @param memoryType 장기기억 의미 유형
 * @param content 장기기억 본문
 * @param validFrom 기억이 유효하기 시작한 시각
 * @param validTo 기억이 유효하지 않게 된 시각
 * @param observedAt 기억을 관찰한 시각
 */
public record AiFreeTalkMemoryContext(
    Long memoryId,
    ConversationMemoryType memoryType,
    String content,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    LocalDateTime observedAt) {

  /**
   * 시간 정보가 없는 기존 호출 경로에서 기억 문맥을 생성한다.
   *
   * @param memoryId 장기기억 식별자
   * @param memoryType 장기기억 의미 유형
   * @param content 장기기억 본문
   */
  public AiFreeTalkMemoryContext(Long memoryId, ConversationMemoryType memoryType, String content) {
    this(memoryId, memoryType, content, null, null, null);
  }
}
