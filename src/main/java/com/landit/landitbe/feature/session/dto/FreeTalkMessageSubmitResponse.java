// 프리톡 발화 처리 결과와 진행 상태를 반환한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.shared.domain.InnerThoughtType;

/** 프리톡 발화 처리 결과와 진행 상태를 반환한다. */
public record FreeTalkMessageSubmitResponse(
    Long sessionId,
    String title,
    FreeTalkTurnStatus turnStatus,
    SubmittedMessageResponse submittedMessage,
    NextMessageResponse nextMessage,
    ProgressResponse progress) {

  /** 사용자가 제출한 발화의 응답 표현이다. */
  public record SubmittedMessageResponse(
      Long messageId,
      int turnNumber,
      int messageSequence,
      String role,
      String innerThought,
      InnerThoughtType innerThoughtType) {
    /** 세션 메시지 엔티티를 응답 값으로 변환한다. */
    public static SubmittedMessageResponse from(SessionHistoryMessage message) {
      return new SubmittedMessageResponse(
          message.getId(),
          message.getTurnNumber(),
          message.getMessageSequence(),
          message.getRole().name(),
          message.getInnerThought(),
          message.getInnerThoughtType());
    }
  }

  /** AI 후속 발화의 응답 표현이다. */
  public record NextMessageResponse(
      Long messageId,
      int turnNumber,
      int messageSequence,
      String role,
      String content,
      String translatedContent,
      CharacterEmotion emotion) {
    /** 세션 메시지 엔티티를 응답 값으로 변환한다. */
    public static NextMessageResponse from(SessionHistoryMessage message) {
      return new NextMessageResponse(
          message.getId(),
          message.getTurnNumber(),
          message.getMessageSequence(),
          message.getRole().name(),
          message.getContent(),
          message.getTranslatedContent(),
          message.getEmotion());
    }
  }

  /** 프리톡 진행 상태의 응답 표현이다. */
  public record ProgressResponse(
      FreeTalkConversationStatus sessionStatus,
      long accumulatedSpeakingDurationMs,
      long speakingTimeLimitMs,
      ExpressionGenerationStatus expressionGenerationStatus) {}
}
