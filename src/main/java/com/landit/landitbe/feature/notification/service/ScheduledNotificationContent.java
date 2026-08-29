// 학습 이어 하기 알림의 유형별 제목, 본문과 딥링크를 정의한다.

package com.landit.landitbe.feature.notification.service;

/** 학습 이어 하기 알림의 유형별 제목, 본문과 딥링크를 정의한다. */
record ScheduledNotificationContent(String title, String body, String deepLink) {

  static ScheduledNotificationContent from(SelectedNotificationTarget target) {
    return switch (target.notificationType()) {
      case CONTINUE_SCENARIO ->
          new ScheduledNotificationContent(
              "다음 시나리오를 시작해 볼까요?",
              "새로운 대화를 이어서 학습해 보세요.",
              "/conversation/" + target.targetId() + campaign("continue_scenario"));
      case CONTINUE_EXPRESSION ->
          new ScheduledNotificationContent(
              "표현 학습을 이어가 볼까요?",
              "배운 표현을 다음 단계로 이어가 보세요.",
              "/expressions/" + target.targetId() + campaign("continue_expression"));
      case REVIEW_LEARNING ->
          new ScheduledNotificationContent(
              "다시 한번 연습해 볼까요?", "완료한 시나리오를 다시 살펴보세요.", "/home" + campaign("review_learning"));
      default -> throw new IllegalArgumentException("예약 알림에 지원하지 않는 유형입니다.");
    };
  }

  private static String campaign(String campaign) {
    return "?utm_source=push&utm_medium=notification&utm_campaign=" + campaign;
  }
}
