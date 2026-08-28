// 프리톡 AI에 전달할 범위 검증된 장기기억 문맥을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;

/**
 * 프리톡 시작 시 AI에 전달할 장기기억 문맥을 표현한다.
 *
 * @param memoryId 장기기억 식별자
 * @param memoryType 장기기억 의미 유형
 * @param content 장기기억 본문
 */
public record AiFreeTalkMemoryContext(
    Long memoryId, ConversationMemoryType memoryType, String content) {}
