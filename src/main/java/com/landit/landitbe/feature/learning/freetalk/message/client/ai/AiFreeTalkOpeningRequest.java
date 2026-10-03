// 프리톡 첫 AI 메시지 생성 요청을 담는다.

package com.landit.landitbe.feature.learning.freetalk.message.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.feature.learning.freetalk.followup.dto.AiFreeTalkPendingFollowUp;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
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
 * @param pendingFollowUp 이어서 물어볼 예고 질문. 일반 주제 시작이면 null
 */
public record AiFreeTalkOpeningRequest(
    Long sessionId,
    String characterId,
    String targetLocale,
    String baseLocale,
    AiFreeTalkTopic topic,
    List<AiFreeTalkMemoryContext> memoryContext,
    @JsonInclude(JsonInclude.Include.NON_NULL) AiFreeTalkPendingFollowUp pendingFollowUp) {

  /**
   * 기존 주제 시작 요청을 구성한다.
   *
   * @param sessionId 세션 ID
   * @param characterId 캐릭터 ID
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param topic 선택한 주제
   * @param memoryContext 장기기억 문맥
   */
  public AiFreeTalkOpeningRequest(
      Long sessionId,
      String characterId,
      String targetLocale,
      String baseLocale,
      AiFreeTalkTopic topic,
      List<AiFreeTalkMemoryContext> memoryContext) {
    this(sessionId, characterId, targetLocale, baseLocale, topic, memoryContext, null);
  }

  /**
   * 요청 문맥을 방어적으로 복사한다.
   *
   * @param sessionId 프리톡 세션 ID
   * @param characterId 선택한 프리톡 캐릭터 식별자
   * @param targetLocale 학습 언어
   * @param baseLocale 기준 언어
   * @param topic 선택한 추천 주제
   * @param memoryContext 세션 시작에 사용할 범위 검증된 장기기억 문맥
   * @param pendingFollowUp 이어서 물어볼 예고 질문
   */
  public AiFreeTalkOpeningRequest {
    memoryContext = memoryContext == null ? List.of() : List.copyOf(memoryContext);
  }
}
