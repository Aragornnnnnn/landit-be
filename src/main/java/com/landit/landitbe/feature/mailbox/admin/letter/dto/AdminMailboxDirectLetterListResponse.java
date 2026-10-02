// 관리자 직접 편지 발송 이력 페이지를 정의한다.

package com.landit.landitbe.feature.mailbox.admin.letter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 발송 시각과 ID 내림차순으로 정렬한 직접 편지 페이지다.
 *
 * @param items 발송 건별 요약
 * @param page 0부터 시작하는 현재 페이지
 * @param size 페이지 크기
 * @param totalElements 전체 발송 건수
 * @param totalPages 전체 페이지 수
 */
@Schema(description = "관리자 직접 편지 발송 이력 페이지")
public record AdminMailboxDirectLetterListResponse(
    List<AdminMailboxDirectLetterSummary> items,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
