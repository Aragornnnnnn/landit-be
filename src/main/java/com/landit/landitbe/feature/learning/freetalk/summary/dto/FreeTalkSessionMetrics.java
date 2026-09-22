// 스몰톡 한 세션의 "얼마나 말했나" 지표 셋을 전달한다.

package com.landit.landitbe.feature.learning.freetalk.summary.dto;

/**
 * 지난번과 비교 카드의 지표 셋이다.
 *
 * @param speakingMs 말한 시간. 사용자 발화 시간의 합(ms). 예: 245000
 * @param turnCount 주고받은 말. 사용자 발화 수. 예: 18
 * @param maxWordsInTurn 가장 길게 말한 턴. 한 발화의 최대 단어 수. 예: 23
 */
public record FreeTalkSessionMetrics(long speakingMs, int turnCount, int maxWordsInTurn) {

  /** 첫 스몰톡의 직전 지표로 쓰는 0이다. */
  public static final FreeTalkSessionMetrics NONE = new FreeTalkSessionMetrics(0, 0, 0);

  /**
   * 지표는 음수가 될 수 없다.
   *
   * @throws IllegalArgumentException 음수 지표가 있을 때
   */
  public FreeTalkSessionMetrics {
    if (speakingMs < 0 || turnCount < 0 || maxWordsInTurn < 0) {
      throw new IllegalArgumentException("세션 지표는 음수일 수 없습니다.");
    }
  }
}
