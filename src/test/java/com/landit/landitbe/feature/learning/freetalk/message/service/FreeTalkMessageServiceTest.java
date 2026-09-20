// 프리톡 AI 호출 이후 속마음 저장 실패 처리를 검증한다.

package com.landit.landitbe.feature.learning.freetalk.message.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.domain.CharacterEmotion;
import com.landit.landitbe.feature.learning.conversation.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.domain.SessionMessageInputType;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.learning.freetalk.expression.service.FreeTalkExpressionGenerationDispatcher;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtResult;
import com.landit.landitbe.feature.learning.freetalk.memory.service.FreeTalkMemoryGenerationDispatchService;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkExitDecisionReservation;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageReservation;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitResponse.NextMessageResponse;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitResponse.ProgressResponse;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitResponse.SubmittedMessageResponse;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.retrieval.domain.MemoryRetrievalStage;
import com.landit.landitbe.feature.memory.retrieval.dto.MemoryRetrievalResult;
import com.landit.landitbe.feature.memory.retrieval.service.FreeTalkMemoryRetrievalService;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;

/** 프리톡 발화 이후 속마음 저장 실패 처리를 검증한다. */
class FreeTalkMessageServiceTest {
  private final FreeTalkMessageReplayService replayService =
      mock(FreeTalkMessageReplayService.class);

  private final FreeTalkSubmittedMessageService submittedMessageService =
      mock(FreeTalkSubmittedMessageService.class);
  private final AiFreeTalkClient aiFreeTalkClient = mock(AiFreeTalkClient.class);
  private final FreeTalkTurnResultService turnResultService = mock(FreeTalkTurnResultService.class);
  private final FreeTalkExpressionGenerationDispatcher expressionGenerationDispatcher =
      mock(FreeTalkExpressionGenerationDispatcher.class);
  private final FreeTalkMemoryGenerationDispatchService memoryGenerationDispatchService =
      mock(FreeTalkMemoryGenerationDispatchService.class);
  private final FreeTalkMemoryRetrievalService memoryRetrievalService =
      mock(FreeTalkMemoryRetrievalService.class);
  private final TaskExecutor directExecutor = Runnable::run;
  private final FreeTalkMessageService service =
      new FreeTalkMessageService(
          submittedMessageService,
          replayService,
          aiFreeTalkClient,
          turnResultService,
          directExecutor,
          expressionGenerationDispatcher,
          memoryGenerationDispatchService,
          memoryRetrievalService);

  @DisplayName("사용자의 첫 발화에서만 기억을 조회하고 응답에서 사용한 기억을 기록한다.")
  @Test
  void retrievesMemoryOnlyForTheFirstUserTurnAndRecordsUsedResponse() {
    FreeTalkMessageReservation reservation = reservation();
    MemoryRetrievalResult memoryResult =
        new MemoryRetrievalResult(
            30L,
            MemoryRetrievalStage.FIRST_USER_TURN,
            List.of(new AiFreeTalkMemoryContext(11L, ConversationMemoryType.EVENT, "hiking")),
            true);
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation);
    when(memoryRetrievalService.retrieve(any())).thenReturn(memoryResult);
    when(aiFreeTalkClient.generateTurn(any()))
        .thenReturn(
            new AiFreeTalkTurnResult(
                false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY, List.of(11L)));
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());

    service.submit(1L, 300L, request());

    verify(memoryRetrievalService)
        .retrieve(
            argThat(
                request ->
                    request.sessionId() == 30L
                        && request.userProfileId() == 1L
                        && request.stage() == MemoryRetrievalStage.FIRST_USER_TURN
                        && request.query().equals("I went hiking.")));
    verify(aiFreeTalkClient)
        .generateTurn(
            argThat(
                request ->
                    request.isFirstUserTurn()
                        && request.memoryContext().stream()
                            .map(AiFreeTalkMemoryContext::memoryId)
                            .toList()
                            .equals(List.of(11L))));
    verify(memoryRetrievalService).recordUsage(memoryResult, List.of(11L), 8L);
  }

  @DisplayName("AI가 기억 사용 메타데이터를 거부해도 같은 발화를 재시도하지 않는다.")
  @Test
  void doesNotRetryTurnWhenAiRejectsMemoryUsageMetadata() {
    MemoryRetrievalResult memoryResult =
        new MemoryRetrievalResult(
            30L,
            MemoryRetrievalStage.FIRST_USER_TURN,
            List.of(new AiFreeTalkMemoryContext(11L, ConversationMemoryType.EVENT, "hiking")),
            true);
    ApiException exception = new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation());
    when(memoryRetrievalService.retrieve(any())).thenReturn(memoryResult);
    when(aiFreeTalkClient.generateTurn(any())).thenThrow(exception);

    assertThatThrownBy(() -> service.submit(1L, 300L, request())).isSameAs(exception);

    verify(aiFreeTalkClient, times(1)).generateTurn(any());
  }

  @DisplayName("완료된 속마음의 저장이 실패하면 속마음을 실패 상태로 바꾼다.")
  @Test
  void marksInnerThoughtFailedWhenPersistingCompletedThoughtFails() {
    FreeTalkMessageReservation reservation = reservation();
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation);
    when(aiFreeTalkClient.generateTurn(any()))
        .thenReturn(
            new AiFreeTalkTurnResult(
                false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY, List.of()));
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());
    when(aiFreeTalkClient.generateInnerThought(any()))
        .thenReturn(
            new AiFreeTalkInnerThoughtResult(
                "즐거웠나 보다.", InnerThoughtType.GOOD, FreeTalkTurnCorrection.failed()));
    doThrow(new IllegalStateException("save failed"))
        .when(turnResultService)
        .complete(7L, "즐거웠나 보다.", InnerThoughtType.GOOD, FreeTalkTurnCorrection.failed());

    service.submit(1L, 300L, request());

    verify(turnResultService).fail(7L);
    verify(aiFreeTalkClient)
        .generateTurn(argThat(request -> request.characterId().equals("chloe")));
    verify(aiFreeTalkClient)
        .generateInnerThought(argThat(request -> request.characterId().equals("chloe")));
  }

  @DisplayName("속마음과 같은 응답에 실려 온 턴 교정을 한 번의 저장으로 함께 넘긴다.")
  @Test
  void persistsTurnCorrectionTogetherWithInnerThought() {
    FreeTalkTurnCorrection correction =
        FreeTalkTurnCorrection.completed(
            new FreeTalkTurnCorrection.Sentence(
                "I go hiking.", "I went hiking.", "과거 일이에요.", FreeTalkMistakePattern.TENSE),
            true);
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation());
    when(aiFreeTalkClient.generateTurn(any()))
        .thenReturn(
            new AiFreeTalkTurnResult(
                false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY, List.of()));
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());
    when(aiFreeTalkClient.generateInnerThought(any()))
        .thenReturn(
            new AiFreeTalkInnerThoughtResult("즐거웠나 보다.", InnerThoughtType.GOOD, correction));

    service.submit(1L, 300L, request());

    verify(turnResultService).complete(7L, "즐거웠나 보다.", InnerThoughtType.GOOD, correction);
  }

  @DisplayName("일반 턴의 속마음 요청에 세션에서 이미 검색한 기억을 새 검색 없이 싣는다.")
  @Test
  void sendsAlreadyRetrievedMemoriesWithInnerThoughtRequest() {
    List<AiFreeTalkMemoryContext> retrieved =
        List.of(new AiFreeTalkMemoryContext(42L, ConversationMemoryType.PROFILE, "집 앞 헬스장에 다닌다."));
    when(memoryRetrievalService.retrievedContexts(30L, 1L)).thenReturn(retrieved);
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation());
    when(aiFreeTalkClient.generateTurn(any())).thenReturn(turnResult());
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());
    when(aiFreeTalkClient.generateInnerThought(any()))
        .thenReturn(
            new AiFreeTalkInnerThoughtResult(
                "즐거웠나 보다.", InnerThoughtType.GOOD, FreeTalkTurnCorrection.completed(null, true)));

    service.submit(1L, 300L, request());

    verify(aiFreeTalkClient)
        .generateInnerThought(argThat(request -> request.memoryContext().equals(retrieved)));
  }

  @DisplayName("속마음 요청의 기억은 이 턴의 기억 검색보다 먼저 읽는다. 사용자가 먼저 말을 건 세션의 첫 턴은 빈 문맥으로 나간다.")
  @Test
  void readsCorrectionMemoryBeforeTheTurnRetrievesItsOwn() {
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation());
    when(memoryRetrievalService.retrieve(any()))
        .thenReturn(
            new MemoryRetrievalResult(
                30L,
                MemoryRetrievalStage.FIRST_USER_TURN,
                List.of(new AiFreeTalkMemoryContext(11L, ConversationMemoryType.EVENT, "hiking")),
                true));
    when(aiFreeTalkClient.generateTurn(any())).thenReturn(turnResult());
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());
    when(aiFreeTalkClient.generateInnerThought(any()))
        .thenReturn(
            new AiFreeTalkInnerThoughtResult(
                "즐거웠나 보다.", InnerThoughtType.GOOD, FreeTalkTurnCorrection.completed(null, true)));

    service.submit(1L, 300L, request());

    org.mockito.InOrder order = org.mockito.Mockito.inOrder(memoryRetrievalService);
    order.verify(memoryRetrievalService).retrievedContexts(30L, 1L);
    order.verify(memoryRetrievalService).retrieve(any());
    verify(aiFreeTalkClient)
        .generateInnerThought(argThat(request -> request.memoryContext().isEmpty()));
  }

  @DisplayName("기억 문맥을 읽다 실패해도 예약을 되돌린다.")
  @Test
  void compensatesReservationWhenReadingCorrectionMemoryFails() {
    FreeTalkMessageReservation reservation = reservation();
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation);
    when(memoryRetrievalService.retrievedContexts(30L, 1L))
        .thenThrow(new IllegalStateException("unexpected"));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.submit(1L, 300L, request()))
        .isInstanceOf(IllegalStateException.class);

    verify(submittedMessageService).compensate(reservation);
  }

  @DisplayName("종료 선택 뒤의 속마음 요청에도 세션에서 이미 검색한 기억을 싣는다.")
  @Test
  void sendsAlreadyRetrievedMemoriesWithInnerThoughtRequestAfterExitDecision() {
    List<AiFreeTalkMemoryContext> retrieved =
        List.of(new AiFreeTalkMemoryContext(42L, ConversationMemoryType.PROFILE, "집 앞 헬스장에 다닌다."));
    when(memoryRetrievalService.retrievedContexts(30L, 1L)).thenReturn(retrieved);
    when(submittedMessageService.reserveDecision(
            any(Long.class), any(Long.class), any(Long.class), any()))
        .thenReturn(decisionReservation());
    when(aiFreeTalkClient.generateClosing(any())).thenReturn(closingResult());
    when(submittedMessageService.finalizeEnd(any(), any())).thenReturn(completedResponse());

    service.decideExit(1L, 300L, new FreeTalkExitDecisionRequest(7L, FreeTalkExitDecision.END));

    verify(aiFreeTalkClient)
        .generateInnerThought(argThat(request -> request.memoryContext().equals(retrieved)));
    verify(memoryRetrievalService, org.mockito.Mockito.never()).retrieve(any());
  }

  @DisplayName("속마음 생성 실패를 구조화된 오류 로그로 기록한다.")
  @Test
  void logsFailedInnerThoughtGenerationAsStructuredError() {
    Logger logger = (Logger) LoggerFactory.getLogger(FreeTalkMessageService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
          .thenReturn(reservation());
      when(aiFreeTalkClient.generateTurn(any()))
          .thenReturn(
              new AiFreeTalkTurnResult(
                  false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY, List.of()));
      when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());
      when(aiFreeTalkClient.generateInnerThought(any()))
          .thenThrow(new ApiException(ErrorCode.AI_RESPONSE_INVALID));

      service.submit(1L, 300L, request());

      assertThat(appender.list)
          .anySatisfy(
              event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage())
                    .contains("workflow=free_talk_inner_thought_failed")
                    .contains("messageId=7")
                    .contains("errorCode=AI_RESPONSE_INVALID");
                assertThat(event.getThrowableProxy()).isNotNull();
              });
      verify(turnResultService).fail(7L);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @DisplayName("프리톡 발화 생성 전에 속마음 생성을 시작한다.")
  @Test
  void startsInnerThoughtBeforeGeneratingTurn() {
    TaskExecutor taskExecutor = mock(TaskExecutor.class);
    final FreeTalkMessageService concurrentService = service(taskExecutor);
    FreeTalkMessageReservation reservation = reservation();
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation);
    when(aiFreeTalkClient.generateTurn(any()))
        .thenReturn(
            new AiFreeTalkTurnResult(
                false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY, List.of()));
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());

    concurrentService.submit(1L, 300L, request());

    InOrder invocationOrder = inOrder(taskExecutor, aiFreeTalkClient);
    invocationOrder.verify(taskExecutor).execute(any(Runnable.class));
    invocationOrder.verify(aiFreeTalkClient).generateTurn(any());
  }

  @DisplayName("속마음 실행기가 작업을 거부해도 핵심 대화 응답을 유지한다.")
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
                false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY, List.of()));
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());

    concurrentService.submit(1L, 300L, request());

    verify(aiFreeTalkClient).generateTurn(any());
    verify(submittedMessageService).finalizeTurn(any(), any());
    verify(turnResultService).fail(7L);
  }

  /** 완료 응답이 트랜잭션 확정 뒤에만 기억 생성 dispatcher로 전달되는지 확인한다. */
  @DisplayName("완료 응답이 트랜잭션 확정 뒤에만 기억 생성 dispatcher로 전달되는지 확인한다.")
  @Test
  void dispatchesMemoryGenerationAfterNewlyCompletedResponse() {
    FreeTalkMessageReservation reservation = timeLimitReservation();
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation);
    when(aiFreeTalkClient.generateClosing(any())).thenReturn(closingResult());
    when(submittedMessageService.finalizeTimeLimit(any(), any())).thenReturn(completedResponse());

    service.submit(1L, 300L, request());

    InOrder invocationOrder = inOrder(submittedMessageService, memoryGenerationDispatchService);
    invocationOrder.verify(submittedMessageService).finalizeTimeLimit(any(), any());
    invocationOrder.verify(memoryGenerationDispatchService).dispatch(300L);
  }

  /** 완료되지 않은 응답은 기억 생성 dispatcher로 전달하지 않는다. */
  @DisplayName("완료되지 않은 응답은 기억 생성 dispatcher로 전달하지 않는다.")
  @Test
  void doesNotDispatchMemoryGenerationForNonCompletedResponse() {
    when(submittedMessageService.reserve(any(Long.class), any(Long.class), any()))
        .thenReturn(reservation());
    when(aiFreeTalkClient.generateTurn(any())).thenReturn(turnResult());
    when(submittedMessageService.finalizeTurn(any(), any())).thenReturn(continueResponse());

    service.submit(1L, 300L, request());

    verify(memoryGenerationDispatchService, org.mockito.Mockito.never()).dispatch(any(Long.class));
  }

  /** 이미 저장된 완료 응답을 재생할 때 기억 생성 dispatcher를 중복 호출하지 않는다. */
  @DisplayName("이미 저장된 완료 응답을 재생할 때 기억 생성 dispatcher를 중복 호출하지 않는다.")
  @Test
  void doesNotDispatchMemoryGenerationForReplayedResponse() {
    when(replayService.findCompletedResponse(any(Long.class), any(Long.class), any()))
        .thenReturn(completedResponse());

    service.submit(1L, 300L, request());

    verify(memoryGenerationDispatchService, org.mockito.Mockito.never()).dispatch(any(Long.class));
    verify(submittedMessageService, org.mockito.Mockito.never()).finalizeTimeLimit(any(), any());
  }

  /** 종료 확정으로 새로 완료된 응답도 기억 생성 dispatcher로 전달한다. */
  @DisplayName("종료 확정으로 새로 완료된 응답도 기억 생성 dispatcher로 전달한다.")
  @Test
  void dispatchesMemoryGenerationAfterUserConfirmedCompletion() {
    FreeTalkExitDecisionReservation reservation = decisionReservation();
    when(submittedMessageService.reserveDecision(
            any(Long.class), any(Long.class), any(Long.class), any()))
        .thenReturn(reservation);
    when(aiFreeTalkClient.generateClosing(any())).thenReturn(closingResult());
    when(submittedMessageService.finalizeEnd(any(), any())).thenReturn(completedResponse());

    service.decideExit(1L, 300L, new FreeTalkExitDecisionRequest(7L, FreeTalkExitDecision.END));

    verify(memoryGenerationDispatchService).dispatch(300L);
    verify(memoryRetrievalService, org.mockito.Mockito.never()).retrieve(any());
  }

  @DisplayName("종료 의도를 감지하면 미리 시작한 속마음 생성을 취소한다.")
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
                return new AiFreeTalkTurnResult(true, null, null, null, null, List.of());
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
        replayService,
        aiFreeTalkClient,
        turnResultService,
        taskExecutor,
        expressionGenerationDispatcher,
        memoryGenerationDispatchService,
        memoryRetrievalService);
  }

  private FreeTalkMessageReservation reservation() {
    return new FreeTalkMessageReservation(
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

  private FreeTalkMessageReservation timeLimitReservation() {
    return new FreeTalkMessageReservation(
        1L,
        java.time.LocalDate.now(),
        300L,
        30L,
        "chloe",
        3L,
        7L,
        "9d6928d0-0cbc-4cb1-a9cf-2f91c1f9c0ec",
        1200L,
        true,
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

  private AiFreeTalkTurnResult turnResult() {
    return new AiFreeTalkTurnResult(
        false, null, "That sounds fun!", "재밌겠다!", CharacterEmotion.HAPPY, List.of());
  }

  private AiFreeTalkClosingResult closingResult() {
    return new AiFreeTalkClosingResult("하이킹", "See you!", "또 봐요!", CharacterEmotion.HAPPY);
  }

  private FreeTalkExitDecisionReservation decisionReservation() {
    return new FreeTalkExitDecisionReservation(
        1L,
        300L,
        3L,
        30L,
        "chloe",
        7L,
        FreeTalkExitDecision.END,
        true,
        "EN",
        "KO",
        new AiFreeTalkTopic(null, "하이킹", null),
        List.of(new AiConversationHistoryMessage(7L, 1, "USER", "I went hiking.", null)));
  }

  private FreeTalkMessageSubmitResponse completedResponse() {
    return new FreeTalkMessageSubmitResponse(
        300L,
        "하이킹",
        FreeTalkTurnStatus.COMPLETED,
        new SubmittedMessageResponse(7L, 1, 1, "USER", null, null, ProcessingStatus.PREPARING),
        new NextMessageResponse(8L, 2, 2, "AI", "See you!", "또 봐요!", CharacterEmotion.HAPPY),
        new ProgressResponse(
            FreeTalkConversationStatus.COMPLETED, 1200L, 60000L, 1200L, 58800L, null));
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
