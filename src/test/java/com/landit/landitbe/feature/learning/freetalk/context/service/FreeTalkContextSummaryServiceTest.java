// LAN-531 리뷰에서 경계 결함을 재현한다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.learning.FreeTalkContextProperties;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.context.domain.FreeTalkContextSummary;
import com.landit.landitbe.feature.learning.freetalk.context.repository.FreeTalkContextSummaryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
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

  @Test
  void registeredSessionHasPolicyBeforeFirstSummary() {
    when(repo.findById(30L)).thenReturn(Optional.of(state));
    assertEquals("v1", service.snapshot(1L, 30L).contextPolicyVersion());
    assertNull(service.snapshot(1L, 30L).sessionSummary());
  }

}
