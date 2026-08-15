// 편지함 어드민 일괄 답장 결과를 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 편지함 어드민 일괄 답장 결과다.
 *
 * @param letterId 생성된 답장 편지 ID
 * @param recipientCount 답장을 받은 사용자 수
 * @param completedFeedbackCount 이번에 처리 완료로 변경한 피드백 수
 * @param representativeFeedbackIds 사용자별 대표 피드백 ID 목록
 */
@Schema(description = "편지함 어드민 일괄 답장 결과")
public record AdminMailboxReplyResponse(
    Long letterId,
    int recipientCount,
    int completedFeedbackCount,
    List<Long> representativeFeedbackIds) {}
