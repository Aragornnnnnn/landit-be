// 프리톡 세션의 첫 발화 주체를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 세션의 첫 발화 주체를 정의한다. */
public enum FreeTalkStartMode {
  /** 사용자가 추천 주제를 선택하고 AI가 먼저 말한다. */
  AI_FIRST,

  /** 사용자가 먼저 말하고 AI가 첫 발화에서 주제를 추론한다. */
  USER_FIRST
}
