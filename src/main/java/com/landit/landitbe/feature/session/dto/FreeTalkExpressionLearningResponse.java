// 프리톡 맞춤 표현 학습 시작 응답과 완료 상태를 표현한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.content.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionLearningContent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 프리톡 맞춤 표현 학습 시작 응답과 완료 상태를 표현한다. */
@Schema(description = "프리톡 맞춤 표현 학습 시작 응답")
public record FreeTalkExpressionLearningResponse(
    Long sessionExpressionId,
    String targetExpressionText,
    String baseExpressionMeaningText,
    String usageDescription,
    String representativeQuestionText,
    String representativeQuestionTranslation,
    String representativeSentenceText,
    String representativeSentenceTranslation,
    List<String> representativeSentenceWords,
    List<String> representativeSentenceWordChoices,
    String representativeImageUrl,
    boolean completed) {

  /** 기존 Writing 표현 학습 콘텐츠를 프리톡 응답으로 변환한다. */
  public static FreeTalkExpressionLearningResponse fromExisting(
      Long sessionExpressionId, ExpressionLearningResponse response, boolean completed) {
    return new FreeTalkExpressionLearningResponse(
        sessionExpressionId,
        response.targetExpressionText(),
        response.baseExpressionMeaningText(),
        response.usageDescription(),
        response.representativeQuestionText(),
        response.representativeQuestionTranslation(),
        response.representativeSentenceText(),
        response.representativeSentenceTranslation(),
        response.representativeSentenceWords(),
        response.representativeSentenceWordChoices(),
        response.representativeImageUrl(),
        completed);
  }

  /** AI가 생성한 신규 표현 학습 콘텐츠를 프리톡 응답으로 변환한다. */
  public static FreeTalkExpressionLearningResponse fromGenerated(
      Long sessionExpressionId, AiFreeTalkExpressionLearningContent content, boolean completed) {
    return new FreeTalkExpressionLearningResponse(
        sessionExpressionId,
        content.targetExpressionText(),
        content.baseExpressionMeaningText(),
        content.usageDescription(),
        content.representativeQuestionText(),
        content.representativeQuestionTranslation(),
        content.representativeSentenceText(),
        content.representativeSentenceTranslation(),
        content.representativeSentenceWords(),
        content.representativeSentenceWordChoices(),
        content.representativeImageUrl(),
        completed);
  }
}
