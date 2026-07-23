// 사용자 메시지의 상대 역할 속마음 조회 응답을 정의한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import io.swagger.v3.oas.annotations.media.Schema;

/** 사용자 메시지의 상대 역할 속마음 조회 응답을 정의한다. */
@Schema(description = "사용자 메시지 속마음 조회 응답")
public record SessionInnerThoughtResponse(
    @Schema(
            description = "속마음 처리 상태",
            allowableValues = {"PREPARING", "COMPLETED", "FAILED"})
        String processingStatus,
    @Schema(description = "상대 역할의 속마음. COMPLETED에서만 제공") String innerThought,
    @Schema(description = "속마음 유형. COMPLETED에서만 제공") String innerThoughtType) {

  /** 처리에 실패한 속마음 응답을 생성한다. */
  public static SessionInnerThoughtResponse failed() {
    return new SessionInnerThoughtResponse(ProcessingStatus.FAILED.name(), null, null);
  }

  /** 사용자 메시지의 속마음 처리 상태를 API 응답으로 변환한다. */
  public static SessionInnerThoughtResponse from(SessionHistoryMessage message) {
    ProcessingStatus status = message.getInnerThoughtProcessingStatus();
    if (status != ProcessingStatus.COMPLETED) {
      return new SessionInnerThoughtResponse(status.name(), null, null);
    }
    return new SessionInnerThoughtResponse(
        status.name(), message.getInnerThought(), message.getInnerThoughtType().name());
  }
}
