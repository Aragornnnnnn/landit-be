// 시나리오 목록 조회 API의 응답 구조를 정의한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.feature.content.repository.projection.ScenarioListProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 시나리오 목록 조회 API의 응답 구조를 정의한다.
 *
 * @param categories 카테고리별 시나리오 목록
 */
@Schema(description = "시나리오 전체 조회 응답")
public record ScenarioListResponse(
    @Schema(description = "카테고리별 시나리오 목록") List<CategoryResponse> categories) {

  /** 카테고리 응답 목록을 시나리오 전체 응답으로 변환한다. */
  public static ScenarioListResponse from(List<CategoryResponse> categories) {
    return new ScenarioListResponse(categories);
  }

  /**
   * 내부 타입을 정의한다.
   *
   * @param categoryId 카테고리 ID
   * @param categoryName 카테고리 이름
   * @param displayOrder 카테고리 노출 순서
   * @param categoryLocked 카테고리 잠금 여부
   * @param categoryLockReason 카테고리 잠금 사유
   * @param scenarios 카테고리에 속한 시나리오 목록
   */
  @Schema(description = "시나리오 카테고리 응답")
  public record CategoryResponse(
      @Schema(description = "카테고리 ID") Long categoryId,
      @Schema(description = "카테고리 이름") String categoryName,
      @Schema(description = "카테고리 노출 순서") int displayOrder,
      @Schema(description = "카테고리 잠금 여부") boolean categoryLocked,
      @Schema(description = "카테고리 잠금 사유") String categoryLockReason,
      @Schema(description = "카테고리에 속한 시나리오 목록") List<ScenarioResponse> scenarios) {

    /** 카테고리 메타데이터와 시나리오 목록을 카테고리 응답으로 변환한다. */
    public static CategoryResponse from(
        Long categoryId,
        String categoryName,
        int displayOrder,
        boolean locked,
        String lockReason,
        List<ScenarioResponse> scenarios) {
      return new CategoryResponse(
          categoryId, categoryName, displayOrder, locked, lockReason, scenarios);
    }
  }

  /**
   * 내부 타입을 정의한다.
   *
   * @param scenarioId 시나리오 ID
   * @param starRating 완료한 시나리오의 별점
   * @param displayOrder 카테고리 내 시나리오 노출 순서
   * @param scenarioTitle 시나리오 제목
   * @param briefing 시나리오 설명
   * @param conversationGoal 대화 목표
   * @param difficulty 난이도
   * @param firstSpeaker 첫 발화자
   * @param thumbnailUrl 시나리오 썸네일 URL
   * @param completed 사용자 시나리오 완료 여부
   * @param locked 시나리오 잠금 여부
   * @param lockReason 시나리오 잠금 사유
   * @param openingPreview 잠기지 않은 시나리오의 시작 메시지 미리보기
   */
  @Schema(description = "시나리오 응답")
  public record ScenarioResponse(
      @Schema(description = "시나리오 ID") Long scenarioId,
      @Schema(description = "완료한 시나리오의 별점") BigDecimal starRating,
      @Schema(description = "카테고리 내 시나리오 노출 순서") int displayOrder,
      @Schema(description = "시나리오 제목") String scenarioTitle,
      @Schema(description = "시나리오 설명") String briefing,
      @Schema(description = "대화 목표") String conversationGoal,
      @Schema(description = "난이도") String difficulty,
      @Schema(description = "첫 발화자") String firstSpeaker,
      @Schema(description = "시나리오 썸네일 URL") String thumbnailUrl,
      @Schema(description = "사용자 시나리오 완료 여부") boolean completed,
      @Schema(description = "시나리오 잠금 여부") boolean locked,
      @Schema(description = "시나리오 잠금 사유") String lockReason,
      @Schema(description = "잠기지 않은 시나리오의 시작 메시지 미리보기") OpeningPreviewResponse openingPreview) {

    /** 조회 Projection과 계산된 진행 상태를 시나리오 응답으로 변환한다. */
    public static ScenarioResponse from(
        ScenarioListProjection projection,
        BigDecimal starRating,
        boolean completed,
        boolean locked,
        String lockReason,
        OpeningPreviewResponse openingPreview) {
      return new ScenarioResponse(
          projection.scenarioId(),
          starRating,
          projection.scenarioDisplayOrder(),
          projection.scenarioTitle(),
          projection.briefing(),
          projection.conversationGoal(),
          projection.difficulty().name(),
          projection.firstSpeaker().name(),
          projection.thumbnailUrl(),
          completed,
          locked,
          lockReason,
          openingPreview);
    }
  }

  /**
   * 내부 타입을 정의한다.
   *
   * @param aiOpeningMessage AI first 시 첫 AI 메시지
   * @param aiOpeningMessageTranslation 첫 AI 메시지 번역
   * @param userOpeningInstruction USER first 시 사용자 시작 안내
   * @param innerThought 첫 화면에 보여줄 상대 역할의 속마음
   * @param innerThoughtType 속마음 유형
   * @param ttsVoice 활성 시나리오 TTS 음성. 미설정 또는 비활성 음성이면 null
   */
  @Schema(description = "시작 메시지 미리보기 응답")
  public record OpeningPreviewResponse(
      @Schema(description = "AI first 시 첫 AI 메시지") String aiOpeningMessage,
      @Schema(description = "첫 AI 메시지 번역") String aiOpeningMessageTranslation,
      @Schema(description = "USER first 시 사용자 시작 안내") String userOpeningInstruction,
      @Schema(description = "첫 화면에 보여줄 상대 역할의 속마음") String innerThought,
      @Schema(description = "속마음 유형") String innerThoughtType,
      @Schema(description = "활성 시나리오 TTS 음성. 미설정 또는 비활성 음성이면 null") TtsVoiceResponse ttsVoice) {

    /** AI가 먼저 발화하는 시나리오의 미리보기를 생성한다. */
    public static OpeningPreviewResponse fromAi(ScenarioListProjection projection) {
      return new OpeningPreviewResponse(
          projection.aiOpeningMessage(),
          projection.aiOpeningMessageTranslation(),
          null,
          projection.innerThought(),
          projection.innerThoughtType() == null ? null : projection.innerThoughtType().name(),
          TtsVoiceResponse.from(
              projection.ttsVoiceProvider(),
              projection.ttsVoiceModel(),
              projection.providerVoiceId(),
              projection.ttsVoiceGender()));
    }

    /** 사용자가 먼저 발화하는 시나리오의 미리보기를 생성한다. */
    public static OpeningPreviewResponse fromUser(ScenarioListProjection projection) {
      return new OpeningPreviewResponse(
          null,
          null,
          projection.userOpeningInstruction(),
          null,
          null,
          TtsVoiceResponse.from(
              projection.ttsVoiceProvider(),
              projection.ttsVoiceModel(),
              projection.providerVoiceId(),
              projection.ttsVoiceGender()));
    }
  }
}
