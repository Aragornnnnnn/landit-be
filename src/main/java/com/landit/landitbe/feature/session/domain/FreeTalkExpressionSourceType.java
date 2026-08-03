// 프리톡 표현이 기존 Writing 표현인지 신규 생성 표현인지 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 표현이 기존 Writing 표현인지 신규 생성 표현인지 정의한다. */
public enum FreeTalkExpressionSourceType {
  /** 공통 Writing 표현을 재사용한다. */
  EXISTING,

  /** 프리톡 기록을 바탕으로 새 Writing 표현을 생성한다. */
  NEW
}
