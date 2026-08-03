// 프리톡 신규 표현의 추가 학습 예문을 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 신규 표현의 추가 학습 예문을 담는다.
 *
 * @param imageUrl 추가 예문 이미지 URL
 * @param sentenceText 추가 예문
 * @param sentenceWords 예문의 정답 단어 배열
 * @param highlightingPart 예문에서 강조할 표현 구간
 * @param practiceQuestion 예문을 사용할 수 있는 질문
 * @param sentenceTranslation 예문의 기준 언어 번역
 * @param sentenceWordChoices 정답과 오답이 섞인 선택지
 * @param practiceQuestionTranslation 질문의 기준 언어 번역
 */
public record AiFreeTalkExpressionPracticeExample(
    String imageUrl,
    String sentenceText,
    List<String> sentenceWords,
    String highlightingPart,
    String practiceQuestion,
    String sentenceTranslation,
    List<String> sentenceWordChoices,
    String practiceQuestionTranslation) {}
