// 지난 프리톡 상세 대화 기록을 표현한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.time.LocalDateTime;
import java.util.List;

/** 지난 프리톡 상세 대화 기록을 표현한다. */
public record FreeTalkSessionDetailResponse(
    Long sessionId,
    String title,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    long userSpeakingDurationMs,
    List<Message> messages) {

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
}
