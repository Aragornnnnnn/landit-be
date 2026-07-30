// 프리톡 사용자 발화 처리 결과 상태를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 사용자 발화 처리 결과 상태를 정의한다. */
public enum FreeTalkTurnStatus {
  /** 일반 AI 후속 메시지가 생성됐다. */
  CONTINUE,

  /** 사용자 종료 의사가 감지되어 FE 확인이 필요하다. */
  EXIT_CONFIRMATION_REQUIRED,

  /** AI 마무리 메시지가 생성되고 세션이 완료됐다. */
  COMPLETED
}
