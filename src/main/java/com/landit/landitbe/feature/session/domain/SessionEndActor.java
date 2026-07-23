// 학습 세션 종료 주체를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 학습 세션 종료 주체를 정의한다. */
public enum SessionEndActor {
  USER,
  SYSTEM,
  TIME_LIMIT
}
