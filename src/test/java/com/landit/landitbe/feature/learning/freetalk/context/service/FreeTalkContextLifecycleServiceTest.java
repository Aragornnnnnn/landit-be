// 탈퇴 및 세션 완료 시 요약 작업의 상태 잠금이 거부되는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import java.util.Optional;
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

  @Test
  void completedSessionCannotAcceptSummary() {
    when(profiles.lockActive(1L)).thenReturn(true);
    var session = mock(FreeTalkSession.class);
    when(session.getId()).thenReturn(30L);
    when(session.getConversationStatus()).thenReturn(FreeTalkConversationStatus.COMPLETED);
    when(sessions.findByLearningSessionIdForUpdate(300L)).thenReturn(Optional.of(session));
    assertFalse(service.lockActive(1L, 300L, 30L));
    verifyNoInteractions(learning);
  }

  @Test
  void allStatesMustRemainActive() {
    when(profiles.lockActive(1L)).thenReturn(true);
    var session = mock(FreeTalkSession.class);
    when(session.getId()).thenReturn(30L);
    when(session.getConversationStatus()).thenReturn(FreeTalkConversationStatus.IN_PROGRESS);
    when(sessions.findByLearningSessionIdForUpdate(300L)).thenReturn(Optional.of(session));
    var snapshot = mock(LearningSessionSnapshot.class);
    when(snapshot.getStatus())
        .thenReturn(LearningSessionStatus.IN_PROGRESS, LearningSessionStatus.INTERRUPTED);
    when(learning.lockOwnedSnapshot(300L, 1L)).thenReturn(Optional.of(snapshot));
    assertTrue(service.lockActive(1L, 300L, 30L));
    assertFalse(service.lockActive(1L, 300L, 30L));
  }
}
