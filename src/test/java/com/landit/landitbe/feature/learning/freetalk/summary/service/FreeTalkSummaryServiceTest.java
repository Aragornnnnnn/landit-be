// 요약 조회가 소유·완료를 확인하고, 총평을 교정이 끝난 뒤 한 번만 계산해 저장하며, 멈춘 장기기억 작업을 상한 뒤 확정하는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.summary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.exception.SessionErrorCode;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkExpressionReuseSummary;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.service.FreeTalkExpressionReuseQueryService;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkPatternUsage;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.feedback.repository.FreeTalkPatternUsageRepository;
import com.landit.landitbe.feature.learning.freetalk.feedback.service.FreeTalkMessageFeedbackService;
import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUpTriggerType;
import com.landit.landitbe.feature.learning.freetalk.followup.dto.FreeTalkFollowUpSummary;
import com.landit.landitbe.feature.learning.freetalk.followup.service.FreeTalkFollowUpService;
import com.landit.landitbe.feature.learning.freetalk.memory.domain.MemoryGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlineTrigger;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkSessionSummary;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSessionSummaryResponse;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSummarySource;
import com.landit.landitbe.feature.learning.freetalk.summary.repository.FreeTalkSessionSummaryRepository;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;

/** 요약 조회가 소유·완료를 확인하고, 총평을 교정이 끝난 뒤 한 번만 계산해 저장하며, 멈춘 장기기억 작업을 상한 뒤 확정하는지 검증한다. */
class FreeTalkSummaryServiceTest {

  private static final long USER_ID = 1207L;
  private static final long LEARNING_SESSION_ID = 300L;
  private static final long FREE_TALK_SESSION_ID = 30L;
  private static final long HISTORY_ID = 3100L;
  private static final long PREVIOUS_LEARNING_SESSION_ID = 200L;
  private static final long PREVIOUS_HISTORY_ID = 2100L;
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  // 세션은 서울 시간 2026-09-15 23:50에 시작해 자정을 넘겨 09-16 00:10에 끝났다. 지난 일수는 시작 날짜 기준이어야 한다.
  private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 9, 15, 23, 50);
  private static final LocalDateTime ENDED_AT = LocalDateTime.of(2026, 9, 16, 0, 10);

  private final LearningSessionService learningSessionService = mock(LearningSessionService.class);
  private final FreeTalkSessionRepository freeTalkSessionRepository =
      mock(FreeTalkSessionRepository.class);
  private final SessionHistoryService sessionHistoryService = mock(SessionHistoryService.class);
  private final ConversationMessageService conversationMessageService =
      mock(ConversationMessageService.class);
  private final FreeTalkMessageFeedbackService messageFeedbackService =
      mock(FreeTalkMessageFeedbackService.class);
  private final FreeTalkPatternUsageRepository patternUsageRepository =
      mock(FreeTalkPatternUsageRepository.class);
  private final FreeTalkSessionSummaryRepository summaryRepository =
      mock(FreeTalkSessionSummaryRepository.class);
  private final FreeTalkExpressionReuseQueryService expressionReuseQueryService =
      mock(FreeTalkExpressionReuseQueryService.class);
  private final FreeTalkFollowUpService followUpService = mock(FreeTalkFollowUpService.class);
  private final FreeTalkSession session = mock(FreeTalkSession.class);
  private final LearningSessionSnapshot learningSession = mock(LearningSessionSnapshot.class);

  @BeforeEach
  void stubCompletedSession() {
    when(learningSession.getId()).thenReturn(LEARNING_SESSION_ID);
    when(learningSession.getUserProfileId()).thenReturn(USER_ID);
    when(learningSession.getStatus()).thenReturn(LearningSessionStatus.COMPLETED);
    when(learningSession.getStartedAt()).thenReturn(STARTED_AT);
    when(learningSession.getEndedAt()).thenReturn(ENDED_AT);
    when(learningSessionService.findSession(LEARNING_SESSION_ID))
        .thenReturn(Optional.of(learningSession));
    when(session.getId()).thenReturn(FREE_TALK_SESSION_ID);
    when(session.getLearningSessionId()).thenReturn(LEARNING_SESSION_ID);
    when(session.getTitle()).thenReturn("카페 얘기");
    when(session.getConversationStatus()).thenReturn(FreeTalkConversationStatus.COMPLETED);
    when(session.getExpressionGenerationStatus()).thenReturn(ExpressionGenerationStatus.READY);
    when(session.getMemoryGenerationStatus()).thenReturn(MemoryGenerationStatus.READY);
    when(freeTalkSessionRepository.findByLearningSessionId(LEARNING_SESSION_ID))
        .thenReturn(Optional.of(session));
    SessionHistorySnapshot history = mock(SessionHistorySnapshot.class);
    when(history.getId()).thenReturn(HISTORY_ID);
    when(sessionHistoryService.findByLearningSessionId(LEARNING_SESSION_ID))
        .thenReturn(Optional.of(history));
    List<SessionHistoryMessageSnapshot> messages =
        List.of(
            message(5503L, ConversationSpeaker.AI, "Hi!", null),
            message(5504L, ConversationSpeaker.USER, "I went to the gym.", 4200L),
            message(5505L, ConversationSpeaker.USER, "It was fun.", 1800L));
    when(conversationMessageService.findAll(HISTORY_ID)).thenReturn(messages);
    when(messageFeedbackService.findBySessionHistoryId(HISTORY_ID)).thenReturn(Map.of());
    when(patternUsageRepository.findBySessionHistoryIdOrderByIdAsc(anyLong()))
        .thenReturn(List.of());
    when(freeTalkSessionRepository.findPreviousCompleted(LEARNING_SESSION_ID, PageRequest.of(0, 1)))
        .thenReturn(List.of());
    when(summaryRepository.findByFreeTalkSessionId(FREE_TALK_SESSION_ID))
        .thenReturn(Optional.empty());
    when(summaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(expressionReuseQueryService.findSummary(anyLong(), any()))
        .thenReturn(new FreeTalkExpressionReuseSummary(false, List.of()));
    when(followUpService.findSummary(anyLong(), any())).thenReturn(FreeTalkFollowUpSummary.none());
  }

  @DisplayName("세션이 없으면 404, 다른 사용자면 403, 아직 완료되지 않았으면 409다.")
  @Test
  void rejectsMissingForeignAndUnfinishedSessions() {
    assertThatThrownBy(() -> service(ENDED_AT).getSummary(USER_ID, 999L))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);
    assertThatThrownBy(() -> service(ENDED_AT).getSummary(USER_ID + 1, LEARNING_SESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);

    when(session.getConversationStatus()).thenReturn(FreeTalkConversationStatus.IN_PROGRESS);
    assertThatThrownBy(() -> service(ENDED_AT).getSummary(USER_ID, LEARNING_SESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(SessionErrorCode.SESSION_NOT_COMPLETED);
    verify(summaryRepository, never()).save(any());
  }

  @DisplayName("교정이 아직 준비 상태이고 종료 후 상한 전이면 총평을 계산하지 않고 pending으로 돌려준다.")
  @Test
  void pendsWhileCorrectionsArePreparingWithinWait() {
    when(messageFeedbackService.findBySessionHistoryId(HISTORY_ID))
        .thenReturn(
            Map.of(5504L, new FreeTalkTurnCorrection(ProcessingStatus.PREPARING, null, null)));

    FreeTalkSessionSummaryResponse response =
        service(ENDED_AT.plusSeconds(29)).getSummary(USER_ID, LEARNING_SESSION_ID);

    assertThat(response.pending()).isTrue();
    assertThat(response.headline()).isNull();
    assertThat(response.correctionCount()).isNull();
    assertThat(response.title()).isEqualTo("카페 얘기");
    verify(summaryRepository, never()).save(any());
  }

  @DisplayName("상한이 지나면 아직 준비 상태인 교정을 빼고 확정해 저장한다.")
  @Test
  void settlesAfterWaitExcludingPreparingCorrections() {
    when(messageFeedbackService.findBySessionHistoryId(HISTORY_ID))
        .thenReturn(
            Map.of(
                5504L,
                FreeTalkTurnCorrection.completed(
                    new FreeTalkTurnCorrection.Sentence(
                        "I go to the gym.",
                        "I went to the gym.",
                        "과거",
                        FreeTalkMistakePattern.TENSE),
                    true),
                5505L,
                new FreeTalkTurnCorrection(ProcessingStatus.PREPARING, null, null)));

    FreeTalkSessionSummaryResponse response =
        service(ENDED_AT.plusSeconds(30)).getSummary(USER_ID, LEARNING_SESSION_ID);

    assertThat(response.pending()).isFalse();
    assertThat(response.correctionCount()).isEqualTo(1);
    assertThat(response.firstSession()).isTrue();
    assertThat(response.comparison().current().speakingMs()).isEqualTo(6000);
    assertThat(response.comparison().current().turnCount()).isEqualTo(2);
    assertThat(response.comparison().previous().speakingMs()).isZero();
    verify(summaryRepository).save(any());
  }

  @DisplayName("저장된 총평이 있으면 다시 계산하지 않고 그대로 돌려준다.")
  @Test
  void returnsStoredSummaryWithoutRecalculating() {
    FreeTalkSessionSummary stored = storedSummary();
    when(summaryRepository.findByFreeTalkSessionId(FREE_TALK_SESSION_ID))
        .thenReturn(Optional.of(stored));

    FreeTalkSessionSummaryResponse response =
        service(ENDED_AT.plusDays(3)).getSummary(USER_ID, LEARNING_SESSION_ID);

    assertThat(response.headline().text()).isEqualTo("첫 스몰톡 완주 축하해요!");
    verify(messageFeedbackService, never()).findBySessionHistoryId(anyLong());
    verify(summaryRepository, never()).save(any());
  }

  @DisplayName("동시에 두 조회가 계산하면 저장에 진 쪽은 저장된 총평을 다시 읽어 같은 값을 돌려준다.")
  @Test
  void rereadsSummaryWhenAnotherRequestSavedFirst() {
    FreeTalkSessionSummary stored = storedSummary();
    when(summaryRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
    when(summaryRepository.findByFreeTalkSessionId(FREE_TALK_SESSION_ID))
        .thenReturn(Optional.empty(), Optional.of(stored));

    FreeTalkSessionSummaryResponse response =
        service(ENDED_AT.plusSeconds(1)).getSummary(USER_ID, LEARNING_SESSION_ID);

    assertThat(response.pending()).isFalse();
    assertThat(response.headline().text()).isEqualTo("첫 스몰톡 완주 축하해요!");
  }

  @DisplayName("직전 스몰톡이 있으면 그 세션의 발화·교정·사용례를 읽어 비교하고 직전 날짜와 지난 일수를 계산한다.")
  @Test
  void comparesWithPreviousSession() {
    FreeTalkSession previousSession = mock(FreeTalkSession.class);
    when(previousSession.getLearningSessionId()).thenReturn(PREVIOUS_LEARNING_SESSION_ID);
    when(freeTalkSessionRepository.findPreviousCompleted(LEARNING_SESSION_ID, PageRequest.of(0, 1)))
        .thenReturn(List.of(previousSession));
    LearningSessionSnapshot previousLearning = mock(LearningSessionSnapshot.class);
    when(previousLearning.getEndedAt()).thenReturn(LocalDateTime.of(2026, 9, 10, 23, 50));
    when(learningSessionService.findSession(PREVIOUS_LEARNING_SESSION_ID))
        .thenReturn(Optional.of(previousLearning));
    SessionHistorySnapshot previousHistory = mock(SessionHistorySnapshot.class);
    when(previousHistory.getId()).thenReturn(PREVIOUS_HISTORY_ID);
    when(sessionHistoryService.findByLearningSessionId(PREVIOUS_LEARNING_SESSION_ID))
        .thenReturn(Optional.of(previousHistory));
    List<SessionHistoryMessageSnapshot> previousMessages =
        List.of(message(4504L, ConversationSpeaker.USER, "I go to gym.", 3000L));
    when(conversationMessageService.findAll(PREVIOUS_HISTORY_ID)).thenReturn(previousMessages);
    when(messageFeedbackService.findBySessionHistoryId(PREVIOUS_HISTORY_ID))
        .thenReturn(
            Map.of(
                4504L,
                FreeTalkTurnCorrection.completed(
                    new FreeTalkTurnCorrection.Sentence(
                        "I go to gym.",
                        "I went to the gym.",
                        "과거",
                        FreeTalkMistakePattern.TENSE,
                        "go",
                        "went"),
                    true)));
    when(patternUsageRepository.findBySessionHistoryIdOrderByIdAsc(HISTORY_ID))
        .thenReturn(
            List.of(
                FreeTalkPatternUsage.of(
                    5504L,
                    HISTORY_ID,
                    FreeTalkMistakePattern.TENSE,
                    "I went to the gym.",
                    "went",
                    true)));

    FreeTalkSessionSummaryResponse response =
        service(ENDED_AT.plusSeconds(1)).getSummary(USER_ID, LEARNING_SESSION_ID);

    assertThat(response.firstSession()).isFalse();
    assertThat(response.comparison().previousSessionId()).isEqualTo(PREVIOUS_LEARNING_SESSION_ID);
    assertThat(response.comparison().previousDate())
        .isEqualTo(LocalDateTime.of(2026, 9, 10, 23, 50).toLocalDate());
    assertThat(response.comparison().previous().speakingMs()).isEqualTo(3000);
    assertThat(response.growth().pattern()).isEqualTo(FreeTalkMistakePattern.TENSE);
    assertThat(response.growth().patternLabel()).isEqualTo("시제");
    assertThat(response.growth().succeeded()).isTrue();
    assertThat(response.growth().previousWrongSpan()).isEqualTo("go");
    assertThat(response.growth().currentSpan()).isEqualTo("went");
    ArgumentCaptor<FreeTalkSessionSummary> saved =
        ArgumentCaptor.forClass(FreeTalkSessionSummary.class);
    verify(summaryRepository).save(saved.capture());
    assertThat(saved.getValue().getDaysSincePrevious()).isEqualTo(5);
    assertThat(saved.getValue().getHeadlineTrigger()).isEqualTo(FreeTalkHeadlineTrigger.GROWTH);
  }

  @DisplayName("장기기억 작업이 종료 후 상한을 넘겨도 준비 상태면 DB에 실패로 확정하고 최신 상태로 후속 질문을 읽는다.")
  @Test
  void failsStaleMemoryGenerationAfterWait() {
    when(session.getMemoryGenerationStatus()).thenReturn(MemoryGenerationStatus.PREPARING);
    when(freeTalkSessionRepository.failStaleMemoryGeneration(eq(FREE_TALK_SESSION_ID), any()))
        .thenReturn(1);
    // 다시 읽은 세션은 제목·표현 상태도 달라져 있어, 응답이 옛 객체가 아니라 다시 읽은 쪽을 쓰는지 드러난다.
    FreeTalkSession refreshed = mock(FreeTalkSession.class);
    when(refreshed.getId()).thenReturn(FREE_TALK_SESSION_ID);
    when(refreshed.getTitle()).thenReturn("다시 읽은 제목");
    when(refreshed.getExpressionGenerationStatus()).thenReturn(ExpressionGenerationStatus.FAILED);
    when(refreshed.getMemoryGenerationStatus()).thenReturn(MemoryGenerationStatus.FAILED);
    when(freeTalkSessionRepository.findById(FREE_TALK_SESSION_ID))
        .thenReturn(Optional.of(refreshed));

    FreeTalkSessionSummaryResponse response =
        service(ENDED_AT.plusMinutes(5)).getSummary(USER_ID, LEARNING_SESSION_ID);

    verify(freeTalkSessionRepository)
        .failStaleMemoryGeneration(FREE_TALK_SESSION_ID, ENDED_AT.plusMinutes(5));
    verify(followUpService).findSummary(FREE_TALK_SESSION_ID, MemoryGenerationStatus.FAILED);
    verify(expressionReuseQueryService)
        .findSummary(FREE_TALK_SESSION_ID, ExpressionGenerationStatus.FAILED);
    assertThat(response.title()).isEqualTo("다시 읽은 제목");
  }

  @DisplayName("작업이 이미 선점됐으면 상한은 종료가 아니라 선점 시각부터 세어, 늦게 선점된 정상 작업을 죽이지 않는다.")
  @Test
  void anchorsMemoryWaitOnClaimTimeWhenClaimed() {
    when(session.getMemoryGenerationStatus()).thenReturn(MemoryGenerationStatus.PREPARING);
    // 종료 4분 뒤에 선점됐다. 종료 기준으로는 5분이 지났지만 선점 기준으로는 2분밖에 안 지났다.
    when(session.getMemoryGenerationStartedAt()).thenReturn(ENDED_AT.plusMinutes(4));

    service(ENDED_AT.plusMinutes(6)).getSummary(USER_ID, LEARNING_SESSION_ID);
    verify(freeTalkSessionRepository, never()).failStaleMemoryGeneration(anyLong(), any());

    service(ENDED_AT.plusMinutes(9)).getSummary(USER_ID, LEARNING_SESSION_ID);
    verify(freeTalkSessionRepository)
        .failStaleMemoryGeneration(FREE_TALK_SESSION_ID, ENDED_AT.plusMinutes(9));
  }

  @DisplayName("장기기억 작업이 상한 전이거나 이미 끝났으면 확정하지 않는다.")
  @Test
  void leavesMemoryGenerationAloneBeforeWaitOrWhenFinished() {
    when(session.getMemoryGenerationStatus()).thenReturn(MemoryGenerationStatus.PREPARING);
    service(ENDED_AT.plusMinutes(5).minusSeconds(1)).getSummary(USER_ID, LEARNING_SESSION_ID);

    when(session.getMemoryGenerationStatus()).thenReturn(MemoryGenerationStatus.READY);
    service(ENDED_AT.plusDays(1)).getSummary(USER_ID, LEARNING_SESSION_ID);

    verify(freeTalkSessionRepository, never()).failStaleMemoryGeneration(anyLong(), any());
    verify(followUpService).findSummary(FREE_TALK_SESSION_ID, MemoryGenerationStatus.PREPARING);
  }

  @DisplayName("프리톡이 아닌 학습 세션이면 404이고, 남의 세션은 완료 전이어도 409가 아니라 403이다.")
  @Test
  void rejectsNonFreeTalkSessionAndChecksOwnershipBeforeCompletion() {
    when(freeTalkSessionRepository.findByLearningSessionId(LEARNING_SESSION_ID))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service(ENDED_AT).getSummary(USER_ID, LEARNING_SESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);

    when(freeTalkSessionRepository.findByLearningSessionId(LEARNING_SESSION_ID))
        .thenReturn(Optional.of(session));
    when(session.getConversationStatus()).thenReturn(FreeTalkConversationStatus.IN_PROGRESS);
    assertThatThrownBy(() -> service(ENDED_AT).getSummary(USER_ID + 1, LEARNING_SESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @DisplayName("완료 세션인데 대화 기록이 없으면 404다.")
  @Test
  void rejectsCompletedSessionWithoutHistory() {
    when(sessionHistoryService.findByLearningSessionId(LEARNING_SESSION_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service(ENDED_AT).getSummary(USER_ID, LEARNING_SESSION_ID))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);
  }

  @DisplayName("상한이 지났지만 그사이 장기기억 작업이 끝났으면 확정은 0건이고, 다시 읽은 최신 상태로 후속 질문을 읽는다.")
  @Test
  void rereadsSessionWhenMemoryGenerationFinishedMeanwhile() {
    when(session.getMemoryGenerationStatus()).thenReturn(MemoryGenerationStatus.PREPARING);
    when(freeTalkSessionRepository.failStaleMemoryGeneration(eq(FREE_TALK_SESSION_ID), any()))
        .thenReturn(0);
    FreeTalkSession refreshed = mock(FreeTalkSession.class);
    when(refreshed.getId()).thenReturn(FREE_TALK_SESSION_ID);
    when(refreshed.getTitle()).thenReturn("카페 얘기");
    when(refreshed.getExpressionGenerationStatus()).thenReturn(ExpressionGenerationStatus.READY);
    when(refreshed.getMemoryGenerationStatus()).thenReturn(MemoryGenerationStatus.READY);
    when(freeTalkSessionRepository.findById(FREE_TALK_SESSION_ID))
        .thenReturn(Optional.of(refreshed));

    service(ENDED_AT.plusMinutes(6)).getSummary(USER_ID, LEARNING_SESSION_ID);

    verify(followUpService).findSummary(FREE_TALK_SESSION_ID, MemoryGenerationStatus.READY);
  }

  @DisplayName("표현 재사용과 후속 질문은 저장된 값을 필드 그대로 응답에 옮긴다.")
  @Test
  void passesReusedExpressionsAndFollowUpThrough() {
    when(expressionReuseQueryService.findSummary(
            FREE_TALK_SESSION_ID, ExpressionGenerationStatus.READY))
        .thenReturn(
            new FreeTalkExpressionReuseSummary(
                false,
                List.of(
                    new FreeTalkExpressionReuseSummary.Item(
                        812L,
                        "grab a coffee",
                        "커피 한잔하다",
                        "9월 10일 「주말 계획」",
                        5504L,
                        "Let's grab a coffee.",
                        "grab a coffee"))));
    when(followUpService.findSummary(FREE_TALK_SESSION_ID, MemoryGenerationStatus.READY))
        .thenReturn(
            new FreeTalkFollowUpSummary(
                false, FreeTalkFollowUpTriggerType.CONCERN, "면접은 어떻게 됐어?", "다음엔 그 얘기 하자."));

    FreeTalkSessionSummaryResponse response =
        service(ENDED_AT.plusSeconds(1)).getSummary(USER_ID, LEARNING_SESSION_ID);

    assertThat(response.reusedExpressions().pending()).isFalse();
    assertThat(response.reusedExpressions().items())
        .containsExactly(
            new FreeTalkSessionSummaryResponse.ReusedExpressions.Item(
                812L,
                "grab a coffee",
                "커피 한잔하다",
                "9월 10일 「주말 계획」",
                5504L,
                "Let's grab a coffee.",
                "grab a coffee"));
    assertThat(response.followUp())
        .isEqualTo(
            new FreeTalkSessionSummaryResponse.FollowUp(
                false, FreeTalkFollowUpTriggerType.CONCERN, "면접은 어떻게 됐어?", "다음엔 그 얘기 하자."));
  }

  @DisplayName("헤드라인과 비교는 항상 있고, 실수 기억 카드·표현 재사용·후속 질문은 각자 있을 때만 있으며 서로 영향을 주지 않는다.")
  @ParameterizedTest(name = "growth={0} reused={1} followUp={2}")
  @CsvSource({
    "false,false,false",
    "true,false,false",
    "false,true,false",
    "false,false,true",
    "true,true,false",
    "true,false,true",
    "false,true,true",
    "true,true,true",
  })
  void combinesOptionalBlocksIndependently(boolean growth, boolean reused, boolean followUp) {
    if (growth) {
      stubPreviousSessionWithTenseCard();
    }
    when(expressionReuseQueryService.findSummary(anyLong(), any()))
        .thenReturn(
            reused
                ? new FreeTalkExpressionReuseSummary(
                    false,
                    List.of(
                        new FreeTalkExpressionReuseSummary.Item(
                            812L,
                            "grab a coffee",
                            "커피 한잔하다",
                            "9월 10일",
                            5504L,
                            "Let's grab a coffee.",
                            "grab a coffee")))
                : new FreeTalkExpressionReuseSummary(false, List.of()));
    when(followUpService.findSummary(anyLong(), any()))
        .thenReturn(
            followUp
                ? new FreeTalkFollowUpSummary(
                    false, FreeTalkFollowUpTriggerType.HOBBY, "요즘도 등산 다녀?", "다음엔 산 얘기 하자.")
                : FreeTalkFollowUpSummary.none());

    FreeTalkSessionSummaryResponse response =
        service(ENDED_AT.plusSeconds(1)).getSummary(USER_ID, LEARNING_SESSION_ID);

    assertThat(response.pending()).isFalse();
    assertThat(response.headline()).isNotNull();
    assertThat(response.comparison()).isNotNull();
    assertThat(response.comparison().current().turnCount()).isEqualTo(2);
    assertThat(response.correctionCount()).isZero();
    assertThat(response.firstSession()).isEqualTo(!growth);
    assertThat(response.growth() != null).isEqualTo(growth);
    assertThat(response.reusedExpressions().items().isEmpty()).isEqualTo(!reused);
    assertThat(response.reusedExpressions().pending()).isFalse();
    assertThat(response.followUp().question() != null).isEqualTo(followUp);
    assertThat(response.followUp().pending()).isFalse();
  }

  // 직전 세션에 TENSE 교정이 있고 오늘 TENSE를 맞게 쓴 사용례가 있어 실수 기억 카드가 생기는 상황을 꾸민다.
  private void stubPreviousSessionWithTenseCard() {
    FreeTalkSession previousSession = mock(FreeTalkSession.class);
    when(previousSession.getLearningSessionId()).thenReturn(PREVIOUS_LEARNING_SESSION_ID);
    when(freeTalkSessionRepository.findPreviousCompleted(LEARNING_SESSION_ID, PageRequest.of(0, 1)))
        .thenReturn(List.of(previousSession));
    LearningSessionSnapshot previousLearning = mock(LearningSessionSnapshot.class);
    when(previousLearning.getEndedAt()).thenReturn(LocalDateTime.of(2026, 9, 10, 23, 50));
    when(learningSessionService.findSession(PREVIOUS_LEARNING_SESSION_ID))
        .thenReturn(Optional.of(previousLearning));
    SessionHistorySnapshot previousHistory = mock(SessionHistorySnapshot.class);
    when(previousHistory.getId()).thenReturn(PREVIOUS_HISTORY_ID);
    when(sessionHistoryService.findByLearningSessionId(PREVIOUS_LEARNING_SESSION_ID))
        .thenReturn(Optional.of(previousHistory));
    List<SessionHistoryMessageSnapshot> previousMessages =
        List.of(message(4504L, ConversationSpeaker.USER, "I go to gym.", 3000L));
    when(conversationMessageService.findAll(PREVIOUS_HISTORY_ID)).thenReturn(previousMessages);
    when(messageFeedbackService.findBySessionHistoryId(PREVIOUS_HISTORY_ID))
        .thenReturn(
            Map.of(
                4504L,
                FreeTalkTurnCorrection.completed(
                    new FreeTalkTurnCorrection.Sentence(
                        "I go to gym.",
                        "I went to the gym.",
                        "과거",
                        FreeTalkMistakePattern.TENSE,
                        "go",
                        "went"),
                    true)));
    when(patternUsageRepository.findBySessionHistoryIdOrderByIdAsc(HISTORY_ID))
        .thenReturn(
            List.of(
                FreeTalkPatternUsage.of(
                    5504L,
                    HISTORY_ID,
                    FreeTalkMistakePattern.TENSE,
                    "I went to the gym.",
                    "went",
                    true)));
  }

  private FreeTalkSummaryService service(LocalDateTime now) {
    return new FreeTalkSummaryService(
        learningSessionService,
        freeTalkSessionRepository,
        sessionHistoryService,
        conversationMessageService,
        messageFeedbackService,
        patternUsageRepository,
        summaryRepository,
        new FreeTalkSummaryCalculator(),
        expressionReuseQueryService,
        followUpService,
        mock(PlatformTransactionManager.class),
        Clock.fixed(now.atZone(SEOUL).toInstant(), SEOUL));
  }

  private FreeTalkSessionSummary storedSummary() {
    return new FreeTalkSummaryCalculator()
        .calculate(
            USER_ID,
            FREE_TALK_SESSION_ID,
            HISTORY_ID,
            // 지금 다시 계산하면 나올 값과 구별되도록 다른 세션 ID로 만든 문구를 저장된 총평으로 쓴다.
            LEARNING_SESSION_ID + 1,
            new FreeTalkSummarySource(
                List.of(
                    new FreeTalkSummarySource.Utterance("a", 1L),
                    new FreeTalkSummarySource.Utterance("b", 1L)),
                List.of(),
                List.of()),
            null,
            null);
  }

  private static SessionHistoryMessageSnapshot message(
      long id, ConversationSpeaker role, String content, Long durationMs) {
    SessionHistoryMessageSnapshot message = mock(SessionHistoryMessageSnapshot.class);
    when(message.getId()).thenReturn(id);
    when(message.getRole()).thenReturn(role);
    when(message.getContent()).thenReturn(content);
    when(message.getUtteranceDurationMs()).thenReturn(durationMs);
    return message;
  }
}
