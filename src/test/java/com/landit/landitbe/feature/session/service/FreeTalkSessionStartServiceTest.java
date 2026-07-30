// 프리톡 세션 시작 전 일일 발화 잔여 시간을 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import org.junit.jupiter.api.Test;

/** 프리톡 세션 시작 전 일일 발화 잔여 시간을 검증한다. */
class FreeTalkSessionStartServiceTest {

  private final FreeTalkSessionService freeTalkSessionService = mock(FreeTalkSessionService.class);
  private final AiFreeTalkClient aiFreeTalkClient = mock(AiFreeTalkClient.class);
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService =
      mock(FreeTalkDailySpeakingUsageService.class);
  private final FreeTalkSessionStartService service =
      new FreeTalkSessionStartService(
          freeTalkSessionService, aiFreeTalkClient, dailySpeakingUsageService);

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
