// 프리톡에서 생성한 Writing 표현의 콘텐츠 데이터를 전달한다.

package com.landit.landitbe.feature.content.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * 프리톡에서 생성한 Writing 표현의 콘텐츠 데이터를 전달한다.
 *
 * @param targetExpressionText 학습 언어 표현
 * @param baseExpressionMeaningText 기준 언어 표현 뜻
 * @param usageSummary 표현 사용 요약
 * @param usageDescription 표현 사용 설명
 * @param representativeQuestionText 대표 질문 원문
 * @param representativeQuestionTranslation 대표 질문 번역문
 * @param representativeSentenceText 대표 예문 원문
 * @param representativeSentenceTranslation 대표 예문 번역문
 * @param representativeSentenceWords 대표 예문의 단어 배열
 * @param representativeSentenceWordChoices 대표 예문 단어 선택지
 * @param representativeImageUrl 대표 예문 이미지 주소
 * @param practiceExamplesPayload 연습 예문 JSON 배열
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
