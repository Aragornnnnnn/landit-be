// 프리톡 턴 교정에서 고른 문장의 대표 실수 유형을 정의한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.domain;

/**
 * 프리톡 턴 교정의 대표 실수 유형이다. 세션을 가로질러 비교하므로 AI 서버와 같은 고정 목록을 쓴다.
 *
 * <p>앞의 아홉 가지는 형태가 단어로 드러나 "다음 세션에서 맞게 썼는지"를 셀 수 있는 유형이다(AI 서버의 WATCHABLE_PATTERNS와 같다). 단어
 * 선택·자연스러움처럼 모든 문장이 사용례가 되는 유형은 지켜볼 수 없다.
 */
public enum FreeTalkMistakePattern {
  TENSE(true, "시제"),
  SUBJECT_VERB_AGREEMENT(true, "주어-동사 일치"),
  VERB_FORM(true, "동사 형태"),
  ARTICLE(true, "관사"),
  PLURAL(true, "복수형"),
  PRONOUN(true, "대명사"),
  PREPOSITION(true, "전치사"),
  NEGATION(true, "부정문"),
  QUESTION_FORM(true, "의문문"),
  WORD_ORDER(false, "어순"),
  MISSING_WORD(false, "빠진 단어"),
  REDUNDANCY(false, "군더더기"),
  WORD_CHOICE(false, "단어 선택"),
  LITERAL_TRANSLATION(false, "직역"),
  REGISTER(false, "말투"),
  NATURALNESS(false, "자연스러움"),
  OTHER(false, "기타");

  private final boolean watchable;
  private final String koreanLabel;

  FreeTalkMistakePattern(boolean watchable, String koreanLabel) {
    this.watchable = watchable;
    this.koreanLabel = koreanLabel;
  }

  /**
   * 화면에 쓰는 한국어 이름을 돌려준다. 기준 언어와 무관하게 한국어로 고정한다(요약 화면 문구가 전부 한국어다).
   *
   * @return 예: "시제"
   */
  public String koreanLabel() {
    return koreanLabel;
  }

  /**
   * 다음 세션에서 지켜볼 수 있는 유형인지 돌려준다.
   *
   * @return AI 서버가 사용례를 판정할 수 있는 유형이면 true
   */
  public boolean isWatchable() {
    return watchable;
  }
}
