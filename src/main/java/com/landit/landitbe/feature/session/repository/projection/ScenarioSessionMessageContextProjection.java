// 사용자 발화 제출에 필요한 시나리오 컨텍스트 조회 결과를 담는다.

package com.landit.landitbe.feature.session.repository.projection;

import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.Locale;

/**
 * 사용자 발화 제출에 필요한 시나리오 컨텍스트 조회 결과를 담는다.
 *
 * @param scenarioId 시나리오 ID
 * @param title 제목
 * @param briefing 시나리오 설명
 * @param conversationGoal 대화 목표
 * @param counterpartRole 상대 발화자 역할
 * @param firstSpeaker 첫 발화자
 * @param userOpeningInstruction 사용자 첫 발화 안내
 * @param totalQuestionCount 전체 질문 수
 * @param targetLocale 학습 대상 언어
 * @param baseLocale 사용자 기준 언어
 */
public record ScenarioSessionMessageContextProjection(
    Long scenarioId,
    String title,
    String briefing,
    String conversationGoal,
    String counterpartRole,
    ConversationSpeaker firstSpeaker,
    String userOpeningInstruction,
    int totalQuestionCount,
    Locale targetLocale,
    Locale baseLocale) {}
