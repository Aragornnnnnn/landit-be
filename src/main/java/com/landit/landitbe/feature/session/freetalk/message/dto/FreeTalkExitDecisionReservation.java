// 업무 간에 전달할 FreeTalkExitDecisionReservation 값을 정의한다.

package com.landit.landitbe.feature.session.freetalk.message.dto;

import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.session.freetalk.topic.client.ai.AiFreeTalkTopic;
import java.util.List;

/**
 * 종료 확인 전에 저장한 사용자 메시지와 대화 문맥이다.
 *
 * @param userId 요청 사용자 ID
 * @param learningSessionId 학습 세션 ID
 * @param historyId 세션 히스토리 ID
 * @param freeTalkSessionId 프리톡 세션 ID
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param userMessageId 종료 의사가 감지된 사용자 메시지 ID
 * @param decision 사용자가 선택한 종료 여부
 * @param titleGenerationRequired 사용자 선시작 세션의 제목 생성 필요 여부
 * @param targetLocale 학습 대상 언어
 * @param baseLocale 사용자 기준 언어
 * @param topic 프리톡 주제
 * @param history AI에 전달할 대화 기록
 */
public record FreeTalkExitDecisionReservation(
    long userId,
    long learningSessionId,
    long historyId,
    long freeTalkSessionId,
    String characterId,
    long userMessageId,
    FreeTalkExitDecision decision,
    boolean titleGenerationRequired,
    String targetLocale,
    String baseLocale,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> history) {}
