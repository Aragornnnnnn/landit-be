// 백엔드가 발송하는 푸시 알림 유형을 정의한다.

package com.landit.landitbe.feature.notification.domain;

/** 백엔드가 발송하는 푸시 알림 유형을 정의한다. */
public enum NotificationType {
  CONTINUE_SCENARIO,
  CONTINUE_EXPRESSION,
  REVIEW_LEARNING,
  REVIEW_REMINDER,
  TEST_NOTIFICATION
}
