// 프리톡 AI 캐릭터의 현재 감정을 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 AI 캐릭터의 현재 감정을 정의한다. */
public enum CharacterEmotion {
  /** 특별한 감정 변화가 없는 상태다. */
  NEUTRAL,

  /** 기쁨이나 즐거움을 나타낸다. */
  HAPPY,

  /** 놀람이나 예상 밖의 반응을 나타낸다. */
  SURPRISED,

  /** 슬픔이나 안타까움을 나타낸다. */
  SAD,

  /** 화남이나 불쾌함을 나타낸다. */
  ANGRY
}
