// 프리톡 세션의 대화 진행 상태를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 세션의 대화 진행 상태를 정의한다. */
public enum FreeTalkConversationStatus {
  IN_PROGRESS,
  AWAITING_EXIT_DECISION,
  COMPLETED
}
