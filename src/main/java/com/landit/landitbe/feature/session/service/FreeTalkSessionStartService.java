// 프리톡 세션 시작과 외부 AI opening 호출을 조율한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.memory.service.FreeTalkMemoryRetrievalService;
import com.landit.landitbe.feature.memory.service.MemoryRetrievalStage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.session.domain.FreeTalkCharacter;
import com.landit.landitbe.feature.session.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartResponse.CurrentMessageResponse;
import com.landit.landitbe.feature.session.service.FreeTalkSessionService.StartedFreeTalkSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 프리톡 세션 시작과 외부 AI opening 호출을 조율한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkSessionStartService {

  private final FreeTalkSessionService freeTalkSessionService;
  private final AiFreeTalkClient aiFreeTalkClient;
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService;
  private final FreeTalkMemoryRetrievalService memoryRetrievalService;

  /**
   * AI 또는 사용자가 먼저 발화하는 프리톡 세션을 시작한다.
   *
   * @param userId 세션을 시작할 사용자 ID
   * @param request 시작 방식과 선택 주제
   * @return 생성된 프리톡 세션 시작 응답
   * @throws com.landit.landitbe.shared.exception.ApiException 요청 또는 AI 생성 결과가 유효하지 않을 때
   * @throws com.landit.landitbe.feature.session.exception.SessionException 당일 발화 한도를 모두 사용했을 때
   */
  public FreeTalkSessionStartResponse startFreeTalkSession(
      long userId, FreeTalkSessionStartRequest request) {
    FreeTalkCharacter.fromId(request == null ? null : request.characterId());
    dailySpeakingUsageService.requireRemaining(userId);
    StartedFreeTalkSession startedSession = freeTalkSessionService.createStart(userId, request);
    if (startedSession.startMode() == FreeTalkStartMode.USER_FIRST) {
      return response(startedSession, null);
    }
    return startAiFirstSession(userId, startedSession);
  }

  private FreeTalkSessionStartResponse startAiFirstSession(
      long userId, StartedFreeTalkSession startedSession) {
    FreeTalkMemoryRetrievalService.RetrievalResult memoryResult =
        memoryRetrievalService.retrieve(
            new FreeTalkMemoryRetrievalService.RetrievalRequest(
                startedSession.freeTalkSessionId(),
                userId,
                startedSession.characterId(),
                MemoryRetrievalStage.OPENING,
                memoryQuery(startedSession)));
    try {
      AiFreeTalkOpeningResult openingResult = generateOpening(startedSession, memoryResult);
      CurrentMessageResponse currentMessage =
          freeTalkSessionService.saveOpening(startedSession, openingResult);
      memoryRetrievalService.recordUsage(
          memoryResult, openingResult.usedMemoryIds(), currentMessage.messageId());
      return response(startedSession, currentMessage);
    } catch (RuntimeException exception) {
      freeTalkSessionService.deleteStart(startedSession.learningSessionId());
      throw exception;
    }
  }

  private AiFreeTalkOpeningResult generateOpening(
      StartedFreeTalkSession startedSession,
      FreeTalkMemoryRetrievalService.RetrievalResult memoryResult) {
    return aiFreeTalkClient.generateOpening(
        openingRequest(startedSession, memoryResult.contexts()));
  }

  private AiFreeTalkOpeningRequest openingRequest(
      StartedFreeTalkSession startedSession,
      java.util.List<AiFreeTalkMemoryContext> memoryContext) {
    return new AiFreeTalkOpeningRequest(
        startedSession.learningSessionId(),
        startedSession.characterId(),
        startedSession.targetLocale(),
        startedSession.baseLocale(),
        new AiFreeTalkTopic(
            startedSession.topicId(),
            startedSession.title(),
            startedSession.topicPromptDescription()),
        memoryContext);
  }

  private String memoryQuery(StartedFreeTalkSession startedSession) {
    return String.join(
        " ",
        java.util.List.of(
                startedSession.title(),
                startedSession.topicPromptDescription(),
                startedSession.characterId())
            .stream()
            .filter(value -> value != null && !value.isBlank())
            .toList());
  }

  private FreeTalkSessionStartResponse response(
      StartedFreeTalkSession startedSession, CurrentMessageResponse currentMessage) {
    return FreeTalkSessionStartResponse.from(
        startedSession.learningSessionId(),
        startedSession.startMode().name(),
        startedSession.characterId(),
        startedSession.title(),
        startedSession.ttsVoice(),
        currentMessage);
  }
}
