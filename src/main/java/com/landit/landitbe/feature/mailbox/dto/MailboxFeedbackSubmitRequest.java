// 편지함 피드백 등록 요청을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.landit.landitbe.feature.mailbox.domain.MailboxFeedback;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 편지함 피드백 등록 요청이다.
 *
 * @param type 피드백 유형
 * @param content 피드백 본문
 */
@Schema(description = "편지함 피드백 등록 요청")
public record MailboxFeedbackSubmitRequest(
    @NotNull @Schema(description = "피드백 유형", example = "QUESTION") UserFeedbackType type,
    @NotBlank @Schema(description = "피드백 내용", example = "로그인 관련 문의입니다.") String content) {

  /**
   * 인증된 사용자 ID를 연결한 피드백 Entity로 변환한다.
   *
   * @param userProfileId 피드백을 제출한 사용자 ID
   * @return 저장할 피드백 Entity
   */
  public MailboxFeedback toEntity(Long userProfileId) {
    return new MailboxFeedback(userProfileId, type, content);
  }
}
