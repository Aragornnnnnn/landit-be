// 다시 쓴 표현을 어디에서 배웠는지 정의한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain;

/** 다시 쓴 표현을 배운 곳이다. 표현 완료 이력의 학습 출처와 같은 구분이다. */
public enum FreeTalkExpressionReuseSource {
  /** 시나리오 학습에서 배운 표현이다. */
  SCENARIO("시나리오"),
  /** 스몰톡 맞춤 표현 학습에서 배운 표현이다. */
  FREE_TALK("스몰톡");

  private final String koreanLabel;

  FreeTalkExpressionReuseSource(String koreanLabel) {
    this.koreanLabel = koreanLabel;
  }

  /**
   * 출처 라벨에 넣을 한국어 이름이다.
   *
   * @return 예: "시나리오"
   */
  public String koreanLabel() {
    return koreanLabel;
  }
}
