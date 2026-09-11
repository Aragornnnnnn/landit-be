// 대화 진행에 필요한 AI 서버 호출을 추상화한다.

package com.landit.landitbe.feature.session.client.ai;

import java.time.Duration;

/** 대화 진행에 필요한 AI 서버 호출을 추상화한다. */
public interface AiConversationClient {

  /** 다음 AI 메시지를 생성한다. */
  AiNextMessageResult generateNextMessage(AiNextMessageRequest request);

  /** 사용자 메시지에 대한 상대 역할 속마음을 생성한다. */
  AiInnerThoughtResult generateInnerThought(AiInnerThoughtRequest request);

  /** 대화 종료 메시지를 생성한다. */
  AiClosingMessageResult generateClosingMessage(AiClosingMessageRequest request);

  /** 사용자 메시지의 피드백 생성을 요청한다. */
  AiMessageFeedbackResult requestMessageFeedback(AiMessageFeedbackRequest request);

  /** 세션 최종 피드백을 생성한다. */
  AiSessionFeedbackResult generateSessionFeedback(AiSessionFeedbackRequest request);

  /**
   * 최종 피드백을 전체 요청의 남은 시간 안에서 생성한다.
   *
   * @param request 최종 피드백 입력
   * @param timeout 외부 호출에 허용되는 남은 시간
   * @return 최종 피드백 결과
   */
  default AiSessionFeedbackResult generateSessionFeedback(
      AiSessionFeedbackRequest request, Duration timeout) {
    return generateSessionFeedback(request);
  }

  /**
   * 세션 최종 피드백과 독립적으로 텍스트 수준 평가를 생성한다.
   *
   * @param request 세션 질문과 사용자 답변을 포함한 평가 입력
   * @return 수준 평가 결과. 평가 미지원 또는 복구 실패 시 null
   */
  default AiSessionLevelAssessment generateSessionLevelAssessment(
      AiSessionFeedbackRequest request) {
    return null;
  }
}
