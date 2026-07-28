// 프리톡 맞춤 표현 학습 시작 응답과 완료 상태를 표현한다.

package com.landit.landitbe.feature.session.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.feature.content.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.session.domain.FreeTalkExpression;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
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
  public static FreeTalkExpressionLearningResponse fromNew(
      Long sessionExpressionId, FreeTalkExpression expression, boolean completed) {
    return new FreeTalkExpressionLearningResponse(
        sessionExpressionId,
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText(),
        expression.getUsageDescription(),
        expression.getRepresentativeQuestionText(),
        expression.getRepresentativeQuestionTranslation(),
        expression.getRepresentativeSentenceText(),
        expression.getRepresentativeSentenceTranslation(),
        toStringList(expression.getRepresentativeSentenceWords()),
        toStringList(expression.getRepresentativeSentenceWordChoices()),
        expression.getRepresentativeImageUrl(),
        completed);
  }

  private static List<String> toStringList(JsonNode arrayNode) {
    List<String> values = new ArrayList<>();
    arrayNode.forEach(value -> values.add(value.asText()));
    return List.copyOf(values);
  }
}
