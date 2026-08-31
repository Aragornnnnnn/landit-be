// 예약 학습 알림의 유형별 제목, 본문과 딥링크를 정의한다.

package com.landit.landitbe.feature.notification.service;

/** 예약 학습 알림의 유형별 제목, 본문과 딥링크를 정의한다. */
record ScheduledNotificationContent(String title, String body, String deepLink) {

  static ScheduledNotificationContent from(SelectedNotificationTarget target) {
    return switch (target.notificationType()) {
      case DAILY_SCENARIO_REMINDER ->
          new ScheduledNotificationContent(
              "오늘의 시나리오를 시작해 볼까요?",
              "오늘 배정된 대화로 영어 학습을 이어가 보세요.",
              "/conversation/scenario/" + target.targetId() + campaign("daily_scenario_reminder"));
      case CONTINUE_EXPRESSION ->
          new ScheduledNotificationContent(
              "표현 학습을 이어가 볼까요?",
              "배운 표현을 다음 단계로 이어가 보세요.",
              "/expressions/scenario/"
                  + target.scenarioId()
                  + "/"
                  + target.targetId()
                  + campaign("continue_expression"));
      case SMALL_TALK_REMINDER ->
          new ScheduledNotificationContent(
              "오늘 1분 스몰톡 해볼까요?",
              "가볍게 대화하며 영어 감각을 이어가 보세요.",
              "/smalltalk" + campaign("small_talk_reminder"));
      default -> throw new IllegalArgumentException("예약 알림에 지원하지 않는 유형입니다.");
    };
  }

  private static String campaign(String campaign) {
    return "?utm_source=push&utm_medium=notification&utm_campaign=" + campaign;
  }
}
