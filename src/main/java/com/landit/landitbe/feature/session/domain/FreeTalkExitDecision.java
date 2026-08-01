// 프리톡 종료 확인에 대한 사용자 선택을 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 종료 확인에 대한 사용자 선택을 정의한다. */
public enum FreeTalkExitDecision {
  /** 종료하지 않고 대화를 이어간다. */
  CONTINUE,

  /** 대화를 종료한다. */
  END
}
