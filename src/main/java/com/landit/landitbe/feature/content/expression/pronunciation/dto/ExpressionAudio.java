// 표현과 대표 예문의 발음 음성 주소를 전달한다.

package com.landit.landitbe.feature.content.expression.pronunciation.dto;

/**
 * 준비된 발음 음성 주소다.
 *
 * @param sentenceAudioUrl 대표 예문 음성. 미준비이면 null
 * @param expressionAudioUrl 표현 음성. 미준비이거나 발화 불가이면 null
 */
public record ExpressionAudio(String sentenceAudioUrl, String expressionAudioUrl) {}
