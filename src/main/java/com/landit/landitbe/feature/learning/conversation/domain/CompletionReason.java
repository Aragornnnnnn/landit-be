// 학습 세션 완료 또는 종료 사유를 정의한다.

package com.landit.landitbe.feature.learning.conversation.domain;

/** 학습 세션 완료 또는 종료 사유를 정의한다. */
public enum CompletionReason {
  GOAL_COMPLETED,
  MAX_TURNS_REACHED,
  USER_ENDED,
  /** 작별 발화 없이 사용자가 완료 버튼으로 종료했다. */
  DIRECT_COMPLETION,
  TIME_LIMIT_REACHED
}
