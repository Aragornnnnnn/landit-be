// 프리톡 턴 교정의 준비·확정·조회 경계를 검증한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.config.session.FreeTalkCorrectionRetryProperties;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMessageFeedback;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.feedback.repository.FreeTalkMessageFeedbackRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** 프리톡 턴 교정의 준비·확정·조회 경계를 검증한다. */
class FreeTalkMessageFeedbackServiceTest {

  private static final LocalDateTime STALE_LEASE = LocalDateTime.of(2026, 9, 1, 0, 0);

  private final FreeTalkMessageFeedbackRepository repository =
      mock(FreeTalkMessageFeedbackRepository.class);
  private final ConversationMessageService conversationMessageService =
      mock(ConversationMessageService.class);
  private final AiClientProperties aiClientProperties = mock(AiClientProperties.class);
  // 서울 시간 2026-09-21 21:30:00
  private final Clock clock =
      Clock.fixed(Instant.parse("2026-09-21T12:30:00Z"), ZoneId.of("Asia/Seoul"));
  private final FreeTalkMessageFeedbackService service =
      new FreeTalkMessageFeedbackService(
          repository,
          conversationMessageService,
          aiClientProperties,
          new FreeTalkCorrectionRetryProperties(
              3, List.of(Duration.ZERO, Duration.ofMinutes(1), Duration.ofMinutes(5)), 10, true),
          new SimpleMeterRegistry(),
          clock);

  @BeforeEach
  void stubAiTimeout() {
    when(aiClientProperties.requestTimeout()).thenReturn(Duration.ofSeconds(60));
  }

  @DisplayName("교정 준비는 대화 기록 ID를 발화에서 읽어 준비 상태 행을 만든다.")
  @Test
  void preparesFeedbackRowWithHistoryIdReadFromMessage() {
    SessionHistoryMessageSnapshot message = mock(SessionHistoryMessageSnapshot.class);
    when(message.getSessionHistoryId()).thenReturn(3L);
    when(conversationMessageService.require(7L)).thenReturn(message);

    service.prepareCorrection(7L);

    ArgumentCaptor<FreeTalkMessageFeedback> saved =
        ArgumentCaptor.forClass(FreeTalkMessageFeedback.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getSessionHistoryMessageId()).isEqualTo(7L);
    assertThat(saved.getValue().getSessionHistoryId()).isEqualTo(3L);
    assertThat(saved.getValue().getProcessingStatus()).isEqualTo(ProcessingStatus.PREPARING);
    // 첫 시도로 세고, AI 응답을 기다려 줄 시각(요청 타임아웃 60초 + 여유 30초)을 서울 시간 Clock으로 적는다.
    assertThat(saved.getValue().getAttempts()).isEqualTo(1);
    assertThat(saved.getValue().getLeaseUntil())
        .isEqualTo(LocalDateTime.of(2026, 9, 21, 21, 31, 30));
    assertThat(saved.getValue().getAttemptToken()).isNull();
  }

  @DisplayName("교정 도입 전에 실패로 채워진 발화를 확정하면 새 행을 만들지 않고 그 행을 준비 상태로 되돌린다.")
  @Test
  void restartsBackfilledFailedFeedbackInsteadOfInsertingDuplicate() {
    FreeTalkMessageFeedback backfilled = FreeTalkMessageFeedback.preparing(7L, 3L, STALE_LEASE);
    ReflectionTestUtils.setField(backfilled, "processingStatus", ProcessingStatus.FAILED);
    ReflectionTestUtils.setField(backfilled, "reactedToPartner", Boolean.TRUE);
    ReflectionTestUtils.setField(backfilled, "attempts", 3);
    ReflectionTestUtils.setField(backfilled, "attemptToken", "old-token");
    when(repository.findBySessionHistoryMessageId(7L)).thenReturn(Optional.of(backfilled));

    service.prepareCorrection(7L);

    assertThat(backfilled.getProcessingStatus()).isEqualTo(ProcessingStatus.PREPARING);
    assertThat(backfilled.getReactedToPartner()).isNull();
    // 예전 시도의 흔적을 지우고 첫 시도부터 다시 센다.
    assertThat(backfilled.getAttempts()).isEqualTo(1);
    assertThat(backfilled.getLeaseUntil()).isEqualTo(LocalDateTime.of(2026, 9, 21, 21, 31, 30));
    assertThat(backfilled.getAttemptToken()).isNull();
    verify(repository, never()).save(any());
  }

  @DisplayName("이미 판정이 끝난 교정은 다시 준비해도 지우지 않는다.")
  @Test
  void keepsCompletedCorrectionWhenPreparedAgain() {
    FreeTalkMessageFeedback completed = FreeTalkMessageFeedback.preparing(7L, 3L, STALE_LEASE);
    ReflectionTestUtils.setField(completed, "processingStatus", ProcessingStatus.COMPLETED);
    ReflectionTestUtils.setField(completed, "betterSentence", "I went.");
    when(repository.findBySessionHistoryMessageId(7L)).thenReturn(Optional.of(completed));

    service.prepareCorrection(7L);

    assertThat(completed.getProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
    assertThat(completed.getBetterSentence()).isEqualTo("I went.");
    verify(repository, never()).save(any());
  }

  @DisplayName("교정 판정은 준비 상태 조건의 한 갱신으로 문장과 근거 기억 값을 함께 반영한다.")
  @Test
  void completesCorrectionInOneConditionalUpdate() {
    service.completeIfPreparing(
        7L,
        FreeTalkTurnCorrection.completed(
            new FreeTalkTurnCorrection.Sentence(
                "at a gym",
                "at the gym",
                "둘 다 아는 곳이에요.",
                FreeTalkMistakePattern.ARTICLE,
                42L,
                LocalDate.of(2026, 9, 13),
                "헬스장"),
            false));

    verify(repository)
        .updateIfPreparing(
            7L,
            ProcessingStatus.COMPLETED,
            false,
            "at a gym",
            "at the gym",
            "둘 다 아는 곳이에요.",
            FreeTalkMistakePattern.ARTICLE,
            42L,
            LocalDate.of(2026, 9, 13),
            "헬스장",
            ProcessingStatus.PREPARING);
  }

  @DisplayName("다시 해도 같을 실패(계약 위반)는 첫 시도에서 바로 실패로 확정한다.")
  @Test
  void failsContractViolationImmediately() {
    service.completeFirstAttempt(7L, FreeTalkTurnCorrection.failed());

    verify(repository)
        .updateIfPreparing(
            7L,
            ProcessingStatus.FAILED,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ProcessingStatus.PREPARING);
    verify(repository, never()).releaseForRetry(anyLong(), anyInt(), any(), any(), any());
  }

  @DisplayName("AI가 판정을 돌려주지 못한 첫 시도는 실패로 끝내지 않고 준비 상태로 둔 채 바로 다시 시도되게 한다.")
  @Test
  void keepsUnavailableFirstAttemptPreparingForImmediateRetry() {
    when(repository.releaseForRetry(anyLong(), anyInt(), any(), any(), any())).thenReturn(1);

    service.completeFirstAttempt(7L, FreeTalkTurnCorrection.unavailable());

    // 첫 시도는 선점 식별자가 없고, 첫 실패 뒤의 간격은 0초라 지금 시각(서울 21:30:00)이 다음 시도 시각이다.
    verify(repository)
        .releaseForRetry(
            7L, 1, "", LocalDateTime.of(2026, 9, 21, 21, 30, 0), ProcessingStatus.PREPARING);
    verify(repository, never())
        .updateIfPreparing(
            anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @DisplayName("AI 호출 자체가 실패한 첫 시도도 같은 방식으로 다시 시도되게 한다.")
  @Test
  void keepsFailedCallPreparingForRetry() {
    service.failFirstAttempt(7L);

    verify(repository)
        .releaseForRetry(
            7L, 1, "", LocalDateTime.of(2026, 9, 21, 21, 30, 0), ProcessingStatus.PREPARING);
  }

  @DisplayName("두 번째 시도가 실패하면 1분 뒤로 다음 시도를 미루고, 선점한 시도의 식별자가 같을 때만 반영한다.")
  @Test
  void schedulesThirdAttemptOneMinuteLater() {
    service.retryOrFail(7L, 2, "token-2");

    verify(repository)
        .releaseForRetry(
            7L, 2, "token-2", LocalDateTime.of(2026, 9, 21, 21, 31, 0), ProcessingStatus.PREPARING);
  }

  @DisplayName("마지막 시도까지 실패하면 그 시도의 식별자로 실패를 확정하고 더 미루지 않는다.")
  @Test
  void failsForGoodAfterLastAttempt() {
    service.retryOrFail(7L, 3, "token-3");

    verify(repository)
        .failAttempt(7L, 3, "token-3", ProcessingStatus.FAILED, ProcessingStatus.PREPARING);
    verify(repository, never()).releaseForRetry(anyLong(), anyInt(), any(), any(), any());
  }

  @DisplayName("근거 기억 값이 어긋난 행은 교정은 내려주되 근거 기억만 빼고 경고를 남긴다.")
  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void dropsOnlyInconsistentStoredMemoryWhenReading(CapturedOutput output) {
    FreeTalkMessageFeedback broken = FreeTalkMessageFeedback.preparing(7L, 3L, STALE_LEASE);
    ReflectionTestUtils.setField(broken, "processingStatus", ProcessingStatus.COMPLETED);
    ReflectionTestUtils.setField(broken, "originalSentence", "at a gym");
    ReflectionTestUtils.setField(broken, "betterSentence", "at the gym");
    ReflectionTestUtils.setField(broken, "reason", "둘 다 아는 곳이에요.");
    ReflectionTestUtils.setField(broken, "mistakePattern", FreeTalkMistakePattern.ARTICLE);
    ReflectionTestUtils.setField(broken, "memoryId", 42L);
    when(repository.findBySessionHistoryId(3L)).thenReturn(List.of(broken));

    FreeTalkTurnCorrection correction = service.findBySessionHistoryId(3L).get(7L);

    assertThat(correction.sentence().betterSentence()).isEqualTo("at the gym");
    assertThat(correction.sentence().usedMemoryId()).isNull();
    assertThat(correction.sentence().memoryObservedOn()).isNull();
    assertThat(output.getOut())
        .contains("reason=inconsistent_stored_memory")
        .contains("messageId=7");
  }

  @DisplayName("대화 기록의 교정을 사용자 발화 ID로 묶어 돌려준다.")
  @Test
  void groupsCorrectionsByUserMessageId() {
    when(repository.findBySessionHistoryId(3L))
        .thenReturn(List.of(FreeTalkMessageFeedback.preparing(7L, 3L, STALE_LEASE)));

    assertThat(service.findBySessionHistoryId(3L))
        .containsOnlyKeys(7L)
        .containsValue(new FreeTalkTurnCorrection(ProcessingStatus.PREPARING, null, null));
  }
}
