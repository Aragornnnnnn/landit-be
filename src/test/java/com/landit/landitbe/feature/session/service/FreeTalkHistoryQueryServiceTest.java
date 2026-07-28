// 지난 프리톡 기록 조회의 콘텐츠 보존과 일괄 조회를 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkExpressionSourceType;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionDetailResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionListResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

/** 지난 프리톡 기록 조회의 콘텐츠 보존과 일괄 조회를 검증한다. */
@ExtendWith(MockitoExtension.class)
class FreeTalkHistoryQueryServiceTest {

  @Mock private LearningSessionRepository learningSessionRepository;
  @Mock private FreeTalkSessionRepository freeTalkSessionRepository;
  @Mock private SessionHistoryRepository sessionHistoryRepository;
  @Mock private SessionHistoryMessageRepository sessionHistoryMessageRepository;
  @Mock private FreeTalkSessionExpressionRepository sessionExpressionRepository;
  @Mock private WritingExpressionRepository writingExpressionRepository;

  @InjectMocks private FreeTalkHistoryQueryService historyQueryService;

  /** 기존 표현이 비활성화돼도 과거 프리톡 상세의 스냅샷은 조회한다. */
  @Test
  void returnsExistingExpressionInHistoryAfterItBecomesInactive() {
    LearningSession learningSession = completedLearningSessionForDetail();
    FreeTalkSession freeTalkSession = completedFreeTalkSessionForDetail(100L);
    SessionHistory history = org.mockito.Mockito.mock(SessionHistory.class);
    when(history.getId()).thenReturn(1_000L);
    FreeTalkSessionExpression sessionExpression =
        org.mockito.Mockito.mock(FreeTalkSessionExpression.class);
    when(sessionExpression.getId()).thenReturn(500L);
    when(sessionExpression.getSourceType()).thenReturn(FreeTalkExpressionSourceType.EXISTING);
    when(sessionExpression.getWritingExpressionId()).thenReturn(300L);
    when(sessionExpression.getDisplayOrder()).thenReturn(1);
    when(sessionExpression.getPersonalizedExampleText()).thenReturn("We made up for it.");
    when(sessionExpression.getPersonalizedExampleTranslation()).thenReturn("우리는 만회했어.");
    WritingExpression historicalExpression = org.mockito.Mockito.mock(WritingExpression.class);
    when(historicalExpression.getId()).thenReturn(300L);
    when(historicalExpression.getTargetExpressionText()).thenReturn("make up for");
    when(historicalExpression.getBaseExpressionMeaningText()).thenReturn("만회하다");
    when(learningSessionRepository.findById(10L)).thenReturn(Optional.of(learningSession));
    when(freeTalkSessionRepository.findByLearningSessionId(10L))
        .thenReturn(Optional.of(freeTalkSession));
    when(sessionHistoryRepository.findByLearningSessionId(10L)).thenReturn(Optional.of(history));
    when(sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(1_000L))
        .thenReturn(List.of());
    when(sessionExpressionRepository.findByFreeTalkSessionIdOrderByDisplayOrderAsc(100L))
        .thenReturn(List.of(sessionExpression));
    when(writingExpressionRepository.findAllById(List.of(300L)))
        .thenReturn(List.of(historicalExpression));

    FreeTalkSessionDetailResponse response = historyQueryService.getSession(1L, 10L);

    assertThat(response.expressions())
        .extracting(FreeTalkSessionDetailResponse.Expression::targetExpressionText)
        .containsExactly("make up for");
  }

  /** 목록 조회는 페이지의 세션과 표현을 일괄 조회한다. */
  @Test
  void loadsSessionListLearningSessionsAndExpressionsInBatches() {
    FreeTalkSession firstSession = listedFreeTalkSession(100L, 10L);
    FreeTalkSession secondSession = listedFreeTalkSession(200L, 20L);
    LearningSession firstLearningSession = listedLearningSession(10L);
    LearningSession secondLearningSession = listedLearningSession(20L);
    when(freeTalkSessionRepository.findCompletedByUserProfileId(any(), any()))
        .thenReturn(new PageImpl<>(List.of(firstSession, secondSession)));
    when(learningSessionRepository.findAllById(List.of(10L, 20L)))
        .thenReturn(List.of(firstLearningSession, secondLearningSession));
    when(sessionExpressionRepository
            .findByFreeTalkSessionIdInOrderByFreeTalkSessionIdAscDisplayOrderAsc(
                List.of(100L, 200L)))
        .thenReturn(List.of());

    FreeTalkSessionListResponse response = historyQueryService.getSessions(1L, 0, 20);

    assertThat(response.items()).hasSize(2);
    verify(learningSessionRepository).findAllById(List.of(10L, 20L));
    verify(sessionExpressionRepository)
        .findByFreeTalkSessionIdInOrderByFreeTalkSessionIdAscDisplayOrderAsc(List.of(100L, 200L));
  }

  private LearningSession completedLearningSessionForDetail() {
    LearningSession learningSession = org.mockito.Mockito.mock(LearningSession.class);
    when(learningSession.getUserProfileId()).thenReturn(1L);
    when(learningSession.getStatus()).thenReturn(LearningSessionStatus.COMPLETED);
    when(learningSession.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 28, 10, 0));
    when(learningSession.getEndedAt()).thenReturn(LocalDateTime.of(2026, 7, 28, 10, 3));
    return learningSession;
  }

  private LearningSession listedLearningSession(long id) {
    LearningSession learningSession = org.mockito.Mockito.mock(LearningSession.class);
    when(learningSession.getId()).thenReturn(id);
    when(learningSession.getStartedAt()).thenReturn(LocalDateTime.of(2026, 7, 28, 10, 0));
    when(learningSession.getEndedAt()).thenReturn(LocalDateTime.of(2026, 7, 28, 10, 3));
    return learningSession;
  }

  private FreeTalkSession completedFreeTalkSessionForDetail(long id) {
    FreeTalkSession freeTalkSession = org.mockito.Mockito.mock(FreeTalkSession.class);
    when(freeTalkSession.getId()).thenReturn(id);
    when(freeTalkSession.getConversationStatus()).thenReturn(FreeTalkConversationStatus.COMPLETED);
    when(freeTalkSession.getExpressionGenerationStatus())
        .thenReturn(ExpressionGenerationStatus.READY);
    when(freeTalkSession.getAccumulatedSpeakingDurationMs()).thenReturn(10_000L);
    return freeTalkSession;
  }

  private FreeTalkSession listedFreeTalkSession(long id, long learningSessionId) {
    FreeTalkSession freeTalkSession = org.mockito.Mockito.mock(FreeTalkSession.class);
    when(freeTalkSession.getId()).thenReturn(id);
    when(freeTalkSession.getLearningSessionId()).thenReturn(learningSessionId);
    when(freeTalkSession.getExpressionGenerationStatus())
        .thenReturn(ExpressionGenerationStatus.READY);
    when(freeTalkSession.getAccumulatedSpeakingDurationMs()).thenReturn(10_000L);
    return freeTalkSession;
  }
}
