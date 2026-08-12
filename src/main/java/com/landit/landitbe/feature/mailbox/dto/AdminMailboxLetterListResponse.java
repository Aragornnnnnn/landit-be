// 편지함 어드민 공지·업데이트 페이지 응답을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 편지함 어드민 공지·업데이트 페이지 응답이다.
 *
 * @param items 편지 목록
 * @param page 현재 페이지
 * @param size 페이지 크기
 * @param totalElements 전체 편지 수
 * @param totalPages 전체 페이지 수
 */
@Schema(description = "편지함 어드민 공지·업데이트 페이지 응답")
public record AdminMailboxLetterListResponse(
    List<AdminMailboxLetterResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
