// 프리톡에서 생성한 Writing 표현의 콘텐츠 데이터를 전달한다.

package com.landit.landitbe.feature.content.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * 프리톡에서 생성한 Writing 표현의 콘텐츠 데이터를 전달한다.
 *
 * @param targetExpressionText 학습 언어 표현
 * @param baseExpressionMeaningText 기준 언어 뜻
 * @param usageSummary 짧은 용법 요약
 * @param usageDescription 상세 용법 설명
 * @param representativeQuestionText 대표 작문 질문. 없으면 null
 * @param representativeQuestionTranslation 대표 작문 질문 번역. 질문이 없으면 null
 * @param representativeSentenceText 대표 예문
 * @param representativeSentenceTranslation 대표 예문 번역
 * @param representativeSentenceWords 대표 예문 정답 단어 배열
 * @param representativeSentenceWordChoices 대표 예문 작문 선택지 배열
 * @param representativeImageUrl 대표 예문 이미지 URL. 없으면 null
 * @param practiceExamplesPayload 추가 예문 JSON 배열
 */
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
