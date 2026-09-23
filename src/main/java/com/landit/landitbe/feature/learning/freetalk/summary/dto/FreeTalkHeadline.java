// 스몰톡 요약의 래디 말풍선 헤드라인을 전달한다.

package com.landit.landitbe.feature.learning.freetalk.summary.dto;

import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlinePose;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlineTrigger;

/**
 * 래디 말풍선 헤드라인이다.
 *
 * @param trigger 어떤 사실을 골라 말했는지. 예: SPEAKING_TIME_UP
 * @param text 첫 문장(사실). 예: "지난번보다 1분 24초 더 말했어요!"
 * @param subline 둘째 문장(의미 한 마디). 예: "할 말이 그만큼 늘었다는 거예요."
 * @param pose 래디 포즈. 예: POINT
 */
public record FreeTalkHeadline(
    FreeTalkHeadlineTrigger trigger, String text, String subline, FreeTalkHeadlinePose pose) {

  /**
   * 헤드라인의 값은 모두 필수다.
   *
   * @throws IllegalArgumentException 값이 없거나 문구가 비었을 때
   */
  public FreeTalkHeadline {
    if (trigger == null
        || pose == null
        || text == null
        || text.isBlank()
        || subline == null
        || subline.isBlank()) {
      throw new IllegalArgumentException("헤드라인의 계기·문구·포즈는 필수입니다.");
    }
  }
}
