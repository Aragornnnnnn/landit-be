// 세션 진행에 필요한 고정 질문 조회 결과를 표현한다.

package com.landit.landitbe.feature.content.infrastructure;

/** 세션 진행에 필요한 고정 질문 조회 결과를 표현한다. */
public record ScenarioQuestionRow(
    Long questionId, int sequence, String questionText, String questionTranslation) {}
