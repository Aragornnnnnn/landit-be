// 총평을 계산할 재료로, 한 세션의 사용자 발화·교정·실수 패턴 사용례를 전달한다.

package com.landit.landitbe.feature.learning.freetalk.summary.dto;

import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkPatternUsageDraft;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import java.util.List;

/**
 * 한 세션에서 총평 계산에 쓰는 재료다. 모두 발화 순서대로 담는다.
 *
 * @param utterances 사용자 발화. 예: [("I went to the gym.", 4200)]
 * @param corrections 판정을 마쳐 문장이 있는 교정. 고칠 것이 없던 턴은 들어 있지 않다
 * @param patternUsages 지켜보던 실수 패턴의 사용례
 */
public record FreeTalkSummarySource(
    List<Utterance> utterances,
    List<FreeTalkTurnCorrection.Sentence> corrections,
    List<FreeTalkPatternUsageDraft> patternUsages) {

  /** 목록을 null 없이 불변으로 보관한다. */
  public FreeTalkSummarySource {
    utterances = utterances == null ? List.of() : List.copyOf(utterances);
    corrections = corrections == null ? List.of() : List.copyOf(corrections);
    patternUsages = patternUsages == null ? List.of() : List.copyOf(patternUsages);
  }

  /**
   * 사용자 발화 하나다.
   *
   * @param content 발화 원문. 예: "I went to the gym."
   * @param utteranceDurationMs 말한 시간(ms). 문자로 보낸 발화처럼 없으면 null. 예: 4200
   */
  public record Utterance(String content, Long utteranceDurationMs) {}
}
