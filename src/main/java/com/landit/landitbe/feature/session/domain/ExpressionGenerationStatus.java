// 프리톡 맞춤 표현 생성 작업의 상태를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 맞춤 표현 생성 작업의 상태를 정의한다. */
public enum ExpressionGenerationStatus {
  /** 맞춤 표현을 생성하거나 기존 표현과 연결하고 있다. */
  PREPARING,

  /** 맞춤 표현 준비가 완료됐다. */
  READY,

  /** 맞춤 표현 준비에 실패했다. */
  FAILED
}
