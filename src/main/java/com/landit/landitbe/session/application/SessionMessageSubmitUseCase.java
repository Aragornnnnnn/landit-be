// 사용자 발화 제출과 AI 후속 메시지 저장 유스케이스를 처리한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.session.api.dto.SessionMessageSubmitRequest;
import com.landit.landitbe.session.api.dto.SessionMessageSubmitResponse;
import com.landit.landitbe.session.domain.SessionMessageInputType;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * UseCase: API 요청 하나의 전체 흐름과 트랜잭션 경계를 책임진다.
 * Recorder: 그 흐름 안에서 사용자 메시지 저장이나 AI 결과 저장 같은
 * 특정 기록 작업만 책임진다.
 */
@RequiredArgsConstructor
@Service
public class SessionMessageSubmitUseCase {

    private final SubmittedMessageRecorder submittedMessageRecorder;
    private final SessionMessageAiGenerator sessionMessageAiGenerator;
    private final GeneratedMessageRecorder generatedMessageRecorder;
    private final PlatformTransactionManager transactionManager;

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
        try {
            // 외부 AI 호출 중에는 DB 트랜잭션과 세션 row lock을 유지하지 않는다.
            SessionMessageAiGenerator.Generation generation =
                    sessionMessageAiGenerator.generate(toAiRequest(submittedContext));
            return executeInTransaction(() -> generatedMessageRecorder.record(submittedContext, generation));
        } catch (RuntimeException exception) {
            // AI 생성이나 결과 저장 실패 시 제출 메시지를 제거해 부분 히스토리를 막는다.
            removeSubmittedMessageInTransaction(submittedContext);
            throw exception;
        }
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

    private void removeSubmittedMessageInTransaction(SubmittedMessageContext submittedContext) {
        executeInTransaction(() -> {
            submittedMessageRecorder.remove(submittedContext);
            return null;
        });
    }

    private <T> T executeInTransaction(Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }
}
