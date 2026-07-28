// 프리톡 상황에 맞춘 표현 예문을 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * 프리톡 상황에 맞춘 표현 예문을 담는다.
 *
 * @param sentenceText 개인화된 학습 언어 예문
 * @param sentenceTranslation 예문의 기준 언어 번역
 */
public record AiFreeTalkContextualExample(String sentenceText, String sentenceTranslation) {}
