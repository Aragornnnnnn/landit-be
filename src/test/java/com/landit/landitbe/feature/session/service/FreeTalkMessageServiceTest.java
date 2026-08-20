// 프리톡 AI 호출 이후 속마음 저장 실패 처리를 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkInnerThoughtResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionMessageInputType;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse.NextMessageResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse.ProgressResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse.SubmittedMessageResponse;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.task.TaskExecutor;

/** 프리톡 발화 이후 속마음 저장 실패 처리를 검증한다. */
class FreeTalkMessageServiceTest {

  private final FreeTalkSubmittedMessageService submittedMessageService =
      mock(FreeTalkSubmittedMessageService.class);
  private final AiFreeTalkClient aiFreeTalkClient = mock(AiFreeTalkClient.class);
  private final SessionMessageService sessionMessageService = mock(SessionMessageService.class);
  private final FreeTalkExpressionGenerationDispatcher expressionGenerationDispatcher =
      mock(FreeTalkExpressionGenerationDispatcher.class);
  private final TaskExecutor directExecutor = Runnable::run;
  private final FreeTalkMessageService service =
      new FreeTalkMessageService(
          submittedMessageService,
          aiFreeTalkClient,
          sessionMessageService,
          directExecutor,
          expressionGenerationDispatcher);

  @Test
  void marksInnerThoughtFailedWhenPersistingCompletedThoughtFails() {
    FreeTalkSubmittedMessageService.Reservation reservation = reservation();
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation);
    when(aiFreeTalkClient.generateTurn(any()))
        .thenReturn(
            new AiFreeTalkTurnResult(
                false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY));
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());
    when(aiFreeTalkClient.generateInnerThought(any()))
        .thenReturn(new AiFreeTalkInnerThoughtResult("즐거웠나 보다.", InnerThoughtType.GOOD));
    doThrow(new IllegalStateException("save failed"))
        .when(sessionMessageService)
        .completeInnerThought(7L, "즐거웠나 보다.", InnerThoughtType.GOOD);

    service.submit(1L, 300L, request());

    verify(sessionMessageService).failInnerThought(7L);
    verify(aiFreeTalkClient)
        .generateTurn(argThat(request -> request.characterId().equals("chloe")));
    verify(aiFreeTalkClient)
        .generateInnerThought(argThat(request -> request.characterId().equals("chloe")));
  }

  @Test
  void startsInnerThoughtBeforeGeneratingTurn() {
    TaskExecutor taskExecutor = mock(TaskExecutor.class);
    final FreeTalkMessageService concurrentService = service(taskExecutor);
    FreeTalkSubmittedMessageService.Reservation reservation = reservation();
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation);
    when(aiFreeTalkClient.generateTurn(any()))
        .thenReturn(
            new AiFreeTalkTurnResult(
                false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY));
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());

    concurrentService.submit(1L, 300L, request());

    InOrder invocationOrder = inOrder(taskExecutor, aiFreeTalkClient);
    invocationOrder.verify(taskExecutor).execute(any(Runnable.class));
    invocationOrder.verify(aiFreeTalkClient).generateTurn(any());
  }

  @Test
  void keepsCoreResponseWhenInnerThoughtExecutorRejectsTask() {
    TaskExecutor rejectingExecutor =
        task -> {
          throw new RejectedExecutionException("executor full");
        };
    final FreeTalkMessageService concurrentService = service(rejectingExecutor);
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation());
    when(aiFreeTalkClient.generateTurn(any()))
        .thenReturn(
            new AiFreeTalkTurnResult(
                false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY));
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());

    concurrentService.submit(1L, 300L, request());

    verify(aiFreeTalkClient).generateTurn(any());
    verify(submittedMessageService).finalizeTurn(any(), any());
    verify(sessionMessageService).failInnerThought(7L);
  }

  @Test
  void cancelsSpeculativeInnerThoughtWhenExitIntentIsDetected() throws InterruptedException {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    CountDownLatch innerThoughtStarted = new CountDownLatch(1);
    CountDownLatch innerThoughtInterrupted = new CountDownLatch(1);
    try {
      final FreeTalkMessageService concurrentService = service(executor::execute);
      when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
          .thenReturn(reservation());
      when(aiFreeTalkClient.generateInnerThought(any()))
          .thenAnswer(
              invocation -> {
                innerThoughtStarted.countDown();
                try {
                  new CountDownLatch(1).await();
                  return null;
                } catch (InterruptedException exception) {
                  innerThoughtInterrupted.countDown();
                  Thread.currentThread().interrupt();
                  throw exception;
                }
              });
      when(aiFreeTalkClient.generateTurn(any()))
          .thenAnswer(
              invocation -> {
                assertTrue(innerThoughtStarted.await(1, TimeUnit.SECONDS));
                return new AiFreeTalkTurnResult(true, null, null, null, null);
              });
      when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(exitResponse());

      concurrentService.submit(1L, 300L, request());

      assertTrue(innerThoughtInterrupted.await(1, TimeUnit.SECONDS));
    } finally {
      executor.shutdownNow();
    }
  }

  private FreeTalkMessageService service(TaskExecutor taskExecutor) {
    return new FreeTalkMessageService(
        submittedMessageService,
        aiFreeTalkClient,
        sessionMessageService,
        taskExecutor,
        expressionGenerationDispatcher);
  }

  private FreeTalkSubmittedMessageService.Reservation reservation() {
    return new FreeTalkSubmittedMessageService.Reservation(
        1L,
        java.time.LocalDate.now(),
        300L,
        30L,
        "chloe",
        3L,
        7L,
        "9d6928d0-0cbc-4cb1-a9cf-2f91c1f9c0ec",
        1200L,
        false,
        true,
        "EN",
        "KO",
        new AiFreeTalkTopic(null, "하이킹", null),
        List.of(new AiConversationHistoryMessage(7L, 1, "USER", "I went hiking.", null)));
  }

  private FreeTalkMessageSubmitRequest request() {
    return new FreeTalkMessageSubmitRequest(
        "9d6928d0-0cbc-4cb1-a9cf-2f91c1f9c0ec",
        "I went hiking.",
        SessionMessageInputType.VOICE,
        1200L,
        false);
  }

  private FreeTalkMessageSubmitResponse continueResponse() {
    return new FreeTalkMessageSubmitResponse(
        300L,
        "하이킹",
        FreeTalkTurnStatus.CONTINUE,
        new SubmittedMessageResponse(7L, 1, 1, "USER", null, null, ProcessingStatus.PREPARING),
        new NextMessageResponse(
            8L, 2, 2, "AI", "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY),
        new ProgressResponse(
            FreeTalkConversationStatus.IN_PROGRESS, 1200L, 60000L, 1200L, 58800L, null));
  }

  private FreeTalkMessageSubmitResponse exitResponse() {
    return new FreeTalkMessageSubmitResponse(
        300L,
        "하이킹",
        FreeTalkTurnStatus.EXIT_CONFIRMATION_REQUIRED,
        new SubmittedMessageResponse(7L, 1, 1, "USER", null, null, null),
        null,
        new ProgressResponse(
            FreeTalkConversationStatus.AWAITING_EXIT_DECISION, 1200L, 60000L, 1200L, 58800L, null));
  }
}
