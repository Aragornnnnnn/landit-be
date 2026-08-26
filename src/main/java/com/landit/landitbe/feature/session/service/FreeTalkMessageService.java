// 프리톡 발화와 종료 확인의 AI 호출 흐름을 조정한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.memory.service.FreeTalkMemoryGenerationDispatchService;
import com.landit.landitbe.feature.memory.service.FreeTalkMemoryRetrievalService;
import com.landit.landitbe.feature.memory.service.MemoryRetrievalStage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingReason;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkInnerThoughtResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkResponseMode;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.session.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.session.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.session.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse;
import com.landit.landitbe.shared.exception.ApiException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/** 프리톡 발화와 종료 확인의 AI 호출 흐름을 조정한다. */
@Service
@Slf4j
public class FreeTalkMessageService {

  private final FreeTalkSubmittedMessageService submittedMessageService;
  private final AiFreeTalkClient aiFreeTalkClient;
  private final SessionMessageService sessionMessageService;
  private final TaskExecutor taskExecutor;
  private final FreeTalkExpressionGenerationDispatcher expressionGenerationDispatcher;
  private final FreeTalkMemoryGenerationDispatchService memoryGenerationDispatchService;
  private final FreeTalkMemoryRetrievalService memoryRetrievalService;

  FreeTalkMessageService(
      FreeTalkSubmittedMessageService submittedMessageService,
      AiFreeTalkClient aiFreeTalkClient,
      SessionMessageService sessionMessageService,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
      FreeTalkExpressionGenerationDispatcher expressionGenerationDispatcher,
      FreeTalkMemoryGenerationDispatchService memoryGenerationDispatchService,
      FreeTalkMemoryRetrievalService memoryRetrievalService) {
    this.submittedMessageService = submittedMessageService;
    this.aiFreeTalkClient = aiFreeTalkClient;
    this.sessionMessageService = sessionMessageService;
    this.taskExecutor = taskExecutor;
    this.expressionGenerationDispatcher = expressionGenerationDispatcher;
    this.memoryGenerationDispatchService = memoryGenerationDispatchService;
    this.memoryRetrievalService = memoryRetrievalService;
  }

  /**
   * 사용자 발화를 처리하고 AI 후속 메시지 또는 종료 확인 상태를 반환한다.
   *
   * @param userId 요청 사용자 ID
   * @param learningSessionId 프리톡 학습 세션 ID
   * @param request 사용자 발화 요청
   * @return 저장된 사용자 발화와 AI 처리 결과
   * @throws com.landit.landitbe.shared.exception.ApiException 세션 접근, 처리 상태 또는 AI 생성에 실패할 때
   */
  public FreeTalkMessageSubmitResponse submit(
      long userId, long learningSessionId, FreeTalkMessageSubmitRequest request) {
    FreeTalkMessageSubmitResponse replayedResponse =
        submittedMessageService.findCompletedResponse(userId, learningSessionId, request);
    if (replayedResponse != null) {
      return replayedResponse;
    }
    FreeTalkSubmittedMessageService.Reservation reservation =
        submittedMessageService.reserve(userId, learningSessionId, request);
    AiFreeTalkInnerThoughtRequest innerThoughtRequest = innerThoughtRequest(reservation);
    CompletableFuture<AiFreeTalkInnerThoughtResult> innerThoughtFuture = null;
    try {
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
          != com.landit.landitbe.feature.session.domain.FreeTalkTurnStatus
              .EXIT_CONFIRMATION_REQUIRED) {
        recordInnerThought(innerThoughtRequest, innerThoughtFuture);
      } else {
        innerThoughtFuture.cancel(true);
      }
      dispatchIfCompleted(response);
      return response;
    } catch (RuntimeException exception) {
      cancelInnerThought(innerThoughtFuture);
      submittedMessageService.compensate(reservation);
      throw exception;
    }
  }

  private FreeTalkMessageSubmitResponse processRegularTurn(
      FreeTalkSubmittedMessageService.Reservation reservation) {
    FreeTalkMemoryRetrievalService.RetrievalResult memoryResult =
        retrieveFirstUserMemory(reservation);
    AiFreeTalkTurnResult turnResult =
        generateTurn(reservation, AiFreeTalkResponseMode.NORMAL, memoryResult);
    FreeTalkMessageSubmitResponse response =
        submittedMessageService.finalizeTurn(reservation, turnResult);
    recordMemoryUsage(memoryResult, turnResult, response);
    return response;
  }

  private void recordMemoryUsage(
      FreeTalkMemoryRetrievalService.RetrievalResult memoryResult,
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
   */
  public FreeTalkMessageSubmitResponse decideExit(
      long userId, long learningSessionId, FreeTalkExitDecisionRequest request) {
    FreeTalkMessageSubmitResponse replayedResponse =
        submittedMessageService.findCompletedDecisionResponse(
            userId, learningSessionId, request.submittedMessageId(), request.decision());
    if (replayedResponse != null) {
      return replayedResponse;
    }
    FreeTalkSubmittedMessageService.DecisionReservation reservation =
        submittedMessageService.reserveDecision(
            userId, learningSessionId, request.submittedMessageId(), request.decision());
    AiFreeTalkInnerThoughtRequest innerThoughtRequest = innerThoughtRequest(reservation);
    CompletableFuture<AiFreeTalkInnerThoughtResult> innerThoughtFuture = null;
    try {
      innerThoughtFuture = startInnerThought(innerThoughtRequest);
      FreeTalkMessageSubmitResponse response;
      if (reservation.decision() == FreeTalkExitDecision.END) {
        response =
            submittedMessageService.finalizeEnd(
                reservation,
                aiFreeTalkClient.generateClosing(
                    closingRequestForDecision(
                        reservation, AiFreeTalkClosingReason.USER_CONFIRMED)));
      } else {
        response =
            submittedMessageService.finalizeContinue(
                reservation,
                aiFreeTalkClient.generateTurn(
                    turnRequestForDecision(
                        reservation, AiFreeTalkResponseMode.CONTINUE_AFTER_EXIT_DECLINED)));
      }
      recordInnerThought(innerThoughtRequest, innerThoughtFuture);
      dispatchIfCompleted(response);
      return response;
    } catch (RuntimeException exception) {
      cancelInnerThought(innerThoughtFuture);
      submittedMessageService.compensateDecision(reservation);
      throw exception;
    }
  }

  private AiFreeTalkTurnRequest turnRequest(
      FreeTalkSubmittedMessageService.Reservation reservation,
      AiFreeTalkResponseMode responseMode,
      List<AiFreeTalkMemoryContext> memoryContext) {
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
        reservation.history(),
        memoryContext);
  }

  private FreeTalkMemoryRetrievalService.RetrievalResult retrieveFirstUserMemory(
      FreeTalkSubmittedMessageService.Reservation reservation) {
    if (!isFirstUserTurn(reservation)) {
      return null;
    }
    String query = firstUserMessageQuery(reservation);
    return memoryRetrievalService.retrieve(
        new FreeTalkMemoryRetrievalService.RetrievalRequest(
            reservation.freeTalkSessionId(),
            reservation.userId(),
            reservation.characterId(),
            MemoryRetrievalStage.FIRST_USER_TURN,
            query));
  }

  private String firstUserMessageQuery(FreeTalkSubmittedMessageService.Reservation reservation) {
    return reservation.history().stream()
        .filter(message -> "USER".equals(message.role()))
        .map(message -> message.content())
        .filter(content -> content != null && !content.isBlank())
        .findFirst()
        .orElse("");
  }

  private boolean isFirstUserTurn(FreeTalkSubmittedMessageService.Reservation reservation) {
    return reservation.titleGenerationRequired()
        && reservation.history().stream().filter(message -> "USER".equals(message.role())).count()
            == 1;
  }

  private AiFreeTalkTurnResult generateTurn(
      FreeTalkSubmittedMessageService.Reservation reservation,
      AiFreeTalkResponseMode responseMode,
      FreeTalkMemoryRetrievalService.RetrievalResult memoryResult) {
    List<AiFreeTalkMemoryContext> memoryContext =
        memoryResult == null ? List.of() : memoryResult.contexts();
    return aiFreeTalkClient.generateTurn(turnRequest(reservation, responseMode, memoryContext));
  }

  // 완료 응답이면 맞춤 표현 생성 작업을 제출한다.
  private void dispatchIfCompleted(FreeTalkMessageSubmitResponse response) {
    if (response.turnStatus() == FreeTalkTurnStatus.COMPLETED) {
      expressionGenerationDispatcher.dispatch(response.sessionId());
      memoryGenerationDispatchService.dispatch(response.sessionId());
    }
  }

  private AiFreeTalkClosingRequest closingRequest(
      FreeTalkSubmittedMessageService.Reservation reservation,
      AiFreeTalkClosingReason closingReason) {
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
        reservation.history());
  }

  private AiFreeTalkTurnRequest turnRequestForDecision(
      FreeTalkSubmittedMessageService.DecisionReservation reservation,
      AiFreeTalkResponseMode responseMode) {
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
        reservation.history(),
        List.of());
  }

  private AiFreeTalkClosingRequest closingRequestForDecision(
      FreeTalkSubmittedMessageService.DecisionReservation reservation,
      AiFreeTalkClosingReason closingReason) {
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
        reservation.history());
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest(
      FreeTalkSubmittedMessageService.Reservation reservation) {
    return new AiFreeTalkInnerThoughtRequest(
        reservation.freeTalkSessionId(),
        reservation.characterId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.topic(),
        reservation.history());
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest(
      FreeTalkSubmittedMessageService.DecisionReservation reservation) {
    return new AiFreeTalkInnerThoughtRequest(
        reservation.freeTalkSessionId(),
        reservation.characterId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.topic(),
        reservation.history());
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
              sessionMessageService.completeInnerThought(
                  request.submittedMessageId(), result.innerThought(), result.innerThoughtType());
            } catch (RuntimeException persistenceException) {
              log.warn(
                  "프리톡 속마음 저장에 실패했습니다. messageId={}",
                  request.submittedMessageId(),
                  persistenceException);
              sessionMessageService.failInnerThought(request.submittedMessageId());
            }
            return;
          }
          log.error(
              "프리톡 속마음 생성에 실패했습니다. "
                  + "workflow=free_talk_inner_thought_failed messageId={} errorCode={}",
              request.submittedMessageId(),
              errorCode(exception),
              exception);
          sessionMessageService.failInnerThought(request.submittedMessageId());
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
