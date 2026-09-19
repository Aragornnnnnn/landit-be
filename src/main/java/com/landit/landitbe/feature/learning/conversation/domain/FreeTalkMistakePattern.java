// 프리톡 턴 교정에서 고른 문장의 대표 실수 유형을 정의한다.

package com.landit.landitbe.feature.learning.conversation.domain;

/** 프리톡 턴 교정의 대표 실수 유형이다. 세션을 가로질러 비교하므로 AI 서버와 같은 고정 목록을 쓴다. */
public enum FreeTalkMistakePattern {
  TENSE,
  SUBJECT_VERB_AGREEMENT,
  VERB_FORM,
  ARTICLE,
  PLURAL,
  PRONOUN,
  PREPOSITION,
  NEGATION,
  QUESTION_FORM,
  WORD_ORDER,
  MISSING_WORD,
  REDUNDANCY,
  WORD_CHOICE,
  LITERAL_TRANSLATION,
  REGISTER,
  NATURALNESS,
  OTHER
}
