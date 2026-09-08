// 관리자 푸시 캠페인의 원문과 발송 집계를 전달한다.

package com.landit.landitbe.feature.notification.dto;

import com.landit.landitbe.feature.notification.domain.AdminPushAudienceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 관리자 푸시 캠페인의 원문과 토큰 기준 발송 집계다.
 *
 * @param id 캠페인 ID
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 알림 이동 경로
 * @param createdBy 생성 관리자 ID
 * @param status 캠페인 상태
 * @param targetUserCount 발송 시작 시점의 대상 사용자 수
 * @param targetTokenCount 발송 시작 시점의 대상 Token 수
 * @param pendingCount Ticket 또는 Receipt 처리 중인 수
 * @param succeededCount Receipt 성공 수
 * @param failedCount 명시적인 실패 수
 * @param excludedCount 발송 전에 비활성화되어 제외된 수
 * @param createdAt 생성 시각
 * @param completedAt 전체 대상 제출 완료 시각
 * @param audienceType 발송 대상 유형
 * @param userProfileIds 생성 시 고정한 선택 사용자 목록. ALL이면 빈 목록
 */
public record AdminPushCampaignView(
    UUID id,
    String title,
    String body,
    String deepLink,
    long createdBy,
    String status,
    long targetUserCount,
    long targetTokenCount,
    long pendingCount,
    long succeededCount,
    long failedCount,
    long excludedCount,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    AdminPushAudienceType audienceType,
    List<Long> userProfileIds) {}
