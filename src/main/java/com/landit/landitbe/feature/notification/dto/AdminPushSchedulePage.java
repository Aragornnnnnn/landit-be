// 관리자 예약 캠페인 목록과 페이지 번호 표시 정보를 전달한다.

package com.landit.landitbe.feature.notification.dto;

import java.util.List;

/**
 * 예약 시각이 있는 캠페인의 조회 페이지다.
 *
 * @param items 예약 캠페인 목록
 * @param page 0부터 시작하는 페이지 번호
 * @param size 페이지 크기
 * @param hasNext 다음 페이지 존재 여부
 * @param totalCount 상태 필터가 적용된 전체 예약 수
 * @param totalPages 전체 페이지 수. 빈 결과는 0
 */
public record AdminPushSchedulePage(
    List<AdminPushCampaignView> items,
    int page,
    int size,
    boolean hasNext,
    long totalCount,
    long totalPages) {}
