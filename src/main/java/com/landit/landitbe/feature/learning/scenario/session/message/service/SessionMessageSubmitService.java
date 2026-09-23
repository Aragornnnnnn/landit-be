// 사용자 발화 제출과 AI 후속 메시지 저장 흐름을 처리한다.

package com.landit.landitbe.feature.learning.scenario.session.message.service;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.domain.SessionMessageInputType;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.scenario.assessment.service.SessionLevelAssessmentGenerationService;
import com.landit.landitbe.feature.learning.scenario.session.innerthought.client.ai.AiInnerThoughtResult;
import com.landit.landitbe.feature.learning.scenario.session.message.dto.SessionMessageSubmitRequest;
import com.landit.landitbe.feature.learning.scenario.session.message.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.observability.FailureObservation;
import com.landit.landitbe.shared.observability.ObservationContext;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 사용자 발화 저장, 병렬 AI 요청, 후속 메시지 저장과 실패 보상을 조율한다. */
@Service
@Slf4j
public class SessionMessageSubmitService {

  private final SubmittedMessageService submittedMessageService;
  private final SessionMessageAiGenerator sessionMessageAiGenerator;
  private final SessionInnerThoughtGenerator sessionInnerThoughtGenerator;
  private final ConversationMessageService conversationMessageService;
  private final SessionMessageFeedbackRequester sessionMessageFeedbackRequester;
  private final SessionLevelAssessmentGenerationService levelAssessmentGenerationService;
  private final GeneratedMessageService generatedMessageService;
  private final UserProfileService userProfileService;
  private final PlatformTransactionManager transactionManager;
  private final TaskExecutor taskExecutor;

  SessionMessageSubmitService(
      SubmittedMessageService submittedMessageService,
      SessionMessageAiGenerator sessionMessageAiGenerator,
      SessionInnerThoughtGenerator sessionInnerThoughtGenerator,
      ConversationMessageService conversationMessageService,
      SessionMessageFeedbackRequester sessionMessageFeedbackRequester,
      SessionLevelAssessmentGenerationService levelAssessmentGenerationService,
      GeneratedMessageService generatedMessageService,
      UserProfileService userProfileService,
      PlatformTransactionManager transactionManager,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
    this.submittedMessageService = submittedMessageService;
    this.sessionMessageAiGenerator = sessionMessageAiGenerator;
    this.sessionInnerThoughtGenerator = sessionInnerThoughtGenerator;
    this.conversationMessageService = conversationMessageService;
    this.sessionMessageFeedbackRequester = sessionMessageFeedbackRequester;
    this.levelAssessmentGenerationService = levelAssessmentGenerationService;
    this.generatedMessageService = generatedMessageService;
    this.userProfileService = userProfileService;
    this.transactionManager = transactionManager;
    this.taskExecutor = taskExecutor;
  }

  /**
   * 사용자 발화를 저장하고 AI 후속 메시지 생성과 저장을 조율한다.
   *
   * @param userId 세션 소유자 ID
   * @param sessionId 발화를 제출할 학습 세션 ID
   * @param request 사용자 발화와 입력 방식
   * @return 저장된 사용자 발화와 AI 후속 메시지
   * @throws RuntimeException AI 생성 또는 결과 저장에 실패했을 때
   */
  public SessionMessageSubmitResponse submitMessage(
      long userId, long sessionId, SessionMessageSubmitRequest request) {
    String content = request.normalizedContent();
    SessionMessageInputType inputType = request.requiredInputType();
    String clientMessageId = request.validatedClientMessageId();
    Reservation reservation =
        executeInTransaction(
            () -> {
              SessionMessageSubmitResponse replay =
                  submittedMessageService.replay(
                      userId, sessionId, content, inputType, clientMessageId);
              if (replay != null) {
                return new Reservation(null, replay);
              }
              SubmittedMessageContext context =
                  submittedMessageService.record(
                      userId, sessionId, content, inputType, clientMessageId);
              sessionMessageFeedbackRequester.prepare(context);
              return new Reservation(context, null);
            });
    if (reservation.response() != null) {
      return reservation.response();
    }
    SubmittedMessageContext submittedContext = reservation.context();
    return ObservationContext.call(
        sessionId,
        null,
        submittedContext.submittedMessageId(),
        () -> {
          AsyncGenerationRequests asyncGenerationRequests = AsyncGenerationRequests.none();
          try {
            asyncGenerationRequests = startAsyncGeneration(submittedContext);
            // 외부 AI 호출 중에는 DB 트랜잭션과 세션 row lock을 유지하지 않는다.
            SessionMessageAiGenerator.Generation generation = generateAiMessage(submittedContext);
            ProcessingStatus feedbackProcessingStatus =
                feedbackProcessingStatus(submittedContext, generation);
            SessionMessageSubmitResponse response =
                executeInTransaction(
                    () -> {
                      if (generation.completed()) {
                        // 세션 잠금보다 먼저 사용자 잠금을 획득해 완료 기록 순서를 통일한다.
                        userProfileService.requireActiveForUpdate(userId);
                      }
                      return generatedMessageService.record(
                          submittedContext, generation, feedbackProcessingStatus);
                    });
            recordInnerThoughtAfterMessageGeneration(
                submittedContext.submittedMessageId(),
                asyncGenerationRequests.innerThoughtFuture());
            if (response.progress().completed()) {
              levelAssessmentGenerationService.startIfNeeded(userId, sessionId);
            }
            log.info(
                "session message submitted: userId={}, sessionId={}, messageId={}, "
                    + "inputType={}, contentLength={}",
                userId,
                sessionId,
                response.submittedMessage().messageId(),
                inputType,
                content.length());
            return response;
          } catch (RuntimeException exception) {
            // 키 있는 발화는 보존하고 구 FE의 키 없는 실패 발화는 다시 입력할 수 있게 한다.
            asyncGenerationRequests.cancel();
            removeSubmittedMessageInTransaction(submittedContext);
            throw exception;
          }
        });
  }

  private AsyncGenerationRequests startAsyncGeneration(SubmittedMessageContext submittedContext) {
    if (submittedContext.nextQuestion().isEmpty()) {
      return AsyncGenerationRequests.none();
    }
    CompletableFuture<AiInnerThoughtResult> innerThoughtFuture =
        submitCancellableAsync(() -> sessionInnerThoughtGenerator.generate(submittedContext));
    CompletableFuture<Void> feedbackFuture;
    try {
      feedbackFuture =
          submitCancellableAsync(
              () -> {
                requestMessageFeedback(submittedContext);
                return null;
              });
    } catch (RuntimeException exception) {
      innerThoughtFuture.cancel(true);
      throw exception;
    }
    feedbackFuture.whenComplete(
        (ignored, exception) -> {
          if (exception != null) {
            if (unwrap(exception) instanceof java.util.concurrent.CancellationException) {
              FailureObservation.observed(
                  "message_feedback",
                  "execution",
                  "parent_request_cancelled",
                  "expected_rejection");
            } else {
              FailureObservation.failed(
                  "message_feedback", "execution", "unexpected_worker_failure", unwrap(exception));
            }
            try {
              conversationMessageService.failFeedback(submittedContext.submittedMessageId());
            } catch (RuntimeException persistenceException) {
              FailureObservation.failed(
                  "message_feedback",
                  "failure_state_persistence",
                  "storage_failed",
                  persistenceException);
            }
          }
        });
    return new AsyncGenerationRequests(
        submittedContext.submittedMessageId(), innerThoughtFuture, feedbackFuture);
  }

  private ProcessingStatus feedbackProcessingStatus(
      SubmittedMessageContext submittedContext, SessionMessageAiGenerator.Generation generation) {
    if (!generation.completed()) {
      return ProcessingStatus.PREPARING;
    }
    try {
      return requestMessageFeedback(submittedContext);
    } catch (RuntimeException exception) {
      FailureObservation.failed(
          "message_feedback", "execution", "unexpected_worker_failure", exception);
      // 마지막 발화의 피드백 장애가 세션 완료와 독립 수준 평가를 막지 않게 한다.
      return ProcessingStatus.FAILED;
    }
  }

  void recordInnerThoughtAfterMessageGeneration(
      long submittedMessageId, CompletableFuture<AiInnerThoughtResult> innerThoughtFuture) {
    if (innerThoughtFuture == null) {
      return;
    }
    innerThoughtFuture
        .handleAsync(
            (result, exception) -> {
              if (exception != null) {
                FailureObservation.failed(
                    "inner_thought", "generation", "result_missing", unwrap(exception));
              }
              try {
                if (exception == null) {
                  conversationMessageService.completeInnerThought(
                      result.messageId(), result.innerThought(), result.innerThoughtType());
                } else {
                  conversationMessageService.failInnerThought(submittedMessageId);
                }
              } catch (RuntimeException persistenceException) {
                FailureObservation.failed(
                    "inner_thought", "persistence", "storage_failed", persistenceException);
              }
              return null;
            },
            taskExecutor)
        .exceptionally(
            exception -> {
              FailureObservation.failed(
                  "inner_thought", "dispatch", "executor_unavailable", unwrap(exception));
              try {
                conversationMessageService.failInnerThought(submittedMessageId);
              } catch (RuntimeException persistenceException) {
                FailureObservation.failed(
                    "inner_thought", "persistence", "storage_failed", persistenceException);
              }
              return null;
            });
  }

  private Throwable unwrap(Throwable exception) {
    return exception instanceof java.util.concurrent.CompletionException
            && exception.getCause() != null
        ? exception.getCause()
        : exception;
  }

  private SessionMessageAiGenerator.Request toAiRequest(SubmittedMessageContext submittedContext) {
    return new SessionMessageAiGenerator.Request(
        submittedContext.learningSessionId(),
        submittedContext.submittedMessageId(),
        submittedContext.submittedTurnNumber(),
        submittedContext.scenarioContext(),
        submittedContext.conversationHistory(),
        submittedContext.nextQuestion());
  }

  private SessionMessageAiGenerator.Generation generateAiMessage(
      SubmittedMessageContext submittedContext) {
    return sessionMessageAiGenerator.generate(toAiRequest(submittedContext));
  }

  private ProcessingStatus requestMessageFeedback(SubmittedMessageContext submittedContext) {
    return sessionMessageFeedbackRequester.request(submittedContext);
  }

  private void removeSubmittedMessageInTransaction(SubmittedMessageContext submittedContext) {
    executeInTransaction(
        () -> {
          submittedMessageService.remove(submittedContext);
          return null;
        });
  }

  private <T> T executeInTransaction(Supplier<T> supplier) {
    return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
  }

  /** 실행 중인 외부 AI 호출도 인터럽트할 수 있도록 FutureTask와 완료 상태를 연결한다. */
  private <T> CompletableFuture<T> submitCancellableAsync(Callable<T> task) {
    CancellableCompletableFuture<T> result = new CancellableCompletableFuture<>();
    FutureTask<T> futureTask =
        new FutureTask<>(
            () -> {
              try {
                return task.call();
              } catch (Exception failure) {
                ObservationContext.remember(failure);
                throw failure;
              }
            }) {
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

  private record AsyncGenerationRequests(
      Long submittedMessageId,
      CompletableFuture<AiInnerThoughtResult> innerThoughtFuture,
      CompletableFuture<Void> feedbackFuture) {

    private static AsyncGenerationRequests none() {
      return new AsyncGenerationRequests(null, null, null);
    }

    private void cancel() {
      if (innerThoughtFuture != null) {
        innerThoughtFuture.cancel(true);
      }
      if (feedbackFuture != null) {
        feedbackFuture.cancel(true);
      }
    }
  }

  private record Reservation(
      SubmittedMessageContext context, SessionMessageSubmitResponse response) {}

  /** CompletableFuture 취소를 실제 실행 작업의 취소로 전달한다. */
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
