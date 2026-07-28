// 프리톡 세션 시작과 외부 AI opening 호출을 조율한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTopic;
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

  /** AI 또는 사용자가 먼저 발화하는 프리톡 세션을 시작한다. */
  public FreeTalkSessionStartResponse startFreeTalkSession(
      long userId, FreeTalkSessionStartRequest request) {
    StartedFreeTalkSession startedSession = freeTalkSessionService.createStart(userId, request);
    if (startedSession.startMode() == FreeTalkStartMode.USER_FIRST) {
      return response(startedSession, null);
    }
    try {
      AiFreeTalkOpeningResult openingResult =
          aiFreeTalkClient.generateOpening(openingRequest(startedSession));
      CurrentMessageResponse currentMessage =
          freeTalkSessionService.saveOpening(startedSession, openingResult);
      return response(startedSession, currentMessage);
    } catch (RuntimeException exception) {
      freeTalkSessionService.deleteStart(startedSession.learningSessionId());
      throw exception;
    }
  }

  private AiFreeTalkOpeningRequest openingRequest(StartedFreeTalkSession startedSession) {
    return new AiFreeTalkOpeningRequest(
        startedSession.learningSessionId(),
        startedSession.targetLocale(),
        startedSession.baseLocale(),
        startedSession.partnerDisplayName(),
        startedSession.accentLocale(),
        new AiFreeTalkTopic(
            startedSession.topicId(),
            startedSession.title(),
            startedSession.topicPromptDescription()));
  }

  private FreeTalkSessionStartResponse response(
      StartedFreeTalkSession startedSession, CurrentMessageResponse currentMessage) {
    return FreeTalkSessionStartResponse.from(
        startedSession.learningSessionId(),
        startedSession.startMode().name(),
        startedSession.title(),
        startedSession.ttsVoice(),
        currentMessage);
  }
}
