// 시나리오 질문이 요구하는 답변 표현량을 정의한다.

package com.landit.landitbe.feature.content.domain;

import java.math.BigDecimal;

/** 질문별 수준 평가에 적용할 응답 요구도와 가중치다. */
public enum ResponseDemand {
  LOW("0.35"),
  MEDIUM("0.70"),
  HIGH("1.00");

  private final BigDecimal weight;

  ResponseDemand(String weight) {
    this.weight = new BigDecimal(weight);
  }

  /** 질문별 평가에 반영할 가중치를 반환한다. */
  public BigDecimal weight() {
    return weight;
  }
}
