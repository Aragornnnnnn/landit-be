// 스몰톡 요약의 실수 기억 카드("지난번엔 헷갈렸던 과거형") 한 장을 전달한다.

package com.landit.landitbe.feature.learning.freetalk.summary.dto;

import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import java.time.LocalDate;

/**
 * 실수 기억 카드다. 직전 스몰톡에서 교정받은 패턴이 이번 스몰톡에 다시 나왔을 때만 있다.
 *
 * @param pattern 실수 패턴. 지켜볼 수 있는 유형만 온다. 예: TENSE
 * @param succeeded 오늘은 맞게 썼으면 true, 오늘도 틀렸으면 false. 예: true
 * @param previousDate 직전 스몰톡 날짜. 예: 2026-09-10
 * @param previousSentence 직전 스몰톡에서 틀렸던 문장. 예: "I go to gym with my friend."
 * @param previousWrongSpan 직전 문장에서 취소선을 그을 구절. 그 교정에 구절이 없었으면 null. 예: "go"
 * @param currentSentence 이번 스몰톡에서 그 패턴이 나온 문장. 예: "I went to the gym with my friend."
 * @param currentSpan 이번 문장에서 강조할 구절. 특정하지 못했으면 null. 예: "went"
 */
public record FreeTalkGrowthCard(
    FreeTalkMistakePattern pattern,
    boolean succeeded,
    LocalDate previousDate,
    String previousSentence,
    String previousWrongSpan,
    String currentSentence,
    String currentSpan) {

  /**
   * 카드의 필수 값을 확인한다.
   *
   * @throws IllegalArgumentException 패턴이 지켜볼 수 없는 유형이거나 날짜·문장이 없을 때
   */
  public FreeTalkGrowthCard {
    if (pattern == null || !pattern.isWatchable()) {
      throw new IllegalArgumentException("실수 기억 카드의 패턴은 지켜볼 수 있는 유형이어야 합니다.");
    }
    if (previousDate == null || blank(previousSentence) || blank(currentSentence)) {
      throw new IllegalArgumentException("실수 기억 카드의 날짜와 문장은 필수입니다.");
    }
    if (blankNonNull(previousWrongSpan) || blankNonNull(currentSpan)) {
      throw new IllegalArgumentException("실수 기억 카드의 구절은 있으면 비어 있지 않아야 합니다.");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static boolean blankNonNull(String value) {
    return value != null && value.isBlank();
  }
}
