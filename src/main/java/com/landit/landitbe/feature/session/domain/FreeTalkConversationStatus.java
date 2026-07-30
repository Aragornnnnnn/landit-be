// 프리톡 세션의 대화 진행 상태를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 세션의 대화 진행 상태를 정의한다. */
public enum FreeTalkConversationStatus {
  /** 프리톡 대화가 진행 중이다. */
  IN_PROGRESS,

  /** 사용자에게 종료 의사를 확인하고 있다. */
  AWAITING_EXIT_DECISION,

  /** 프리톡 대화가 정상 완료됐다. */
  COMPLETED
}
