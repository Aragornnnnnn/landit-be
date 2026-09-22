// 프리톡 사용자 발화 한 턴의 교정 판정 결과를 표현한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.dto;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import java.time.LocalDate;

/**
 * 프리톡 사용자 발화 한 턴의 교정 판정 결과다.
 *
 * <p>판정에 실패하면 {@code FAILED}이고 나머지 값은 모두 null이다. 고칠 것이 없으면 {@code COMPLETED}이고 문장 값만 null이다.
 *
 * @param status 교정 판정 처리 상태
 * @param sentence 고른 한 문장의 교정. 고칠 것이 없거나 판정에 실패하면 null
 * @param reactedToPartner 직전 상대 말을 받아준 뒤 말했는지 여부. 판정에 실패하면 null
 */
public record FreeTalkTurnCorrection(
    ProcessingStatus status, Sentence sentence, Boolean reactedToPartner) {

  /**
   * 고른 한 문장의 교정이다.
   *
   * @param originalSentence 사용자 발화 원문에서 고른 한 문장
   * @param betterSentence 더 자연스러운 문장
   * @param reason 기준 언어로 쓴 이유
   * @param mistakePattern 대표 실수 유형
   * @param usedMemoryId 교정의 근거가 된 장기기억 ID. 기억을 근거로 쓰지 않았으면 null
   * @param memoryObservedOn 근거 기억을 말한 날짜. 지난 기록이 바뀌지 않도록 교정을 저장하는 시점의 값을 남긴다. 근거 기억이 없으면 null
   * @param memoryLabel 근거 기억이 가리키는 대상을 나타내는 짧은 명사구. 근거 기억이 없거나 AI가 라벨을 주지 못했으면 null
   */
  public record Sentence(
      String originalSentence,
      String betterSentence,
      String reason,
      FreeTalkMistakePattern mistakePattern,
      Long usedMemoryId,
      LocalDate memoryObservedOn,
      String memoryLabel) {

    /**
     * 근거 기억과 날짜는 함께 있거나 함께 없고, 라벨은 근거 기억이 있을 때만 가질 수 있다(chk_free_talk_message_feedback_memory).
     *
     * @throws IllegalArgumentException 근거 기억 값의 짝이 맞지 않을 때
     */
    public Sentence {
      if ((usedMemoryId == null) != (memoryObservedOn == null)) {
        throw new IllegalArgumentException("usedMemoryId and memoryObservedOn must come together");
      }
      if (usedMemoryId == null && memoryLabel != null) {
        throw new IllegalArgumentException("memoryLabel requires usedMemoryId");
      }
    }

    /** 장기기억을 근거로 쓰지 않은 교정을 만든다. */
    public Sentence(
        String originalSentence,
        String betterSentence,
        String reason,
        FreeTalkMistakePattern mistakePattern) {
      this(originalSentence, betterSentence, reason, mistakePattern, null, null, null);
    }
  }

  /**
   * 교정 판정에 실패한 결과를 만든다.
   *
   * @return 문장과 반응 여부가 모두 null인 {@code FAILED} 결과
   */
  public static FreeTalkTurnCorrection failed() {
    return new FreeTalkTurnCorrection(ProcessingStatus.FAILED, null, null);
  }

  /**
   * 교정 판정을 마친 결과를 만든다.
   *
   * @param sentence 고른 한 문장의 교정. 고칠 것이 없으면 null
   * @param reactedToPartner 직전 상대 말을 받아준 뒤 말했는지 여부
   * @return {@code COMPLETED} 결과
   */
  public static FreeTalkTurnCorrection completed(Sentence sentence, boolean reactedToPartner) {
    return new FreeTalkTurnCorrection(ProcessingStatus.COMPLETED, sentence, reactedToPartner);
  }
}
