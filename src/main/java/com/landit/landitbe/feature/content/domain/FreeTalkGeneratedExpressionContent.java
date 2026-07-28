// 프리톡에서 생성한 Writing 표현의 콘텐츠 데이터를 전달한다.

package com.landit.landitbe.feature.content.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** 프리톡에서 생성한 Writing 표현의 콘텐츠 데이터를 전달한다. */
public record FreeTalkGeneratedExpressionContent(
    String targetExpressionText,
    String baseExpressionMeaningText,
    String usageSummary,
    String usageDescription,
    String representativeQuestionText,
    String representativeQuestionTranslation,
    String representativeSentenceText,
    String representativeSentenceTranslation,
    List<String> representativeSentenceWords,
    List<String> representativeSentenceWordChoices,
    String representativeImageUrl,
    JsonNode practiceExamplesPayload) {}
