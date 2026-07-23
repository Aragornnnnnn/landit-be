// 세션 진행에 필요한 고정 질문 조회 결과를 표현한다.

package com.landit.landitbe.feature.content.repository.projection;

/**
 * 세션 진행에 필요한 고정 질문 조회 결과를 표현한다.
 *
 * @param questionId 질문 ID
 * @param sequence 항목 순서
 * @param questionText 질문 본문
 * @param questionTranslation 질문 번역
 */
public record ScenarioQuestionProjection(
    Long questionId, int sequence, String questionText, String questionTranslation) {}
