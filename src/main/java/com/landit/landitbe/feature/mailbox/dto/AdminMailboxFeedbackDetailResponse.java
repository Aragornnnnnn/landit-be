// 편지함 어드민 피드백 상세 응답을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.landit.landitbe.feature.mailbox.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 편지함 어드민 피드백 상세 응답이다.
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
 * @param reply 최신 답장. 없으면 {@code null}
 */
@Schema(description = "편지함 어드민 피드백 상세 응답")
public record AdminMailboxFeedbackDetailResponse(
    Long feedbackId,
    Long userProfileId,
    String email,
    String nickname,
    UserFeedbackType type,
    String content,
    UserFeedbackStatus status,
    Long resolvedByFeedbackId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    @Schema(
            description = "최신 답장. 없으면 null",
            nullable = true,
            types = {"object", "null"})
        Reply reply) {

  /**
   * 피드백에 연결된 최신 답장이다.
   *
   * @param letterId 답장 편지 ID
   * @param title 답장 제목
   * @param bodyText 답장 본문
   * @param sentAt 답장 발송 시각
   */
  public record Reply(
      @Schema(description = "답장 편지 ID", example = "201") Long letterId,
      @Schema(description = "답장 제목") String title,
      @Schema(description = "답장 본문") String bodyText,
      @Schema(description = "답장 발송 시각") LocalDateTime sentAt) {}
}
