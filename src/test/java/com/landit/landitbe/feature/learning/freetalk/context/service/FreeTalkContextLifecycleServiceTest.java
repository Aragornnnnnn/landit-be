// 탈퇴 및 세션 완료 시 요약 작업의 상태 잠금이 거부되는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import org.junit.jupiter.api.Test;

class FreeTalkContextLifecycleServiceTest {
  private final UserProfileService profiles = mock(UserProfileService.class);
  private final FreeTalkSessionRepository sessions = mock(FreeTalkSessionRepository.class);
  private final LearningSessionService learning = mock(LearningSessionService.class);
  private final FreeTalkContextLifecycleService service =
      new FreeTalkContextLifecycleService(profiles, sessions, learning);

  @Test
  void withdrawnUserCannotLockSession() {
    assertFalse(service.lockActive(1L, 300L, 30L));
    verifyNoInteractions(sessions, learning);
  }

}
