// 프리톡 첫 AI 메시지 생성 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 첫 AI 메시지 생성 요청을 담는다.
 *
 * @param sessionId 프리톡 세션 ID
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param topic 선택한 추천 주제
 * @param memoryContext 세션 시작에 사용할 범위 검증된 장기기억 문맥
 */
public record AiFreeTalkOpeningRequest(
    Long sessionId,
    String characterId,
    String targetLocale,
    String baseLocale,
    AiFreeTalkTopic topic,
    List<AiFreeTalkMemoryContext> memoryContext) {

  /**
   * 요청 문맥을 방어적으로 복사한다.
   *
   * @param sessionId 프리톡 세션 ID
   * @param characterId 선택한 프리톡 캐릭터 식별자
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param topic 선택한 추천 주제
   * @param memoryContext 세션 시작에 사용할 범위 검증된 장기기억 문맥
   */
  public AiFreeTalkOpeningRequest {
    memoryContext = memoryContext == null ? List.of() : List.copyOf(memoryContext);
  }
}
