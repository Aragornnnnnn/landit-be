// 프리톡 턴 응답 생성 방식을 정의한다.

package com.landit.landitbe.feature.session.client.ai;

/** 프리톡 턴 응답 생성 방식을 정의한다. */
public enum AiFreeTalkResponseMode {
  /** 일반 프리톡 후속 메시지를 생성한다. */
  NORMAL,

  /** 사용자가 종료 확인을 취소한 뒤 대화를 이어간다. */
  CONTINUE_AFTER_EXIT_DECLINED
}
