// 사용자 발화 제출과 AI 후속 메시지 저장 유스케이스를 처리한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.session.api.dto.SessionMessageSubmitRequest;
import com.landit.landitbe.session.api.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.session.application.port.AiInnerThoughtResult;
import com.landit.landitbe.session.domain.ProcessingStatus;
import com.landit.landitbe.session.domain.SessionMessageInputType;
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

/**
 * UseCase: API 요청 하나의 전체 흐름과 트랜잭션 경계를 책임진다.
 * Recorder: 그 흐름 안에서 사용자 메시지 저장이나 AI 결과 저장 같은
 * 특정 기록 작업만 책임진다.
 */
@Service
@Slf4j
public class SessionMessageSubmitUseCase {

    private final SubmittedMessageRecorder submittedMessageRecorder;
    private final SessionMessageAiGenerator sessionMessageAiGenerator;
    private final SessionInnerThoughtGenerator sessionInnerThoughtGenerator;
    private final SessionInnerThoughtRecorder sessionInnerThoughtRecorder;
    private final SessionMessageFeedbackRequester sessionMessageFeedbackRequester;
    private final SessionMessageFeedbackRecorder sessionMessageFeedbackRecorder;
    private final GeneratedMessageRecorder generatedMessageRecorder;
    private final PlatformTransactionManager transactionManager;
    private final TaskExecutor taskExecutor;

    SessionMessageSubmitUseCase(
            SubmittedMessageRecorder submittedMessageRecorder,
            SessionMessageAiGenerator sessionMessageAiGenerator,
            SessionInnerThoughtGenerator sessionInnerThoughtGenerator,
            SessionInnerThoughtRecorder sessionInnerThoughtRecorder,
            SessionMessageFeedbackRequester sessionMessageFeedbackRequester,
            SessionMessageFeedbackRecorder sessionMessageFeedbackRecorder,
            GeneratedMessageRecorder generatedMessageRecorder,
            PlatformTransactionManager transactionManager,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.submittedMessageRecorder = submittedMessageRecorder;
        this.sessionMessageAiGenerator = sessionMessageAiGenerator;
        this.sessionInnerThoughtGenerator = sessionInnerThoughtGenerator;
        this.sessionInnerThoughtRecorder = sessionInnerThoughtRecorder;
        this.sessionMessageFeedbackRequester = sessionMessageFeedbackRequester;
        this.sessionMessageFeedbackRecorder = sessionMessageFeedbackRecorder;
        this.generatedMessageRecorder = generatedMessageRecorder;
        this.transactionManager = transactionManager;
        this.taskExecutor = taskExecutor;
    }

    /** 사용자 발화를 저장하고 AI 후속 메시지까지 저장한 뒤 응답한다. */
    public SessionMessageSubmitResponse submitMessage(
            long userId,
            long sessionId,
            SessionMessageSubmitRequest request
    ) {
        String content = request.normalizedContent();
        SessionMessageInputType inputType = request.requiredInputType();
        // AI 요청에 사용자 메시지 ID가 필요하므로 짧은 트랜잭션으로 먼저 저장한다.
        SubmittedMessageContext submittedContext = executeInTransaction(
                () -> submittedMessageRecorder.record(userId, sessionId, content, inputType)
        );
        AsyncGenerationRequests asyncGenerationRequests = AsyncGenerationRequests.none();
        try {
            asyncGenerationRequests = startAsyncGeneration(submittedContext);
            // 외부 AI 호출 중에는 DB 트랜잭션과 세션 row lock을 유지하지 않는다.
            SessionMessageAiGenerator.Generation generation = generateAiMessage(submittedContext);
            ProcessingStatus feedbackProcessingStatus = feedbackProcessingStatus(
                    submittedContext,
                    generation
            );
            SessionMessageSubmitResponse response = executeInTransaction(() -> generatedMessageRecorder.record(
                    submittedContext,
                    generation,
                    feedbackProcessingStatus
            ));
            recordInnerThoughtAfterMessageGeneration(asyncGenerationRequests);
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
        CompletableFuture<AiInnerThoughtResult> innerThoughtFuture = submitCancellableAsync(
                () -> sessionInnerThoughtGenerator.generate(submittedContext)
        );
        CompletableFuture<Void> feedbackFuture;
        try {
            feedbackFuture = submitCancellableAsync(() -> {
                requestMessageFeedback(submittedContext);
                return null;
            });
        } catch (RuntimeException exception) {
            innerThoughtFuture.cancel(true);
            throw exception;
        }
        feedbackFuture.whenComplete((ignored, exception) -> {
            if (exception != null) {
                log.warn("AI 메시지별 피드백 요청에 실패했습니다. workflow=message_feedback sessionId={} messageId={}",
                        submittedContext.sessionId(), submittedContext.submittedMessageId(), exception);
                sessionMessageFeedbackRecorder.fail(submittedContext.submittedMessageId());
            }
        });
        return new AsyncGenerationRequests(
                submittedContext.submittedMessageId(),
                innerThoughtFuture,
                feedbackFuture
        );
    }

    private ProcessingStatus feedbackProcessingStatus(
            SubmittedMessageContext submittedContext,
            SessionMessageAiGenerator.Generation generation
    ) {
        if (!generation.completed()) {
            return ProcessingStatus.PREPARING;
        }
        return requestMessageFeedback(submittedContext);
    }

    private void recordInnerThoughtAfterMessageGeneration(
            AsyncGenerationRequests asyncGenerationRequests
    ) {
        if (asyncGenerationRequests.innerThoughtFuture() == null) {
            return;
        }
        CompletableFuture<AiInnerThoughtResult> recordingFuture = asyncGenerationRequests.innerThoughtFuture()
                .whenCompleteAsync((result, exception) -> {
                    if (exception == null) {
                        sessionInnerThoughtRecorder.complete(result);
                        return;
                    }
                    log.warn("AI 속마음 생성에 실패했습니다. workflow=inner_thought", exception);
                    sessionInnerThoughtRecorder.fail(asyncGenerationRequests.submittedMessageId());
                }, taskExecutor);
        recordingFuture.exceptionally(exception -> {
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
                submittedContext.nextQuestion()
        );
    }

    private SessionMessageAiGenerator.Generation generateAiMessage(
            SubmittedMessageContext submittedContext
    ) {
        try {
            return sessionMessageAiGenerator.generate(toAiRequest(submittedContext));
        } catch (RuntimeException exception) {
            log.warn("AI 메시지 생성에 실패했습니다. workflow=message_generation sessionId={}",
                    submittedContext.sessionId(), exception);
            throw exception;
        }
    }

    private ProcessingStatus requestMessageFeedback(SubmittedMessageContext submittedContext) {
        try {
            return sessionMessageFeedbackRequester.request(submittedContext);
        } catch (RuntimeException exception) {
            log.warn("AI 메시지별 피드백 요청에 실패했습니다. workflow=message_feedback sessionId={} messageId={}",
                    submittedContext.sessionId(), submittedContext.submittedMessageId(), exception);
            throw exception;
        }
    }

    private void removeSubmittedMessageInTransaction(SubmittedMessageContext submittedContext) {
        executeInTransaction(() -> {
            submittedMessageRecorder.remove(submittedContext);
            return null;
        });
    }

    private <T> T executeInTransaction(Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }

    /** 실행 중인 외부 AI 호출도 인터럽트할 수 있도록 FutureTask와 완료 상태를 연결한다. */
    private <T> CompletableFuture<T> submitCancellableAsync(Callable<T> task) {
        CancellableCompletableFuture<T> result = new CancellableCompletableFuture<>();
        FutureTask<T> futureTask = new FutureTask<>(task) {
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
            CompletableFuture<Void> feedbackFuture
    ) {

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
