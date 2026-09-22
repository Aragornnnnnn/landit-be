// 프리톡 턴 교정에서 고른 문장의 대표 실수 유형을 정의한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.domain;

/**
 * 프리톡 턴 교정의 대표 실수 유형이다. 세션을 가로질러 비교하므로 AI 서버와 같은 고정 목록을 쓴다.
 *
 * <p>앞의 아홉 가지는 형태가 단어로 드러나 "다음 세션에서 맞게 썼는지"를 셀 수 있는 유형이다(AI 서버의 WATCHABLE_PATTERNS와 같다). 단어
 * 선택·자연스러움처럼 모든 문장이 사용례가 되는 유형은 지켜볼 수 없다.
 */
public enum FreeTalkMistakePattern {
  TENSE(true),
  SUBJECT_VERB_AGREEMENT(true),
  VERB_FORM(true),
  ARTICLE(true),
  PLURAL(true),
  PRONOUN(true),
  PREPOSITION(true),
  NEGATION(true),
  QUESTION_FORM(true),
  WORD_ORDER(false),
  MISSING_WORD(false),
  REDUNDANCY(false),
  WORD_CHOICE(false),
  LITERAL_TRANSLATION(false),
  REGISTER(false),
  NATURALNESS(false),
  OTHER(false);

  private final boolean watchable;

  FreeTalkMistakePattern(boolean watchable) {
    this.watchable = watchable;
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
