// LearningSessionService의 소유권과 진행 상태 검증을 단위 테스트한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** LearningSessionService의 소유권과 진행 상태 검증을 단위 테스트한다. */
class LearningSessionServiceTest {

  private final LearningSessionRepository repository = mock(LearningSessionRepository.class);
  private final LearningSessionService service = new LearningSessionService(repository);

  /** 소유한 진행 중 세션은 잠금 조회 결과를 그대로 반환한다. */
  @Test
  void returnsOwnedInProgressSessionForUpdate() {
    LearningSession session = mock(LearningSession.class);
    when(session.isInProgress()).thenReturn(true);
    when(repository.findByIdAndUserProfileIdForUpdate(10L, 1L)).thenReturn(Optional.of(session));

    assertThat(service.findOwnedInProgressForUpdate(1L, 10L)).isSameAs(session);
  }

  /** 존재하지만 다른 사용자의 세션이면 권한 오류로 변환한다. */
  @Test
  void rejectsSessionOwnedByAnotherUser() {
    when(repository.findByIdAndUserProfileId(10L, 1L)).thenReturn(Optional.empty());
    when(repository.existsById(10L)).thenReturn(true);

    assertThatThrownBy(() -> service.findOwned(1L, 10L))
        .isInstanceOf(SessionException.class)
        .extracting("errorCode")
        .isEqualTo(SessionErrorCode.FORBIDDEN);
  }
}
