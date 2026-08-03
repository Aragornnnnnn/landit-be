// 프리톡 맞춤 표현의 사용자 학습 진행 상태를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 맞춤 표현의 사용자 학습 진행 상태를 정의한다. */
public enum ExpressionLearningStatus {
  /** 추천된 표현을 아직 완료하지 않았다. */
  NOT_STARTED,

  /** 추천된 표현 중 일부를 완료했다. */
  IN_PROGRESS,

  /** 추천된 표현을 모두 완료했다. */
  COMPLETED
}
