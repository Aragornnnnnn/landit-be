// 완료된 프리톡 목록 조회의 세션 일괄 로딩을 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionListResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

/** 완료 프리톡 목록에서 학습 세션을 한 번에 조회하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class FreeTalkHistoryQueryServiceTest {

  @Mock private LearningSessionRepository learningSessionRepository;
  @Mock private FreeTalkSessionRepository freeTalkSessionRepository;
  @Mock private SessionHistoryRepository sessionHistoryRepository;
  @Mock private SessionHistoryMessageRepository sessionHistoryMessageRepository;

  @InjectMocks private FreeTalkHistoryQueryService historyQueryService;

  @Test
  void shouldLoadLearningSessionsInBatchForList() {
    FreeTalkSession firstFreeTalk = freeTalkSession(10L, "주말 등산");
    FreeTalkSession secondFreeTalk = freeTalkSession(20L, "카페 이야기");
    LearningSession firstLearningSession = learningSession(10L);
    LearningSession secondLearningSession = learningSession(20L);
    when(freeTalkSessionRepository.findCompletedByUserProfileId(any(), any()))
        .thenReturn(new PageImpl<>(List.of(firstFreeTalk, secondFreeTalk)));
    when(learningSessionRepository.findAllById(any()))
        .thenReturn(List.of(firstLearningSession, secondLearningSession));

    FreeTalkSessionListResponse response = historyQueryService.getSessions(1L, 0, 20);

    assertThat(response.items())
        .extracting(FreeTalkSessionListResponse.Item::sessionId)
        .containsExactly(10L, 20L);
    verify(learningSessionRepository).findAllById(any());
    verify(learningSessionRepository, never()).findById(any());
  }

  private FreeTalkSession freeTalkSession(long learningSessionId, String title) {
    FreeTalkSession session = org.mockito.Mockito.mock(FreeTalkSession.class);
    when(session.getLearningSessionId()).thenReturn(learningSessionId);
    when(session.getTitle()).thenReturn(title);
    return session;
  }

  private LearningSession learningSession(long id) {
    LearningSession session = org.mockito.Mockito.mock(LearningSession.class);
    LocalDateTime now = LocalDateTime.of(2026, 7, 28, 12, 0);
    when(session.getId()).thenReturn(id);
    when(session.getStartedAt()).thenReturn(now);
    when(session.getEndedAt()).thenReturn(now.plusMinutes(3));
    return session;
  }
}
