// BE가 지정한 다음 고정 질문 정보를 AI 요청에 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * BE가 지정한 다음 고정 질문 정보를 AI 요청에 담는다.
 *
 * @param questionId 질문 ID
 * @param sequence 항목 순서
 * @param questionEn 영어 질문
 * @param questionKo 한국어 질문
 */
public record AiNextQuestion(Long questionId, int sequence, String questionEn, String questionKo) {}
