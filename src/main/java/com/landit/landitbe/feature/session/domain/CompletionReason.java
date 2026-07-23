// 학습 세션 완료 또는 종료 사유를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 학습 세션 완료 또는 종료 사유를 정의한다. */
public enum CompletionReason {
  GOAL_COMPLETED,
  MAX_TURNS_REACHED,
  USER_ENDED,
  TIME_LIMIT_REACHED
}
