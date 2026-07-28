// 지난 프리톡 상세 대화와 맞춤 표현 상태를 표현한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.ExpressionLearningStatus;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.time.LocalDateTime;
import java.util.List;

/** 지난 프리톡 상세 대화와 맞춤 표현 상태를 표현한다. */
public record FreeTalkSessionDetailResponse(
    Long sessionId,
    String title,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    long userSpeakingDurationMs,
    List<Message> messages,
    ExpressionGenerationStatus expressionGenerationStatus,
    ExpressionLearningStatus expressionLearningStatus,
    List<Expression> expressions) {

  /** 저장된 대화 메시지다. */
  public record Message(
      Long messageId,
      int turnNumber,
      int messageSequence,
      String role,
      String content,
      String translatedContent,
      CharacterEmotion emotion,
      String innerThought,
      InnerThoughtType innerThoughtType) {}

  /** 세션별 맞춤 표현의 요약이다. */
  public record Expression(
      Long sessionExpressionId,
      int displayOrder,
      String targetExpressionText,
      String baseExpressionMeaningText,
      ContextualExample contextualExample,
      boolean completed) {}

  /** 이번 프리톡에 맞춘 텍스트 예문이다. */
  public record ContextualExample(String sentenceText, String sentenceTranslation) {}
}
