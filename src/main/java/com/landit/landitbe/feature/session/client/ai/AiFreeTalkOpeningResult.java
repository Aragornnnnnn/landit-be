// 프리톡 첫 AI 메시지 생성 결과를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import java.util.List;

/**
 * 프리톡 첫 AI 메시지 생성 결과를 담는다.
 *
 * @param aiMessage 학습 언어 첫 메시지
 * @param translatedMessage 기준 언어 번역
 * @param emotion AI 상대의 현재 감정
 * @param usedMemoryIds AI가 실제 사용한 장기기억 식별자
 */
public record AiFreeTalkOpeningResult(
    String aiMessage,
    String translatedMessage,
    CharacterEmotion emotion,
    List<Long> usedMemoryIds) {

  /**
   * 사용한 장기기억 목록을 방어적으로 복사한다.
   *
   * @param aiMessage 학습 언어 첫 메시지
   * @param translatedMessage 기준 언어 번역
   * @param emotion AI 상대의 현재 감정
   * @param usedMemoryIds AI가 실제 사용한 장기기억 식별자
   */
  public AiFreeTalkOpeningResult {
    usedMemoryIds = usedMemoryIds == null ? List.of() : List.copyOf(usedMemoryIds);
  }
}
