// 예약 푸시 알림 문구의 분석 가능한 변형을 정의한다.

package com.landit.landitbe.feature.notification.domain;

/** 예약 푸시 알림 문구의 분석 가능한 변형을 정의한다. */
public enum NotificationContentVariant {
  SCENARIO_A1("scenario_daily_fruit"),
  SCENARIO_A2("scenario_bird_tip"),
  SCENARIO_A3("scenario_five_minutes"),
  SCENARIO_A4("scenario_streak_record"),
  SCENARIO_R0("scenario_bird_tip"),
  SCENARIO_R1("scenario_restart_today"),
  SCENARIO_R2("scenario_waiting_days"),
  SCENARIO_R3("scenario_keep_habit"),
  SCENARIO_R4("scenario_feedback"),
  SCENARIO_R5("scenario_missing_you"),
  SCENARIO_R6("scenario_please_study"),
  EXPRESSION_DYNAMIC("expression_usage_question"),
  EXPRESSION_GENERIC("expression_continue"),
  SMALL_TALK_DYNAMIC("small_talk_continue_topic"),
  SMALL_TALK_GENERIC("small_talk_teddy_waiting");

  // 문구를 다듬거나 enum 이름을 바꿔도 이미 발행한 분석 슬러그는 유지한다.
  private final String contentSlug;

  NotificationContentVariant(String contentSlug) {
    this.contentSlug = contentSlug;
  }

  /**
   * 개인화 값과 무관한 문구 템플릿의 고정 분석 슬러그를 반환한다.
   *
   * @return utm_content에 사용하는 소문자 스네이크 케이스 슬러그
   */
  public String contentSlug() {
    return contentSlug;
  }
}
