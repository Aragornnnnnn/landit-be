// 사용자 발화 제출과 AI 후속 메시지 저장 흐름을 처리한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiInnerThoughtResult;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionMessageInputType;
import com.landit.landitbe.feature.session.dto.SessionMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.SessionMessageSubmitResponse;
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
  private final SessionMessageService sessionMessageService;
  private final SessionMessageFeedbackRequester sessionMessageFeedbackRequester;
  private final GeneratedMessageService generatedMessageService;
  private final PlatformTransactionManager transactionManager;
  private final TaskExecutor taskExecutor;

  SessionMessageSubmitService(
      SubmittedMessageService submittedMessageService,
      SessionMessageAiGenerator sessionMessageAiGenerator,
      SessionInnerThoughtGenerator sessionInnerThoughtGenerator,
      SessionMessageService sessionMessageService,
      SessionMessageFeedbackRequester sessionMessageFeedbackRequester,
      GeneratedMessageService generatedMessageService,
      PlatformTransactionManager transactionManager,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
    this.submittedMessageService = submittedMessageService;
    this.sessionMessageAiGenerator = sessionMessageAiGenerator;
    this.sessionInnerThoughtGenerator = sessionInnerThoughtGenerator;
    this.sessionMessageService = sessionMessageService;
    this.sessionMessageFeedbackRequester = sessionMessageFeedbackRequester;
    this.generatedMessageService = generatedMessageService;
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
    // AI 요청에 사용자 메시지 ID가 필요하므로 짧은 트랜잭션으로 먼저 저장한다.
    SubmittedMessageContext submittedContext =
        executeInTransaction(
            () -> submittedMessageService.record(userId, sessionId, content, inputType));
    AsyncGenerationRequests asyncGenerationRequests = AsyncGenerationRequests.none();
    try {
      asyncGenerationRequests = startAsyncGeneration(submittedContext);
      // 외부 AI 호출 중에는 DB 트랜잭션과 세션 row lock을 유지하지 않는다.
      SessionMessageAiGenerator.Generation generation = generateAiMessage(submittedContext);
      ProcessingStatus feedbackProcessingStatus =
          feedbackProcessingStatus(submittedContext, generation);
      SessionMessageSubmitResponse response =
          executeInTransaction(
              () ->
                  generatedMessageService.record(
                      submittedContext, generation, feedbackProcessingStatus));
      recordInnerThoughtAfterMessageGeneration(asyncGenerationRequests);
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
      // AI 생성이나 결과 저장 실패 시 제출 메시지를 제거해 부분 히스토리를 막는다.
      asyncGenerationRequests.cancel();
      removeSubmittedMessageInTransaction(submittedContext);
      throw exception;
    }
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
            log.warn(
                "AI 메시지별 피드백 요청에 실패했습니다. workflow=message_feedback sessionId={} messageId={}",
                submittedContext.sessionId(),
                submittedContext.submittedMessageId(),
                exception);
            sessionMessageService.failFeedback(submittedContext.submittedMessageId());
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
    return requestMessageFeedback(submittedContext);
  }

  private void recordInnerThoughtAfterMessageGeneration(
      AsyncGenerationRequests asyncGenerationRequests) {
    if (asyncGenerationRequests.innerThoughtFuture() == null) {
      return;
    }
    CompletableFuture<AiInnerThoughtResult> recordingFuture =
        asyncGenerationRequests
            .innerThoughtFuture()
            .whenCompleteAsync(
                (result, exception) -> {
                  if (exception == null) {
                    sessionMessageService.completeInnerThought(
                        result.messageId(), result.innerThought(), result.innerThoughtType());
                    return;
                  }
                  log.warn("AI 속마음 생성에 실패했습니다. workflow=inner_thought", exception);
                  sessionMessageService.failInnerThought(
                      asyncGenerationRequests.submittedMessageId());
                },
                taskExecutor);
    recordingFuture.exceptionally(
        exception -> {
          log.error("AI 속마음 처리 결과를 저장하지 못했습니다. workflow=inner_thought", exception);
          return null;
        });
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
    try {
      return sessionMessageAiGenerator.generate(toAiRequest(submittedContext));
    } catch (RuntimeException exception) {
      log.warn(
          "AI 메시지 생성에 실패했습니다. workflow=message_generation sessionId={}",
          submittedContext.sessionId(),
          exception);
      throw exception;
    }
  }

  private ProcessingStatus requestMessageFeedback(SubmittedMessageContext submittedContext) {
    try {
      return sessionMessageFeedbackRequester.request(submittedContext);
    } catch (RuntimeException exception) {
      log.warn(
          "AI 메시지별 피드백 요청에 실패했습니다. workflow=message_feedback sessionId={} messageId={}",
          submittedContext.sessionId(),
          submittedContext.submittedMessageId(),
          exception);
      throw exception;
    }
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
