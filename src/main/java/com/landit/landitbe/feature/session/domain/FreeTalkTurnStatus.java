// 프리톡 사용자 발화 처리 결과 상태를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 사용자 발화 처리 결과 상태를 정의한다. */
public enum FreeTalkTurnStatus {
  CONTINUE,
  EXIT_CONFIRMATION_REQUIRED,
  COMPLETED
}
