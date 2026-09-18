// 업무 간에 전달할 FreeTalkMessageReservation 값을 정의한다.

package com.landit.landitbe.feature.learning.freetalk.message.dto;

import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import java.time.LocalDate;
import java.util.List;

/**
 * AI 호출 전에 저장한 사용자 발화와 대화 문맥이다.
 *
 * @param userId 요청 사용자 ID
 * @param usageDate 일일 발화 사용량을 예약한 KST 날짜
 * @param learningSessionId 학습 세션 ID
 * @param freeTalkSessionId 프리톡 세션 ID
 * @param characterId 선택한 프리톡 캐릭터 식별자
 * @param historyId 세션 히스토리 ID
 * @param userMessageId 저장된 사용자 메시지 ID
 * @param clientMessageId 중복 요청을 식별하는 클라이언트 메시지 ID
 * @param utteranceDurationMs 이번 사용자 발화 시간 밀리초
 * @param dailyLimitReached 이번 발화 예약 후 일일 한도에 도달했는지 여부
 * @param titleGenerationRequired 사용자 선시작 세션의 제목 생성 필요 여부
 * @param targetLocale 학습 대상 언어
 * @param baseLocale 사용자 기준 언어
 * @param topic 프리톡 주제
 * @param history AI에 전달할 대화 기록
 */
public record FreeTalkMessageReservation(
    long userId,
    LocalDate usageDate,
    long learningSessionId,
    long freeTalkSessionId,
    String characterId,
    long historyId,
    long userMessageId,
    String clientMessageId,
    long utteranceDurationMs,
    boolean dailyLimitReached,
    boolean titleGenerationRequired,
    String targetLocale,
    String baseLocale,
    AiFreeTalkTopic topic,
    List<AiConversationHistoryMessage> history) {}
