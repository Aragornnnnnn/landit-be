// LAN-531 리뷰에서 경계 결함을 재현한다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.learning.FreeTalkContextProperties;
import com.landit.landitbe.feature.learning.conversation.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkContextSummaryRequest;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkContextSummaryResult;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkSessionSummaryContent;
import com.landit.landitbe.feature.learning.freetalk.context.domain.FreeTalkContextSummary;
import com.landit.landitbe.feature.learning.freetalk.context.repository.FreeTalkContextSummaryRepository;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageReservation;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class FreeTalkContextSummaryServiceTest {
  final FreeTalkContextSummaryRepository repo = mock(FreeTalkContextSummaryRepository.class);
  final ConversationMessageService messages = mock(ConversationMessageService.class);
  final AiFreeTalkClient ai = mock(AiFreeTalkClient.class);
  final FreeTalkContextSummary state = FreeTalkContextSummary.start(30L, "v1", 6000);
  final FreeTalkContextSummaryService service;
  final FreeTalkContextLifecycleService lifecycle = mock(FreeTalkContextLifecycleService.class);
  final Instant now = Instant.parse("2026-09-20T00:00:00Z");

  FreeTalkContextSummaryServiceTest() {
    PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
    when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    var execution = mock(FreeTalkContextExecutionService.class);
    doAnswer(
            call -> {
              call.<Runnable>getArgument(0).run();
              return null;
            })
        .when(execution)
        .execute(any());
    service =
        new FreeTalkContextSummaryService(
            repo,
            messages,
            ai,
            new FreeTalkContextProperties(true, List.of(1L), 8, 12, 6000, 30, 30),
            tx,
            execution,
            lifecycle);
    when(lifecycle.lockActive(1L, 300L, 30L)).thenReturn(true);
    when(repo.currentTime()).thenReturn(now);
    when(repo.findByIdForUpdate(30L)).thenReturn(Optional.of(state));
  }

  List<SessionHistoryMessageSnapshot> rounds(int count, int size) {
    List<SessionHistoryMessageSnapshot> result = new ArrayList<>();
    for (int i = 1; i <= count * 2; i++) {
      SessionHistoryMessageSnapshot m = mock(SessionHistoryMessageSnapshot.class);
      when(m.getId()).thenReturn((long) i);
      when(m.getMessageSequence()).thenReturn(i);
      when(m.getTurnNumber()).thenReturn((i + 1) / 2);
      when(m.getRole()).thenReturn(i % 2 == 1 ? ConversationSpeaker.USER : ConversationSpeaker.AI);
      when(m.getFreeTalkTurnStatus()).thenReturn(i % 2 == 1 ? FreeTalkTurnStatus.CONTINUE : null);
      when(m.getContent()).thenReturn("x".repeat(size));
      when(m.getCreatedAt()).thenReturn(LocalDateTime.now());
      result.add(m);
    }
    return result;
  }

  @Test
  void waitsForTwelveUnsummarizedRounds() {
    var twelve = rounds(12, 10);
    var thirteen = rounds(13, 10);
    when(messages.findAll(3L)).thenReturn(twelve, thirteen);
    when(ai.generateContextSummary(any()))
        .thenAnswer(
            inv -> {
              AiFreeTalkContextSummaryRequest req = inv.getArgument(0);
              return new AiFreeTalkContextSummaryResult(
                  "v1",
                  req.baseRevision(),
                  req.targetThroughSequence(),
                  new AiFreeTalkSessionSummaryContent("topic", List.of(), List.of(), List.of()));
            });
    FreeTalkMessageReservation reservation = reservation();
    service.dispatchIfNeeded(reservation);
    assertEquals(8, state.getCoveredThroughSequence());
    service.dispatchIfNeeded(reservation);
    verify(ai, times(1)).generateContextSummary(any());
    assertEquals(8, state.getCoveredThroughSequence());
  }

  @Test
  void byteLimitPreservesMinimumCompletedPair() {
    List<SessionHistoryMessageSnapshot> all = rounds(12, 4000);
    List<SessionHistoryMessageSnapshot> source =
        ReflectionTestUtils.invokeMethod(service, "sourceMessages", all, state);
    assertEquals(2, source.size());
    assertEquals(ConversationSpeaker.AI, source.getLast().getRole());
  }

  @Test
  void doesNotEnrollExistingSessionOnDispatch() {
    var all = rounds(12, 10);
    when(messages.findAll(3L)).thenReturn(all);
    when(repo.findByIdForUpdate(30L)).thenReturn(Optional.empty());
    service.dispatchIfNeeded(reservation());
    verifyNoInteractions(ai);
    verify(repo, never()).save(any());
  }

  @Test
  void usesByteTriggerBeforeTwelveRounds() {
    var all = rounds(10, 1000);
    when(messages.findAll(3L)).thenReturn(all);
    when(ai.generateContextSummary(any())).thenAnswer(call -> result(call.getArgument(0)));
    service.dispatchIfNeeded(reservation());
    assertEquals(4, state.getCoveredThroughSequence());
  }

  @Test
  void registeredSessionHasPolicyBeforeFirstSummary() {
    when(repo.findById(30L)).thenReturn(Optional.of(state));
    assertEquals("v1", service.snapshot(1L, 30L).contextPolicyVersion());
    assertNull(service.snapshot(1L, 30L).sessionSummary());
  }

  private AiFreeTalkContextSummaryResult result(AiFreeTalkContextSummaryRequest request) {
    return new AiFreeTalkContextSummaryResult(
        "v1",
        request.baseRevision(),
        request.targetThroughSequence(),
        new AiFreeTalkSessionSummaryContent("topic", List.of(), List.of(), List.of()));
  }

  private FreeTalkMessageReservation reservation() {
    return new FreeTalkMessageReservation(
        1L,
        LocalDate.now(),
        300L,
        30L,
        "chloe",
        3L,
        24L,
        "client-id",
        1000L,
        false,
        false,
        "EN",
        "KO",
        null,
        List.of());
  }
}
