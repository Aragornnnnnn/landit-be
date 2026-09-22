// 스몰톡 요약 헤드라인 옆 래디 캐릭터의 포즈를 정의한다.

package com.landit.landitbe.feature.learning.freetalk.summary.domain;

/** 헤드라인 옆 래디의 포즈다. 화면이 이 값으로 이미지를 고른다. */
public enum FreeTalkHeadlinePose {
  /** 기본. */
  POINT,
  /** 실수 기억 카드가 "아직 헷갈리는" 반복일 때. */
  NORMAL,
  /** 첫 스몰톡. */
  WAVE_SMILE
}
