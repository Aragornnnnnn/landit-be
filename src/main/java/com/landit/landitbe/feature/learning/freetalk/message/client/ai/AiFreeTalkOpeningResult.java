// 프리톡 첫 AI 메시지 생성 결과를 담는다.

package com.landit.landitbe.feature.learning.freetalk.message.client.ai;

import com.landit.landitbe.feature.learning.conversation.domain.CharacterEmotion;
import java.util.List;

/**
 * 프리톡 첫 AI 메시지 생성 결과를 담는다.
 *
 * @param aiMessage 학습 언어 첫 메시지
 * @param translatedMessage 기준 언어 번역
 * @param emotion AI 상대의 현재 감정
 * @param usedMemoryIds AI가 실제 사용한 장기기억 식별자
 * @param followUpAsked 예고 질문을 첫 AI 발화에서 실제로 물었는지
 * @param followUpId AI가 받은 예고 질문 ID
 */
public record AiFreeTalkOpeningResult(
    String aiMessage,
    String translatedMessage,
    CharacterEmotion emotion,
    List<Long> usedMemoryIds,
    boolean followUpAsked,
    Long followUpId) {

  /**
   * 기존 일반 시작 AI 응답을 구성한다.
   *
   * @param aiMessage AI 첫 발화
   * @param translatedMessage 번역
   * @param emotion 감정
   * @param usedMemoryIds 사용한 장기기억 ID
   */
  public AiFreeTalkOpeningResult(
      String aiMessage,
      String translatedMessage,
      CharacterEmotion emotion,
      List<Long> usedMemoryIds) {
    this(aiMessage, translatedMessage, emotion, usedMemoryIds, false, null);
  }

  /**
   * 사용한 장기기억 목록을 방어적으로 복사한다.
   *
   * @param aiMessage 학습 언어 첫 메시지
   * @param translatedMessage 기준 언어 번역
   * @param emotion AI 상대의 현재 감정
   * @param usedMemoryIds AI가 실제 사용한 장기기억 식별자
   * @param followUpAsked 실제 예고 질문 여부
   * @param followUpId 예고 질문 ID
   */
  public AiFreeTalkOpeningResult {
    usedMemoryIds = usedMemoryIds == null ? List.of() : List.copyOf(usedMemoryIds);
  }
}
