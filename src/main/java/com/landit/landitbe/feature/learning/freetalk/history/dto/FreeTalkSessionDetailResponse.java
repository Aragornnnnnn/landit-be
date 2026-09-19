// 지난 프리톡 상세 대화와 맞춤 표현 상태를 표현한다.

package com.landit.landitbe.feature.learning.freetalk.history.dto;

import com.landit.landitbe.feature.learning.conversation.domain.CharacterEmotion;
import com.landit.landitbe.feature.learning.conversation.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.expression.domain.ExpressionLearningStatus;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 지난 프리톡 상세 대화 기록을 표현한다.
 *
 * @param sessionId 프리톡 학습 세션 ID
 * @param title 프리톡 제목
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param startedAt 세션 시작 시각
 * @param completedAt 세션 완료 시각
 * @param userSpeakingDurationMs 세션의 사용자 발화 시간 합계
 * @param correctionCount 교정이 있는 사용자 메시지 수. 기록 상세의 채팅 아이콘 뱃지 숫자이며 0이면 뱃지가 없다
 * @param messages 전체 대화 메시지
 * @param expressionGenerationStatus 맞춤 표현 생성 상태
 * @param expressionLearningStatus 맞춤 표현 학습 상태
 * @param expressions 이번 프리톡에 연결된 표현 목록
 */
public record FreeTalkSessionDetailResponse(
    Long sessionId,
    String title,
    String characterId,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    long userSpeakingDurationMs,
    int correctionCount,
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
   * @param correctionStatus 사용자 메시지의 교정 처리 상태. AI 메시지는 null
   * @param correction 사용자 메시지의 교정. 고칠 것이 없거나({@code COMPLETED}) 생성 중·실패면 null
   * @param reusedExpression 이 메시지에서 다시 쓴 배운 표현. 아직 판정하지 않아 항상 null
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
      InnerThoughtType innerThoughtType,
      ProcessingStatus correctionStatus,
      Correction correction,
      ReusedExpression reusedExpression) {}

  /**
   * 사용자 메시지 한 턴에서 고른 한 문장의 교정이다.
   *
   * @param originalSentence 이 턴에서 고른 한 문장 원문
   * @param betterSentence 더 자연스러운 문장
   * @param reason 기준 언어로 쓴 이유 한 줄
   * @param mistakePattern 실수 패턴 코드. 화면에 노출하지 않는 참고 값
   * @param memoryTag 장기기억을 근거로 교정했을 때의 태그 문구. 아직 생성하지 않아 항상 null
   */
  public record Correction(
      String originalSentence,
      String betterSentence,
      String reason,
      FreeTalkMistakePattern mistakePattern,
      String memoryTag) {}

  /**
   * 사용자 메시지에서 다시 쓴 배운 표현이다.
   *
   * @param expressionId 공통 표현 ID
   * @param text 표현 원형
   * @param matchedText 메시지 원문 안에서 밑줄을 그을 구절
   */
  public record ReusedExpression(Long expressionId, String text, String matchedText) {}

  /**
   * 세션별 맞춤 표현의 요약이다.
   *
   * @param expressionId 공통 원어민 표현 ID
   * @param displayOrder 세션 내 노출 순서
   * @param targetExpressionText 학습 언어 표현
   * @param baseExpressionMeaningText 기준 언어 표현 뜻
   * @param completed 현재 사용자의 표현 학습 완료 여부
   * @param lastRecommendedAt 이전 프리톡에서 같은 표현을 추천받은 마지막 시각
   */
  public record Expression(
      Long expressionId,
      int displayOrder,
      String targetExpressionText,
      String baseExpressionMeaningText,
      boolean completed,
      LocalDateTime lastRecommendedAt) {}
}
