// 프리톡 신규 표현의 전체 학습 데이터를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 신규 표현의 전체 학습 데이터를 담는다.
 *
 * @param targetExpressionText 학습 언어 표현
 * @param baseExpressionMeaningText 기준 언어 뜻
 * @param usageSummary 짧은 용법 설명
 * @param usageDescription 상세 용법 설명
 * @param representativeQuestionText 대표 예문의 질문
 * @param representativeQuestionTranslation 대표 질문의 기준 언어 번역
 * @param representativeSentenceText 대표 예문
 * @param representativeSentenceTranslation 대표 예문의 기준 언어 번역
 * @param representativeSentenceWords 대표 예문의 정답 단어 배열
 * @param representativeSentenceWordChoices 대표 예문의 선택지
 * @param representativeImageUrl 대표 예문 이미지 URL
 * @param practiceExamples 추가 학습 예문 목록
 */
public record AiFreeTalkExpressionLearningContent(
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
    List<AiFreeTalkExpressionPracticeExample> practiceExamples) {}
