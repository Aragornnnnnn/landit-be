// 구독 권한 판정에 필요한 소유 세션 상태를 전달한다.

package com.landit.landitbe.feature.learning.conversation.dto;

import com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus;
import com.landit.landitbe.feature.learning.conversation.domain.SessionType;
import java.time.LocalDateTime;

/**
 * 구독 권한 판정에 필요한 소유 세션 상태를 전달한다.
 *
 * @param sessionType 세션 종류
 * @param status 세션 상태
 * @param startedAt 원래 학습 시작 시각
 */
public record LearningSessionAccess(
    SessionType sessionType, LearningSessionStatus status, LocalDateTime startedAt) {}
