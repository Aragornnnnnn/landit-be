// 프리톡 생성 요청에 포함할 요약 문맥 상태를 담는다.

package com.landit.landitbe.feature.learning.freetalk.context.client.ai;

/**
 * 생성 요청에 전달할 정책 버전, 요약, 불완전 이력 상태다.
 *
 * @param contextPolicyVersion 컨텍스트 정책 버전. 비활성화이면 null
 * @param sessionSummary 요청에 사용할 세션 요약. 없으면 null
 * @param historyIncomplete 전달 이력에 요약으로 보완하지 못한 누락 구간이 있는지 여부
 */
public record AiFreeTalkContextWindow(
    String contextPolicyVersion,
    AiFreeTalkSessionSummary sessionSummary,
    boolean historyIncomplete) {
  /**
   * 기능이 비활성화된 전체 원문 문맥을 만든다.
   *
   * @return 정책과 요약이 없는 전체 원문 문맥
   */
  public static AiFreeTalkContextWindow disabled() {
    return new AiFreeTalkContextWindow(null, null, false);
  }
}
