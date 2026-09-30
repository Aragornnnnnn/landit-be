// 시나리오 총평에서 지난 교정과 비교할 수 있는 문법 실수 유형을 정의한다.

package com.landit.landitbe.feature.learning.scenario.feedback.domain;

/** 스몰톡과 같은 대표 패턴 가운데 세션 간 비교가 가능한 유형이다. */
public enum ScenarioMistakePattern {
  TENSE("시제"),
  SUBJECT_VERB_AGREEMENT("주어-동사 일치"),
  VERB_FORM("동사 형태"),
  ARTICLE("관사"),
  PLURAL("복수형"),
  PRONOUN("대명사"),
  PREPOSITION("전치사"),
  NEGATION("부정문"),
  QUESTION_FORM("의문문");

  private final String koreanLabel;

  ScenarioMistakePattern(String koreanLabel) {
    this.koreanLabel = koreanLabel;
  }

  /**
   * 화면에 사용하는 한국어 패턴 이름을 반환한다.
   *
   * @return 한국어 실수 패턴 이름
   */
  public String koreanLabel() {
    return koreanLabel;
  }
}
