// 프리톡 세션 시작의 사용자 잠금과 일일 한도 검증 순서를 확인한다.

package com.landit.landitbe.feature.session.freetalk.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.tutor.service.ConversationCharacterService;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.dto.UserLearningProfile;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkStartMode;
import com.landit.landitbe.feature.session.freetalk.dto.FreeTalkSessionStartRequest;
import com.landit.landitbe.feature.session.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.freetalk.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.session.history.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.history.repository.SessionHistoryRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** 프리톡 세션 시작의 사용자 잠금과 일일 한도 검증 순서를 확인한다. */
class FreeTalkSessionServiceTest {

  private final UserProfileService userProfileService = mock(UserProfileService.class);
  private final LearningSessionRepository learningSessionRepository =
      mock(LearningSessionRepository.class);
  private final FreeTalkSessionRepository freeTalkSessionRepository =
      mock(FreeTalkSessionRepository.class);
  private final FreeTalkTopicRepository freeTalkTopicRepository =
      mock(FreeTalkTopicRepository.class);
  private final SessionHistoryRepository sessionHistoryRepository =
      mock(SessionHistoryRepository.class);
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository =
      mock(SessionHistoryMessageRepository.class);
  private final FreeTalkDailySpeakingUsageService dailySpeakingUsageService =
      mock(FreeTalkDailySpeakingUsageService.class);
  private final ConversationCharacterService conversationCharacterService =
      mock(ConversationCharacterService.class);
  private final FreeTalkSessionService service =
      new FreeTalkSessionService(
          org.mockito.Mockito.mock(
              com.landit.landitbe.feature.subscription.service.LearningAccessGrantService.class),
          userProfileService,
          learningSessionRepository,
          freeTalkSessionRepository,
          freeTalkTopicRepository,
          sessionHistoryRepository,
          sessionHistoryMessageRepository,
          dailySpeakingUsageService,
          conversationCharacterService);

  /** 사용자 잠금을 얻은 뒤 일일 잔여 시간을 다시 확인하고 세션 저장을 중단한다. */
  @Test
  void checksDailySpeakingTimeAfterLockingUser() {
    UserProfile userProfile = mock(UserProfile.class);
    UserLearningProfile learningProfile = UserLearningProfile.from(userProfile);
    when(userProfileService.requireActiveForUpdate(1L)).thenReturn(learningProfile);
    SessionException exception =
        new SessionException(SessionErrorCode.FREE_TALK_DAILY_SPEAKING_LIMIT_EXCEEDED);
    org.mockito.Mockito.doThrow(exception).when(dailySpeakingUsageService).requireRemaining(1L);

    assertThatThrownBy(
            () ->
                service.createStart(
                    1L, new FreeTalkSessionStartRequest(FreeTalkStartMode.USER_FIRST, null)))
        .isSameAs(exception);

    InOrder order = inOrder(userProfileService, dailySpeakingUsageService);
    order.verify(userProfileService).requireActiveForUpdate(1L);
    order.verify(dailySpeakingUsageService).requireRemaining(1L);
    verifyNoInteractions(
        learningSessionRepository,
        freeTalkSessionRepository,
        freeTalkTopicRepository,
        sessionHistoryRepository,
        sessionHistoryMessageRepository);
  }
}
