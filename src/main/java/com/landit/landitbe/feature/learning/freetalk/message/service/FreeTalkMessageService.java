// 프리톡 발화와 종료 확인의 AI 호출 흐름을 조정한다.

package com.landit.landitbe.feature.learning.freetalk.message.service;

import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkResponseMode;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkContextWindow;
import com.landit.landitbe.feature.learning.freetalk.context.service.FreeTalkContextSummaryService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.learning.freetalk.expression.service.FreeTalkExpressionGenerationDispatcher;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtResult;
import com.landit.landitbe.feature.learning.freetalk.memory.service.FreeTalkMemoryGenerationDispatchService;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingReason;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingRequest;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkExitDecisionReservation;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageReservation;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.retrieval.domain.MemoryRetrievalStage;
import com.landit.landitbe.feature.memory.retrieval.dto.MemoryRetrievalRequest;
import com.landit.landitbe.feature.memory.retrieval.dto.MemoryRetrievalResult;
import com.landit.landitbe.feature.memory.retrieval.service.FreeTalkMemoryRetrievalService;
import com.landit.landitbe.shared.exception.ApiException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/** 프리톡 발화와 종료 확인의 AI 호출 흐름을 조정한다. */
@Service
@Slf4j
public class FreeTalkMessageService {

  private final FreeTalkSubmittedMessageService submittedMessageService;
  private final FreeTalkMessageReplayService replayService;
  private final AiFreeTalkClient aiFreeTalkClient;
  private final ConversationMessageService conversationMessageService;
  private final TaskExecutor taskExecutor;
  private final FreeTalkExpressionGenerationDispatcher expressionGenerationDispatcher;
  private final FreeTalkMemoryGenerationDispatchService memoryGenerationDispatchService;
  private final FreeTalkMemoryRetrievalService memoryRetrievalService;
  private final FreeTalkContextSummaryService contextSummaryService;

  @Autowired
  FreeTalkMessageService(
      FreeTalkSubmittedMessageService submittedMessageService,
      FreeTalkMessageReplayService replayService,
      AiFreeTalkClient aiFreeTalkClient,
      ConversationMessageService conversationMessageService,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
      FreeTalkExpressionGenerationDispatcher expressionGenerationDispatcher,
      FreeTalkMemoryGenerationDispatchService memoryGenerationDispatchService,
      FreeTalkMemoryRetrievalService memoryRetrievalService,
      FreeTalkContextSummaryService contextSummaryService) {
    this.submittedMessageService = submittedMessageService;
    this.replayService = replayService;
    this.aiFreeTalkClient = aiFreeTalkClient;
    this.conversationMessageService = conversationMessageService;
    this.taskExecutor = taskExecutor;
    this.expressionGenerationDispatcher = expressionGenerationDispatcher;
    this.memoryGenerationDispatchService = memoryGenerationDispatchService;
    this.memoryRetrievalService = memoryRetrievalService;
    this.contextSummaryService = contextSummaryService;
  }

  /** 기존 단위 테스트와 로컬 조합을 위한 컨텍스트 비활성 생성자다. */
  FreeTalkMessageService(
      FreeTalkSubmittedMessageService submittedMessageService,
      FreeTalkMessageReplayService replayService,
      AiFreeTalkClient aiFreeTalkClient,
      ConversationMessageService conversationMessageService,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
      FreeTalkExpressionGenerationDispatcher expressionGenerationDispatcher,
      FreeTalkMemoryGenerationDispatchService memoryGenerationDispatchService,
      FreeTalkMemoryRetrievalService memoryRetrievalService) {
    this(
        submittedMessageService,
        replayService,
        aiFreeTalkClient,
        conversationMessageService,
        taskExecutor,
        expressionGenerationDispatcher,
        memoryGenerationDispatchService,
        memoryRetrievalService,
        null);
  }

  /**
   * 사용자 발화를 처리하고 AI 후속 메시지 또는 종료 확인 상태를 반환한다.
   *
   * @param userId 요청 사용자 ID
   * @param learningSessionId 프리톡 학습 세션 ID
   * @param request 사용자 발화 요청
   * @return 저장된 사용자 발화와 AI 처리 결과
   * @throws com.landit.landitbe.shared.exception.ApiException 세션 접근, 처리 상태 또는 AI 생성에 실패할 때
   * @throws com.landit.landitbe.feature.learning.conversation.exception.SessionException 프리톡 이용 한도에
   *     도달했을 때
   */
  public FreeTalkMessageSubmitResponse submit(
      long userId, long learningSessionId, FreeTalkMessageSubmitRequest request) {
    FreeTalkMessageSubmitResponse replayedResponse =
        replayService.findCompletedResponse(userId, learningSessionId, request);
    if (replayedResponse != null) {
      return replayedResponse;
    }
    FreeTalkMessageReservation reservation =
        submittedMessageService.reserve(userId, learningSessionId, request);
    CompletableFuture<AiFreeTalkInnerThoughtResult> innerThoughtFuture = null;
    try {
      AiFreeTalkInnerThoughtRequest innerThoughtRequest = innerThoughtRequest(reservation);
      innerThoughtFuture = startInnerThought(innerThoughtRequest);
      FreeTalkMessageSubmitResponse response;
      if (reservation.dailyLimitReached()) {
        response =
            submittedMessageService.finalizeTimeLimit(
                reservation,
                aiFreeTalkClient.generateClosing(
                    closingRequest(reservation, AiFreeTalkClosingReason.TIME_LIMIT_REACHED)));
      } else {
        response = processRegularTurn(reservation);
      }
      if (response.turnStatus()
          != com.landit.landitbe.feature.learning.conversation.domain.FreeTalkTurnStatus
              .EXIT_CONFIRMATION_REQUIRED) {
        recordInnerThought(innerThoughtRequest, innerThoughtFuture);
      } else {
        innerThoughtFuture.cancel(true);
      }
      dispatchIfCompleted(response, reservation);
      return response;
    } catch (RuntimeException exception) {
      cancelInnerThought(innerThoughtFuture);
      submittedMessageService.compensate(reservation);
      throw exception;
    }
  }

  /** 일반 턴에서 첫 사용자 기억 조회, AI 생성, 저장, 사용 trace를 순서대로 처리한다. */
  private FreeTalkMessageSubmitResponse processRegularTurn(FreeTalkMessageReservation reservation) {
    MemoryRetrievalResult memoryResult = retrieveFirstUserMemory(reservation);
    AiFreeTalkTurnResult turnResult =
        generateTurn(reservation, AiFreeTalkResponseMode.NORMAL, memoryResult);
    FreeTalkMessageSubmitResponse response =
        submittedMessageService.finalizeTurn(reservation, turnResult);
    recordMemoryUsage(memoryResult, turnResult, response);
    return response;
  }

  /** 생성 응답의 memory ID를 해당 턴에 제공한 검색 결과와 연결해 기록한다. */
  private void recordMemoryUsage(
      MemoryRetrievalResult memoryResult,
      AiFreeTalkTurnResult turnResult,
      FreeTalkMessageSubmitResponse response) {
    if (memoryResult == null) {
      return;
    }
    memoryRetrievalService.recordUsage(
        memoryResult,
        turnResult.usedMemoryIds(),
        response.nextMessage() == null ? null : response.nextMessage().messageId());
  }

  /**
   * 종료 의사 확인에 대한 사용자의 선택을 처리한다.
   *
   * @param userId 요청 사용자 ID
   * @param learningSessionId 프리톡 학습 세션 ID
   * @param request 종료 또는 계속 대화 결정 요청
   * @return 저장된 사용자 발화와 AI 처리 결과
   * @throws com.landit.landitbe.shared.exception.ApiException 세션 접근, 처리 상태 또는 AI 생성에 실패할 때
   * @throws com.landit.landitbe.feature.learning.conversation.exception.SessionException 프리톡 이용 한도에
   *     도달했을 때
   */
  public FreeTalkMessageSubmitResponse decideExit(
      long userId, long learningSessionId, FreeTalkExitDecisionRequest request) {
    FreeTalkMessageSubmitResponse replayedResponse =
        replayService.findCompletedDecisionResponse(
            userId, learningSessionId, request.submittedMessageId(), request.decision());
    if (replayedResponse != null) {
      return replayedResponse;
    }
    FreeTalkExitDecisionReservation reservation =
        submittedMessageService.reserveDecision(
            userId, learningSessionId, request.submittedMessageId(), request.decision());
    return processExitDecision(reservation);
  }

  /** 속마음·결정 확정·보상 순서를 한 예외 경계에서 보존한다. */
  private FreeTalkMessageSubmitResponse processExitDecision(
      FreeTalkExitDecisionReservation reservation) {
    CompletableFuture<AiFreeTalkInnerThoughtResult> innerThoughtFuture = null;
    try {
      AiFreeTalkInnerThoughtRequest innerThoughtRequest = innerThoughtRequest(reservation);
      innerThoughtFuture = startInnerThought(innerThoughtRequest);
      FreeTalkMessageSubmitResponse response = finalizeDecision(reservation);
      recordInnerThought(innerThoughtRequest, innerThoughtFuture);
      dispatchIfCompleted(response);
      return response;
    } catch (RuntimeException exception) {
      cancelInnerThought(innerThoughtFuture);
      submittedMessageService.compensateDecision(reservation);
      throw exception;
    }
  }

  /** 종료 선택에 따라 END 또는 CONTINUE의 상태 확정 경계를 선택한다. */
  private FreeTalkMessageSubmitResponse finalizeDecision(
      FreeTalkExitDecisionReservation reservation) {
    if (reservation.decision() == FreeTalkExitDecision.END) {
      return finalizeEndDecision(reservation);
    }
    return finalizeContinueDecision(reservation);
  }

  /** END 선택은 closing AI 응답과 완료 확정을 한 경계에서 처리한다. */
  private FreeTalkMessageSubmitResponse finalizeEndDecision(
      FreeTalkExitDecisionReservation reservation) {
    AiFreeTalkClosingRequest closingRequest =
        closingRequestForDecision(reservation, AiFreeTalkClosingReason.USER_CONFIRMED);
    AiFreeTalkClosingResult closingResult = aiFreeTalkClient.generateClosing(closingRequest);
    return submittedMessageService.finalizeEnd(reservation, closingResult);
  }

  /** CONTINUE 선택은 후속 turn AI 응답과 진행 확정을 한 경계에서 처리한다. */
  private FreeTalkMessageSubmitResponse finalizeContinueDecision(
      FreeTalkExitDecisionReservation reservation) {
    AiFreeTalkTurnRequest turnRequest =
        turnRequestForDecision(reservation, AiFreeTalkResponseMode.CONTINUE_AFTER_EXIT_DECLINED);
    AiFreeTalkTurnResult turnResult = aiFreeTalkClient.generateTurn(turnRequest);
    return submittedMessageService.finalizeContinue(reservation, turnResult);
  }

  private AiFreeTalkTurnRequest turnRequest(
      FreeTalkMessageReservation reservation,
      AiFreeTalkResponseMode responseMode,
      List<AiFreeTalkMemoryContext> memoryContext) {
    AiFreeTalkContextWindow context =
        contextWindow(reservation.userId(), reservation.freeTalkSessionId());
    return new AiFreeTalkTurnRequest(
        reservation.freeTalkSessionId(),
        reservation.characterId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        responseMode,
        isFirstUserTurn(reservation),
        reservation.topic(),
        modelHistory(reservation.history(), context),
        memoryContext,
        context.contextPolicyVersion(),
        context.sessionSummary(),
        context.historyIncomplete());
  }

  /** 장기기억은 제목 생성이 필요한 실제 첫 사용자 턴에서만 조회한다. */
  private MemoryRetrievalResult retrieveFirstUserMemory(FreeTalkMessageReservation reservation) {
    if (!isFirstUserTurn(reservation)) {
      return null;
    }
    String query = firstUserMessageQuery(reservation);
    return memoryRetrievalService.retrieve(
        new MemoryRetrievalRequest(
            reservation.freeTalkSessionId(),
            reservation.userId(),
            reservation.characterId(),
            MemoryRetrievalStage.FIRST_USER_TURN,
            query));
  }

  /** 첫 사용자 발화의 비어 있지 않은 본문만 검색 임베딩 입력으로 사용한다. */
  private String firstUserMessageQuery(FreeTalkMessageReservation reservation) {
    return reservation.history().stream()
        .filter(message -> "USER".equals(message.role()))
        .map(message -> message.content())
        .filter(content -> content != null && !content.isBlank())
        .findFirst()
        .orElse("");
  }

  private boolean isFirstUserTurn(FreeTalkMessageReservation reservation) {
    return reservation.titleGenerationRequired()
        && reservation.history().stream().filter(message -> "USER".equals(message.role())).count()
            == 1;
  }

  private AiFreeTalkTurnResult generateTurn(
      FreeTalkMessageReservation reservation,
      AiFreeTalkResponseMode responseMode,
      MemoryRetrievalResult memoryResult) {
    List<AiFreeTalkMemoryContext> memoryContext =
        memoryResult == null ? List.of() : memoryResult.contexts();
    return aiFreeTalkClient.generateTurn(turnRequest(reservation, responseMode, memoryContext));
  }

  /** 완료 응답만 후속 표현·장기기억 생성을 등록해 중간 응답을 재처리하지 않는다. */
  private void dispatchIfCompleted(
      FreeTalkMessageSubmitResponse response, FreeTalkMessageReservation reservation) {
    if (response.turnStatus() == FreeTalkTurnStatus.COMPLETED) {
      expressionGenerationDispatcher.dispatch(response.sessionId());
      memoryGenerationDispatchService.dispatch(response.sessionId());
    }
    if (response.turnStatus() == FreeTalkTurnStatus.CONTINUE && contextSummaryService != null) {
      contextSummaryService.dispatchIfNeeded(reservation);
    }
  }

  private void dispatchIfCompleted(FreeTalkMessageSubmitResponse response) {
    if (response.turnStatus() == FreeTalkTurnStatus.COMPLETED) {
      expressionGenerationDispatcher.dispatch(response.sessionId());
      memoryGenerationDispatchService.dispatch(response.sessionId());
    }
  }

  private AiFreeTalkClosingRequest closingRequest(
      FreeTalkMessageReservation reservation, AiFreeTalkClosingReason closingReason) {
    AiFreeTalkContextWindow context =
        contextWindow(reservation.userId(), reservation.freeTalkSessionId());
    return new AiFreeTalkClosingRequest(
        reservation.freeTalkSessionId(),
        reservation.characterId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        closingReason,
        reservation.titleGenerationRequired(),
        reservation.topic(),
        modelHistory(reservation.history(), context),
        context.contextPolicyVersion(),
        context.sessionSummary(),
        context.historyIncomplete());
  }

  private AiFreeTalkTurnRequest turnRequestForDecision(
      FreeTalkExitDecisionReservation reservation, AiFreeTalkResponseMode responseMode) {
    AiFreeTalkContextWindow context =
        contextWindow(reservation.userId(), reservation.freeTalkSessionId());
    return new AiFreeTalkTurnRequest(
        reservation.freeTalkSessionId(),
        reservation.characterId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        responseMode,
        false,
        reservation.topic(),
        modelHistory(reservation.history(), context),
        List.of(),
        context.contextPolicyVersion(),
        context.sessionSummary(),
        context.historyIncomplete());
  }

  private AiFreeTalkClosingRequest closingRequestForDecision(
      FreeTalkExitDecisionReservation reservation, AiFreeTalkClosingReason closingReason) {
    AiFreeTalkContextWindow context =
        contextWindow(reservation.userId(), reservation.freeTalkSessionId());
    return new AiFreeTalkClosingRequest(
        reservation.freeTalkSessionId(),
        reservation.characterId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        closingReason,
        reservation.titleGenerationRequired(),
        reservation.topic(),
        modelHistory(reservation.history(), context),
        context.contextPolicyVersion(),
        context.sessionSummary(),
        context.historyIncomplete());
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest(
      FreeTalkMessageReservation reservation) {
    AiFreeTalkContextWindow context =
        contextWindow(reservation.userId(), reservation.freeTalkSessionId());
    return new AiFreeTalkInnerThoughtRequest(
        reservation.freeTalkSessionId(),
        reservation.characterId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.topic(),
        modelHistory(reservation.history(), context),
        context.contextPolicyVersion(),
        context.sessionSummary(),
        context.historyIncomplete());
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest(
      FreeTalkExitDecisionReservation reservation) {
    AiFreeTalkContextWindow context =
        contextWindow(reservation.userId(), reservation.freeTalkSessionId());
    return new AiFreeTalkInnerThoughtRequest(
        reservation.freeTalkSessionId(),
        reservation.characterId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.topic(),
        modelHistory(reservation.history(), context),
        context.contextPolicyVersion(),
        context.sessionSummary(),
        context.historyIncomplete());
  }

  private AiFreeTalkContextWindow contextWindow(long userId, long freeTalkSessionId) {
    return contextSummaryService == null
        ? AiFreeTalkContextWindow.disabled()
        : contextSummaryService.snapshot(userId, freeTalkSessionId);
  }

  /** 확정된 요약 경계 이전 원문을 모델 요청에서 제외하고 현재 사용자 발화는 유지한다. */
  private List<AiConversationHistoryMessage> modelHistory(
      List<AiConversationHistoryMessage> history, AiFreeTalkContextWindow context) {
    if (context.sessionSummary() == null || history.size() <= 1) {
      return history;
    }
    int coveredThroughSequence = context.sessionSummary().coveredThroughSequence();
    if (coveredThroughSequence <= 0) {
      return history;
    }
    int firstIndex = Math.min(coveredThroughSequence, history.size() - 1);
    return List.copyOf(history.subList(firstIndex, history.size()));
  }

  private CompletableFuture<AiFreeTalkInnerThoughtResult> startInnerThought(
      AiFreeTalkInnerThoughtRequest request) {
    try {
      return submitCancellableAsync(() -> aiFreeTalkClient.generateInnerThought(request));
    } catch (RuntimeException exception) {
      log.warn("프리톡 속마음 작업을 시작하지 못했습니다. messageId={}", request.submittedMessageId(), exception);
      return CompletableFuture.failedFuture(exception);
    }
  }

  private void recordInnerThought(
      AiFreeTalkInnerThoughtRequest request,
      CompletableFuture<AiFreeTalkInnerThoughtResult> innerThoughtFuture) {
    innerThoughtFuture.whenComplete(
        (result, exception) -> {
          if (exception == null) {
            try {
              conversationMessageService.completeInnerThought(
                  request.submittedMessageId(), result.innerThought(), result.innerThoughtType());
            } catch (RuntimeException persistenceException) {
              log.warn(
                  "프리톡 속마음 저장에 실패했습니다. messageId={}",
                  request.submittedMessageId(),
                  persistenceException);
              conversationMessageService.failInnerThought(request.submittedMessageId());
            }
            return;
          }
          log.error(
              "프리톡 속마음 생성에 실패했습니다. "
                  + "workflow=free_talk_inner_thought_failed messageId={} errorCode={}",
              request.submittedMessageId(),
              errorCode(exception),
              exception);
          conversationMessageService.failInnerThought(request.submittedMessageId());
        });
  }

  private String errorCode(Throwable exception) {
    if (exception instanceof ApiException apiException) {
      return apiException.getErrorCode().name();
    }
    return exception.getClass().getSimpleName();
  }

  private void cancelInnerThought(
      CompletableFuture<AiFreeTalkInnerThoughtResult> innerThoughtFuture) {
    if (innerThoughtFuture != null) {
      innerThoughtFuture.cancel(true);
    }
  }

  private <T> CompletableFuture<T> submitCancellableAsync(Callable<T> task) {
    CancellableCompletableFuture<T> result = new CancellableCompletableFuture<>();
    FutureTask<T> futureTask =
        new FutureTask<>(task) {
          @Override
          protected void done() {
            if (isCancelled()) {
              result.cancel(false);
              return;
            }
            try {
              result.complete(get());
            } catch (InterruptedException exception) {
              Thread.currentThread().interrupt();
              result.completeExceptionally(exception);
            } catch (ExecutionException exception) {
              result.completeExceptionally(exception.getCause());
            }
          }
        };
    result.bind(futureTask);
    taskExecutor.execute(futureTask);
    return result;
  }

  private static class CancellableCompletableFuture<T> extends CompletableFuture<T> {

    private Future<?> task;

    private void bind(Future<?> task) {
      this.task = task;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      boolean cancelled = super.cancel(mayInterruptIfRunning);
      if (cancelled && task != null) {
        task.cancel(mayInterruptIfRunning);
      }
      return cancelled;
    }
  }
}
