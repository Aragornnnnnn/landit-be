// 날짜별 시나리오의 신규·재도전·완료 상태를 정의한다.

package com.landit.landitbe.feature.content.domain;

/** 날짜별 시나리오의 신규·재도전·완료 상태를 정의한다. */
public enum DailyScenarioType {
  /** 오늘 처음 제공하는 미완료 시나리오다. */
  NEW,

  /** 이전 날짜에 시작했지만 완료하지 못해 다시 제공하는 시나리오다. */
  RETRY,

  /** 조회 날짜에 최초 완료해 복습할 수 있는 시나리오다. */
  CLEARED
}
