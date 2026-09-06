// exact 코사인 검색으로 반환할 장기기억 후보를 표현한다.

package com.landit.landitbe.feature.memory.repository;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.time.LocalDateTime;

/**
 * Exact 코사인 검색으로 반환할 장기기억 후보를 표현한다.
 *
 * @param memoryId 장기기억 ID
 * @param memoryType 장기기억 의미 유형
 * @param content 장기기억 본문
 * @param validFrom 기억 유효 시작 시각
 * @param validTo 기억 유효 종료 시각
 * @param observedAt 기억 관찰 시각
 * @param distance 쿼리와의 코사인 거리
 */
public record ConversationMemoryMatch(
    long memoryId,
    ConversationMemoryType memoryType,
    String content,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    LocalDateTime observedAt,
    double distance) {}
