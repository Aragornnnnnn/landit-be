// Keyset 페이지에서 계산한 사용자별 알림 대상 입력과 발송 가능 여부를 전달한다.

package com.landit.landitbe.feature.notification.service;

import java.util.List;
import java.util.Map;

record NotificationTargetPage(
    List<Long> userProfileIds,
    Map<Long, NotificationTargetSelectionInput> inputs,
    List<Long> sendableUserProfileIds) {}
