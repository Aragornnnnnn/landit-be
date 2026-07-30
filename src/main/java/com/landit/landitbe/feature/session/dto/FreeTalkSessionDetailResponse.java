// 지난 프리톡 상세 대화 기록을 표현한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 지난 프리톡 상세 대화 기록을 표현한다.
 *
 * @param sessionId 프리톡 학습 세션 ID
 * @param title 프리톡 제목
 * @param startedAt 세션 시작 시각
 * @param completedAt 세션 완료 시각
 * @param userSpeakingDurationMs 세션의 사용자 발화 시간 합계
 * @param messages 전체 대화 메시지
 */
public record FreeTalkSessionDetailResponse(
    Long sessionId,
    String title,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    long userSpeakingDurationMs,
    List<Message> messages) {

  /**
   * 저장된 대화 메시지다.
   *
   * @param messageId 메시지 ID
   * @param turnNumber 대화 턴 번호
   * @param messageSequence 세션 내 메시지 순서
   * @param role 메시지 화자
   * @param content 메시지 원문
   * @param translatedContent AI 메시지 번역문
   * @param emotion AI 캐릭터 감정
   * @param innerThought 사용자 메시지에 대한 AI 상대의 속마음
   * @param innerThoughtType 계산된 속마음 유형
   */
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
