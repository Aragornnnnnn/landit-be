// 대화 진행에 필요한 AI 서버 호출을 추상화한다.

package com.landit.landitbe.feature.session.client.ai;

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
}
