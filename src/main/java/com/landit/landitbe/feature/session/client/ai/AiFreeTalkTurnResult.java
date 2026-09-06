// 프리톡 사용자 발화 처리 결과를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import java.util.List;

/**
 * 프리톡 사용자 발화 처리 결과를 담는다.
 *
 * @param userExitIntentDetected 사용자 종료 의사 감지 여부
 * @param inferredTitle 사용자 선시작 첫 발화에서 추론한 제목
 * @param aiMessage 계속 대화할 때의 AI 메시지
 * @param translatedMessage AI 메시지의 기준 언어 번역
 * @param emotion 계속 대화할 때의 AI 감정
 * @param usedMemoryIds AI가 실제 사용한 장기기억 식별자
 */
public record AiFreeTalkTurnResult(
    boolean userExitIntentDetected,
    String inferredTitle,
    String aiMessage,
    String translatedMessage,
    CharacterEmotion emotion,
    List<Long> usedMemoryIds) {

  /**
   * 사용한 장기기억 목록을 방어적으로 복사한다.
   *
   * @param userExitIntentDetected 사용자 종료 의사 감지 여부
   * @param inferredTitle 사용자 선시작 첫 발화에서 추론한 제목
   * @param aiMessage 계속 대화할 때의 AI 메시지
   * @param translatedMessage AI 메시지의 기준 언어 번역
   * @param emotion 계속 대화할 때의 AI 감정
   * @param usedMemoryIds AI가 실제 사용한 장기기억 식별자
   */
  public AiFreeTalkTurnResult {
    usedMemoryIds = usedMemoryIds == null ? List.of() : List.copyOf(usedMemoryIds);
  }
}
