// 날짜별 시나리오 조회 API의 응답 구조를 정의한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.feature.content.domain.DailyScenarioType;
import com.landit.landitbe.feature.content.repository.projection.DailyScenarioProjection;
import com.landit.landitbe.feature.content.service.ExpressionQueryService.ExpressionProgress;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 날짜별 시나리오 조회 API의 응답 구조를 정의한다.
 *
 * @param date 조회 날짜
 * @param playable 시나리오 시작 또는 복습 가능 여부
 * @param scenario 날짜별 시나리오 정보
 */
@Schema(description = "날짜별 시나리오 조회 응답")
public record DailyScenarioResponse(
    @Schema(description = "조회 날짜") LocalDate date,
    @Schema(description = "시나리오 시작 또는 복습 가능 여부") boolean playable,
    @Schema(description = "날짜별 시나리오 정보") ScenarioResponse scenario) {

  /**
   * 완료 이력이나 오늘 제공할 시나리오가 없는 날짜의 응답을 생성한다.
   *
   * @param date 조회 날짜
   * @return 시나리오가 없는 날짜별 응답
   */
  public static DailyScenarioResponse empty(LocalDate date) {
    return new DailyScenarioResponse(date, false, null);
  }

  /**
   * 시작 또는 복습 가능한 날짜별 시나리오 응답을 생성한다.
   *
   * @param date 조회 날짜
   * @param scenario 날짜별 시나리오 정보
   * @return 시나리오가 있는 날짜별 응답
   */
  public static DailyScenarioResponse playable(LocalDate date, ScenarioResponse scenario) {
    return new DailyScenarioResponse(date, true, scenario);
  }

  /**
   * 날짜별 시나리오 상세 정보를 정의한다.
   *
   * @param scenarioId 시나리오 ID
   * @param scenarioTitle 시나리오 제목
   * @param briefing 시나리오 설명
   * @param conversationGoal 대화 목표
   * @param thumbnailUrl 시나리오 썸네일 URL
   * @param difficulty 시나리오 난이도
   * @param firstSpeaker 첫 발화자
   * @param dailyScenarioType 날짜별 시나리오 상태
   * @param openingPreview 시작 메시지 미리보기
   * @param completed 최초 완료 여부
   * @param completedAt 최초 완료 시각
   * @param starRating 시나리오 최고 별점
   * @param expressionCount 활성 표현 학습 수
   * @param completedExpressionCount 완료한 활성 표현 학습 수
   */
  @Schema(description = "날짜별 시나리오 상세 정보")
  public record ScenarioResponse(
      @Schema(description = "시나리오 ID") Long scenarioId,
      @Schema(description = "시나리오 제목") String scenarioTitle,
      @Schema(description = "시나리오 설명") String briefing,
      @Schema(description = "대화 목표") String conversationGoal,
      @Schema(description = "시나리오 썸네일 URL") String thumbnailUrl,
      @Schema(description = "시나리오 난이도") String difficulty,
      @Schema(description = "첫 발화자") String firstSpeaker,
      @Schema(description = "날짜별 시나리오 상태") DailyScenarioType dailyScenarioType,
      @Schema(description = "시작 메시지 미리보기") OpeningPreviewResponse openingPreview,
      @Schema(description = "최초 완료 여부") boolean completed,
      @Schema(description = "최초 완료 시각") OffsetDateTime completedAt,
      @Schema(description = "시나리오 최고 별점") BigDecimal starRating,
      @Schema(description = "활성 표현 학습 수") int expressionCount,
      @Schema(description = "완료한 활성 표현 학습 수") int completedExpressionCount) {

    /**
     * 콘텐츠 조회 결과와 사용자 진행 상태를 날짜별 시나리오 상세 응답으로 변환한다.
     *
     * @param projection 시나리오 콘텐츠 조회 결과
     * @param dailyScenarioType 날짜별 시나리오 상태
     * @param completed 최초 완료 여부
     * @param completedAt 최초 완료 시각
     * @param expressionProgress 표현 학습 진행도
     * @return 날짜별 시나리오 상세 응답
     */
    public static ScenarioResponse from(
        DailyScenarioProjection projection,
        DailyScenarioType dailyScenarioType,
        boolean completed,
        OffsetDateTime completedAt,
        ExpressionProgress expressionProgress) {
      return new ScenarioResponse(
          projection.scenarioId(),
          projection.scenarioTitle(),
          projection.briefing(),
          projection.conversationGoal(),
          projection.thumbnailUrl(),
          projection.difficulty().name(),
          projection.firstSpeaker().name(),
          dailyScenarioType,
          OpeningPreviewResponse.from(projection),
          completed,
          completedAt,
          completed ? projection.bestStarRating() : null,
          expressionProgress.expressionCount(),
          expressionProgress.completedExpressionCount());
    }
  }

  /**
   * 시나리오 첫 발화에 맞는 시작 메시지 미리보기를 정의한다.
   *
   * @param aiOpeningMessage AI first 시 첫 AI 메시지
   * @param aiOpeningMessageTranslation 첫 AI 메시지 번역
   * @param questionAudioUrl 첫 고정 질문 음원 URL
   * @param userOpeningInstruction USER first 시 사용자 시작 안내
   * @param innerThought 첫 화면에 보여줄 상대 역할의 속마음
   * @param innerThoughtType 속마음 유형
   * @param characterId 시나리오 캐릭터 식별자
   * @param ttsVoice 활성 시나리오 TTS 음성
   */
  @Schema(description = "시나리오 시작 메시지 미리보기")
  public record OpeningPreviewResponse(
      @Schema(description = "AI first 시 첫 AI 메시지") String aiOpeningMessage,
      @Schema(description = "첫 AI 메시지 번역") String aiOpeningMessageTranslation,
      @Schema(description = "첫 고정 질문 음원 URL") String questionAudioUrl,
      @Schema(description = "USER first 시 사용자 시작 안내") String userOpeningInstruction,
      @Schema(description = "첫 화면에 보여줄 상대 역할의 속마음") String innerThought,
      @Schema(description = "속마음 유형") String innerThoughtType,
      @Schema(description = "시나리오 캐릭터 식별자", example = "chloe") String characterId,
      @Schema(description = "활성 시나리오 TTS 음성") TtsVoiceResponse ttsVoice) {

    /**
     * 시나리오 첫 발화에 맞는 시작 메시지 미리보기를 생성한다.
     *
     * @param projection 시나리오 콘텐츠 조회 결과
     * @return 시작 메시지 미리보기
     */
    public static OpeningPreviewResponse from(DailyScenarioProjection projection) {
      if (projection.firstSpeaker() == ConversationSpeaker.AI) {
        return new OpeningPreviewResponse(
            projection.aiOpeningMessage(),
            projection.aiOpeningMessageTranslation(),
            projection.openingQuestionAudioUrl(),
            null,
            projection.innerThought(),
            projection.innerThoughtType() == null ? null : projection.innerThoughtType().name(),
            projection.characterId(),
            TtsVoiceResponse.from(
                projection.ttsVoiceProvider(),
                projection.ttsVoiceModel(),
                projection.providerVoiceId(),
                projection.ttsVoiceGender()));
      }
      return new OpeningPreviewResponse(
          null,
          null,
          null,
          projection.userOpeningInstruction(),
          null,
          null,
          projection.characterId(),
          TtsVoiceResponse.from(
              projection.ttsVoiceProvider(),
              projection.ttsVoiceModel(),
              projection.providerVoiceId(),
              projection.ttsVoiceGender()));
    }
  }
}
