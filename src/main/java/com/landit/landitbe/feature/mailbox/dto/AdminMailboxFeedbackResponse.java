// 편지함 어드민 피드백 응답을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.landit.landitbe.feature.mailbox.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 편지함 어드민 피드백 응답이다.
 *
 * @param feedbackId 피드백 ID
 * @param userProfileId 사용자 ID
 * @param email 사용자 이메일
 * @param nickname 사용자 닉네임
 * @param type 피드백 유형
 * @param content 피드백 본문
 * @param status 처리 상태
 * @param resolvedByFeedbackId 대표 피드백 ID
 * @param createdAt 생성 시각
 * @param updatedAt 수정 시각
 */
@Schema(description = "편지함 어드민 피드백 응답")
public record AdminMailboxFeedbackResponse(
    Long feedbackId,
    Long userProfileId,
    String email,
    String nickname,
    UserFeedbackType type,
    String content,
    UserFeedbackStatus status,
    Long resolvedByFeedbackId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
