// 다음 스몰톡 후속 질문이 어떤 계기로 만들어졌는지 정의한다.

package com.landit.landitbe.feature.learning.freetalk.followup.domain;

/** 다음 스몰톡 후속 질문의 계기다. AI 서버의 고정 목록과 같다. */
public enum FreeTalkFollowUpTriggerType {
  /** 이번 세션에서 하다가 끊긴 얘기다. */
  CUT_OFF,
  /** 예정되어 있던 일정이 지났다. */
  PAST_EVENT,
  /** 사용자가 말한 고민이다. */
  CONCERN,
  /** 사용자가 말한 목표다. */
  GOAL,
  /** 사용자의 감정 상태다. */
  MOOD,
  /** 사용자의 취미다. */
  HOBBY,
  /** 물어볼 기억이 없어 기본 문구를 쓴다. */
  NONE
}
