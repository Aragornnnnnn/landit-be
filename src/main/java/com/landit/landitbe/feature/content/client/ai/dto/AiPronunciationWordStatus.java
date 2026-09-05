// AI 발음 판정에서 단어 1개의 판정 상태를 정의한다.

package com.landit.landitbe.feature.content.client.ai.dto;

/** AI 발음 판정에서 단어 1개의 판정 상태를 정의한다. */
public enum AiPronunciationWordStatus {
  /** 정상 발음이다. */
  CORRECT,

  /** 음소 오류다. 예: th를 s처럼 발음. */
  PHONEME_ERROR,

  /** 강세 오류다. 예: 강세를 잘못된 음절에 둠. */
  STRESS_ERROR
}
