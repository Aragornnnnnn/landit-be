// 관리자 푸시 캠페인의 불변 원문과 전체 및 테스트 실행을 전달한다.

package com.landit.landitbe.feature.notification.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 캠페인 원문과 실행 요약이다. 전체 실행이 없으면 DRAFT이며 원문은 수정할 수 없다.
 *
 * @param id 캠페인 ID
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 알림 이동 경로
 * @param createdBy 생성 관리자 ID
 * @param createdAt 생성 시각
 * @param status 전체 실행으로부터 계산한 캠페인 상태
 * @param broadcast 전체 발송 실행, 없으면 null
 * @param tests 관리자 테스트 실행 목록
 */
public record AdminPushCampaignView(
    UUID id,
    String title,
    String body,
    String deepLink,
    long createdBy,
    LocalDateTime createdAt,
    String status,
    AdminPushRunView broadcast,
    List<AdminPushRunView> tests) {}
