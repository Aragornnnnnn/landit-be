// 세션의 소유자와 유형 및 완료 상태에 따른 접근 계약을 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.session.dto.LearningSessionAccess;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 세션의 소유 상태 계약을 검증한다. */
class LearningSessionOwnershipTest {
  private static final long USER_ID = 10L;
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 11, 12, 0);
  private final LearningSessionService sessions =
      spy(new LearningSessionService(mock(LearningSessionRepository.class)));

  /** 완료한 본인 스몰톡의 결과 복구만 허용하며 다른 세션의 신규 작업은 허용하지 않는다. */
  @Test
  void resultRetryRequiresOwnedCompletedFreeTalk() {
    when(sessions.findOwnedIfPresent(USER_ID, 100L))
        .thenReturn(
            Optional.of(
                new LearningSessionAccess(
                    com.landit.landitbe.feature.session.domain.SessionType.FREE_TALK,
                    com.landit.landitbe.feature.session.domain.LearningSessionStatus.IN_PROGRESS,
                    NOW)));
    assertThat(sessions.ownsCompletedFreeTalk(USER_ID, 100L)).isFalse();
    when(sessions.findOwnedIfPresent(USER_ID, 100L))
        .thenReturn(
            Optional.of(
                new LearningSessionAccess(
                    com.landit.landitbe.feature.session.domain.SessionType.FREE_TALK,
                    com.landit.landitbe.feature.session.domain.LearningSessionStatus.COMPLETED,
                    NOW)));
    assertThat(sessions.ownsCompletedFreeTalk(USER_ID, 100L)).isTrue();
    assertThat(sessions.ownsCompletedFreeTalk(USER_ID + 1, 100L)).isFalse();
  }

  /** 세션 소유권과 종류가 일치하는 경우에만 원래 시작 시각을 권한 판정에 제공한다. */
  @Test
  void verifiedStartRequiresMatchingOwnerAndSessionType() {
    var repository = mock(LearningSessionRepository.class);
    var service = new LearningSessionService(repository);
    var session = mock(com.landit.landitbe.feature.session.domain.LearningSession.class);
    when(repository.findByIdAndUserProfileId(100L, USER_ID)).thenReturn(Optional.of(session));
    when(session.getSessionType())
        .thenReturn(com.landit.landitbe.feature.session.domain.SessionType.SCENARIO);
    when(session.getStartedAt()).thenReturn(NOW);
    assertThat(service.findOwnedStart(USER_ID, 100L, "SCENARIO")).contains(NOW);
    assertThat(service.findOwnedStart(USER_ID, 100L, "FREE_TALK")).isEmpty();
    assertThat(service.findOwnedStart(USER_ID + 1, 100L, "SCENARIO")).isEmpty();
    assertThat(service.findOwnedStart(USER_ID, 101L, "SCENARIO")).isEmpty();
  }
}
