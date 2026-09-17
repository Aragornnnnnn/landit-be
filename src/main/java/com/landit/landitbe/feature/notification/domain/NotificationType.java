// 백엔드가 발송하는 푸시 알림 유형을 정의한다.

package com.landit.landitbe.feature.notification.domain;

/** 백엔드가 발송하는 푸시 알림 유형을 정의한다. */
public enum NotificationType {
  ADMIN_BROADCAST,
  ADMIN_BROADCAST_TEST,
  DAILY_SCENARIO_REMINDER,
  CONTINUE_EXPRESSION,
  SMALL_TALK_REMINDER,
  MAILBOX_REPLY,
  /** 연간 구독 무료 체험 종료 예정 알림이다. */
  TRIAL_ENDING,
  /** 개발 환경에서만 사용하는 수동 발송 검증 유형이다. */
  TEST_NOTIFICATION
}
