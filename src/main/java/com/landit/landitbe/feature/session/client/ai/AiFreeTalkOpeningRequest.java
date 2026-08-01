// 프리톡 첫 AI 메시지 생성 요청을 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * 프리톡 첫 AI 메시지 생성 요청을 담는다.
 *
 * @param sessionId 프리톡 세션 ID
 * @param targetLocale 학습 언어
 * @param baseLocale 기준 언어
 * @param topic 선택한 추천 주제
 */
public record AiFreeTalkOpeningRequest(
    Long sessionId, String targetLocale, String baseLocale, AiFreeTalkTopic topic) {}
