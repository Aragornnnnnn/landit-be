// 프리톡 마무리 AI 메시지 생성 결과를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;

/**
 * 프리톡 마무리 AI 메시지 생성 결과를 담는다.
 *
 * @param inferredTitle 전체 대화에서 생성한 제목
 * @param aiMessage 학습 언어 마무리 메시지
 * @param translatedMessage 기준 언어 번역
 * @param emotion AI 상대의 현재 감정
 */
public record AiFreeTalkClosingResult(
    String inferredTitle, String aiMessage, String translatedMessage, CharacterEmotion emotion) {}
