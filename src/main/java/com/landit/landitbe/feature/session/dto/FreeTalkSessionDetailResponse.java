// 지난 프리톡 상세 대화와 맞춤 표현 상태를 표현한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.ExpressionLearningStatus;
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
 * @param expressionGenerationStatus 맞춤 표현 생성 상태
 * @param expressionLearningStatus 맞춤 표현 학습 상태
 * @param expressions 이번 프리톡에 연결된 표현 목록
 */
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

  /**
   * 세션별 맞춤 표현의 요약이다.
   *
   * @param sessionExpressionId 세션 표현 ID
   * @param displayOrder 세션 내 노출 순서
   * @param targetExpressionText 학습 언어 표현
   * @param baseExpressionMeaningText 기준 언어 표현 뜻
   * @param contextualExample 이번 대화 맥락 예문
   * @param completed 현재 사용자의 표현 학습 완료 여부
   */
  public record Expression(
      Long sessionExpressionId,
      int displayOrder,
      String targetExpressionText,
      String baseExpressionMeaningText,
      ContextualExample contextualExample,
      boolean completed) {}

  /**
   * 이번 프리톡에 맞춘 텍스트 예문이다.
   *
   * @param sentenceText 학습 언어 예문
   * @param sentenceTranslation 기준 언어 번역문
   */
  public record ContextualExample(String sentenceText, String sentenceTranslation) {}
}
