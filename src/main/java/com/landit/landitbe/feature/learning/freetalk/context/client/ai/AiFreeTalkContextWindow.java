// 프리톡 생성 요청에 포함할 요약 문맥 상태를 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

/** 생성 요청에 전달할 정책 버전, 요약, 불완전 이력 상태다. */
public record AiFreeTalkContextWindow(
    String contextPolicyVersion,
    AiFreeTalkSessionSummary sessionSummary,
    boolean historyIncomplete) {
  /** 기능이 비활성화된 전체 원문 문맥을 만든다. */
  public static AiFreeTalkContextWindow disabled() {
    return new AiFreeTalkContextWindow(null, null, false);
  }
}
