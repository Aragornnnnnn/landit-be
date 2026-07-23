// 시나리오 목록 조회 결과 한 행을 담는 JPA projection record다.

package com.landit.landitbe.feature.content.repository.projection;

import com.landit.landitbe.feature.content.domain.ScenarioDifficulty;
import com.landit.landitbe.feature.content.domain.TtsVoiceGender;
import com.landit.landitbe.feature.content.domain.TtsVoiceProvider;
import com.landit.landitbe.feature.learning.domain.UserScenarioProgressStatus;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.math.BigDecimal;

/**
 * 시나리오 목록 조회 결과 한 행을 담는 JPA projection record다.
 *
 * @param categoryId 카테고리 ID
 * @param categoryName 카테고리 이름
 * @param categoryDisplayOrder 카테고리 노출 순서
 * @param categoryStatus 카테고리 활성 상태
 * @param scenarioId 시나리오 ID
 * @param scenarioDisplayOrder 시나리오 노출 순서
 * @param scenarioTitle 시나리오 제목
 * @param briefing 시나리오 설명
 * @param conversationGoal 대화 목표
 * @param difficulty 시나리오 난이도
 * @param firstSpeaker 첫 발화자
 * @param thumbnailUrl 시나리오 썸네일 URL
 * @param scenarioStatus 시나리오 활성 상태
 * @param variantStatus 시나리오 언어 Variant 활성 상태
 * @param aiOpeningMessage 첫 AI 메시지
 * @param aiOpeningMessageTranslation 첫 AI 메시지 번역
 * @param userOpeningInstruction 사용자 첫 발화 안내
 * @param innerThought AI 속마음
 * @param innerThoughtType AI 속마음 유형
 * @param ttsVoiceProvider TTS 제공자
 * @param ttsVoiceModel TTS 음성 모델
 * @param providerVoiceId TTS 제공자 음성 ID
 * @param ttsVoiceGender TTS 음성 성별
 * @param progressStatus 학습 진행 상태
 * @param bestStarRating 최고 별점
 */
public record ScenarioListProjection(
    Long categoryId,
    String categoryName,
    int categoryDisplayOrder,
    ActiveStatus categoryStatus,
    Long scenarioId,
    int scenarioDisplayOrder,
    String scenarioTitle,
    String briefing,
    String conversationGoal,
    ScenarioDifficulty difficulty,
    ConversationSpeaker firstSpeaker,
    String thumbnailUrl,
    ActiveStatus scenarioStatus,
    ActiveStatus variantStatus,
    String aiOpeningMessage,
    String aiOpeningMessageTranslation,
    String userOpeningInstruction,
    String innerThought,
    InnerThoughtType innerThoughtType,
    TtsVoiceProvider ttsVoiceProvider,
    String ttsVoiceModel,
    String providerVoiceId,
    TtsVoiceGender ttsVoiceGender,
    UserScenarioProgressStatus progressStatus,
    BigDecimal bestStarRating) {}
