// 프리톡 대화에서 추출한 핵심 발화와 임베딩 한 건을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 대화에서 추출한 핵심 발화와 임베딩 한 건을 담는다.
 *
 * @param excerptText 대화에서 추출한 학습 가치가 있는 사용자 발화
 * @param embedding 추출 발화의 임베딩 벡터. 표현 임베딩과 같은 모델로 생성한 {@value #EMBEDDING_DIMENSION}차원 벡터다.
 */
public record AiConversationExcerpt(String excerptText, List<Float> embedding) {

  /** 표현 임베딩(V52)과 동일하게 고정된 임베딩 차원 수다. */
  public static final int EMBEDDING_DIMENSION = 1536;
}
