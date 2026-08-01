// 프리톡 마무리 메시지 생성 사유를 정의한다.

package com.landit.landitbe.feature.session.client.ai;

/** 프리톡 마무리 메시지 생성 사유를 정의한다. */
public enum AiFreeTalkClosingReason {
  /** 사용자가 종료 확인에 동의한 경우다. */
  USER_CONFIRMED,

  /** 일일 사용자 발화 시간 한도에 도달한 경우다. */
  TIME_LIMIT_REACHED
}
