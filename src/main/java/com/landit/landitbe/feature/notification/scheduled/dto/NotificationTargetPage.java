// Keyset 페이지에서 계산한 사용자별 알림 대상 입력과 발송 가능 여부를 전달한다.

package com.landit.landitbe.feature.notification.scheduled.dto;

import java.util.List;
import java.util.Map;

/**
 * 예약 알림 대상 조회의 한 페이지다.
 *
 * @param userProfileIds 조회한 사용자 ID
 * @param inputs 사용자별 학습 상태
 * @param sendableUserProfileIds 발송 가능한 사용자 ID
 */
public record NotificationTargetPage(
    List<Long> userProfileIds,
    Map<Long, NotificationTargetSelectionInput> inputs,
    List<Long> sendableUserProfileIds) {}
