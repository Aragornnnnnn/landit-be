// 프리톡 세션 시작 전 일일 발화 잔여 시간을 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.service.FreeTalkMemoryRetrievalService;
import com.landit.landitbe.feature.memory.service.MemoryRetrievalStage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.session.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartResponse.CurrentMessageResponse;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 프리톡 세션 시작 전 일일 발화 잔여 시간을 검증한다. */
class FreeTalkSessionStartServiceTest {

  private final FreeTalkSessionService freeTalkSessionService = mock(FreeTalkSessionService.class);
  private final AiFreeTalkClient aiFreeTalkClient = mock(AiFreeTalkClient.class);
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService =
      mock(FreeTalkDailySpeakingUsageService.class);
  private final FreeTalkMemoryRetrievalService memoryRetrievalService =
      mock(FreeTalkMemoryRetrievalService.class);
  private final FreeTalkSessionStartService service =
      new FreeTalkSessionStartService(
          freeTalkSessionService,
          aiFreeTalkClient,
          dailySpeakingUsageService,
          memoryRetrievalService);

  @Test
  void retrievesOpeningMemoryWithTopicAndCharacterQuery() {
    FreeTalkSessionService.StartedFreeTalkSession startedSession =
        new FreeTalkSessionService.StartedFreeTalkSession(
            100L,
            200L,
            300L,
            FreeTalkStartMode.AI_FIRST,
            "chloe",
            10L,
            "Weekend plans",
            "Discuss weekend plans.",
            "EN",
            "KO",
            null);
    FreeTalkMemoryRetrievalService.RetrievalResult memoryResult =
        new FreeTalkMemoryRetrievalService.RetrievalResult(
            300L,
            MemoryRetrievalStage.OPENING,
            List.of(new AiFreeTalkMemoryContext(55L, ConversationMemoryType.EVENT, "hiking")),
            true);
    when(freeTalkSessionService.createStart(
            1L, new FreeTalkSessionStartRequest(FreeTalkStartMode.AI_FIRST, 10L)))
        .thenReturn(startedSession);
    when(memoryRetrievalService.retrieve(any())).thenReturn(memoryResult);
    when(aiFreeTalkClient.generateOpening(any()))
        .thenReturn(new AiFreeTalkOpeningResult("Let's talk.", "이야기하자.", null, List.of(55L)));
    CurrentMessageResponse currentMessage =
        new CurrentMessageResponse(400L, 1, 1, "AI", "Let's talk.", "이야기하자.", null);
    when(freeTalkSessionService.saveOpening(any(), any())).thenReturn(currentMessage);

    service.startFreeTalkSession(
        1L, new FreeTalkSessionStartRequest(FreeTalkStartMode.AI_FIRST, 10L));

    verify(memoryRetrievalService)
        .retrieve(
            argThat(
                request ->
                    request.sessionId() == 300L
                        && request.userProfileId() == 1L
                        && request.query().equals("Weekend plans Discuss weekend plans. chloe")));
    verify(aiFreeTalkClient)
        .generateOpening(
            argThat(
                request ->
                    request.memoryContext().stream()
                        .map(AiFreeTalkMemoryContext::memoryId)
                        .toList()
                        .equals(List.of(55L))));
    verify(memoryRetrievalService).recordUsage(memoryResult, List.of(55L), 400L);
  }

  /** 오늘의 발화 한도를 모두 사용했으면 세션을 생성하지 않는다. */
  @Test
  void rejectsStartWhenDailySpeakingLimitIsUsed() {
    SessionException exception =
        new SessionException(SessionErrorCode.FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED);
    doThrow(exception).when(dailySpeakingUsageService).requireRemaining(1L);

    assertThatThrownBy(
            () ->
                service.startFreeTalkSession(
                    1L, new FreeTalkSessionStartRequest(FreeTalkStartMode.USER_FIRST, null)))
        .isSameAs(exception);

    verify(dailySpeakingUsageService).requireRemaining(1L);
    verifyNoInteractions(freeTalkSessionService, aiFreeTalkClient);
  }
}
