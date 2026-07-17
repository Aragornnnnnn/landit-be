// 학습 세션의 생명주기 상태를 정의한다.

package com.landit.landitbe.session.domain;

/** 학습 세션의 생명주기 상태를 정의한다. */
public enum LearningSessionStatus {
  IN_PROGRESS,
  COMPLETED,
  INTERRUPTED
}
