// 로컬 개발과 테스트에서 사용할 결정적 AI 대체 클라이언트다.
package com.landit.landitbe.session.infrastructure.ai;

import com.landit.landitbe.common.domain.InnerThoughtType;
import com.landit.landitbe.session.application.port.AiClosingMessageRequest;
import com.landit.landitbe.session.application.port.AiClosingMessageResult;
import com.landit.landitbe.session.application.port.AiConversationClient;
import com.landit.landitbe.session.application.port.AiInnerThoughtRequest;
import com.landit.landitbe.session.application.port.AiInnerThoughtResult;
import com.landit.landitbe.session.application.port.AiMessageFeedbackRequest;
import com.landit.landitbe.session.application.port.AiMessageFeedbackResult;
import com.landit.landitbe.session.application.port.AiNextMessageRequest;
import com.landit.landitbe.session.application.port.AiNextMessageResult;
import com.landit.landitbe.session.application.port.AiSessionFeedbackRequest;
import com.landit.landitbe.session.application.port.AiSessionFeedbackResult;
import com.landit.landitbe.session.application.port.AiSessionMessageFeedbackResult;
import com.landit.landitbe.session.domain.FeedbackType;
import com.landit.landitbe.session.domain.GoalCompletionStatus;
import com.landit.landitbe.session.domain.ProcessingStatus;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "landit.ai",
        name = "client-mode",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalAiConversationClient implements AiConversationClient {

    /**
     * 다음 고정 질문을 그대로 반환해 로컬에서도
     * 대화 흐름을 확인할 수 있게 한다.
     */
    @Override
    public AiNextMessageResult generateNextMessage(AiNextMessageRequest request) {
        return new AiNextMessageResult(
                request.nextQuestion().questionEn(),
                request.nextQuestion().questionKo(),
                GoalCompletionStatus.PARTIAL
        );
    }

    /** 로컬 환경에서 사용할 사용자 메시지 속마음을 반환한다. */
    @Override
    public AiInnerThoughtResult generateInnerThought(AiInnerThoughtRequest request) {
        return new AiInnerThoughtResult(
                request.sessionId(),
                request.submittedMessageId(),
                "사용자가 답변을 이어줘서 다음 질문으로 자연스럽게 넘어가면 좋겠다.",
                InnerThoughtType.NORMAL
        );
    }

    /** 로컬 환경에서 사용할 기본 종료 메시지를 반환한다. */
    @Override
    public AiClosingMessageResult generateClosingMessage(AiClosingMessageRequest request) {
        return new AiClosingMessageResult(
                "Thanks for sharing. That was a good conversation.",
                "이야기해줘서 고마워. 좋은 대화였어.",
                "마지막까지 답해줘서 대화를 자연스럽게 마무리하면 좋겠다.",
                InnerThoughtType.NORMAL
        );
    }

    /** 로컬 환경에서 메시지별 피드백 요청 접수 상태를 반환한다. */
    @Override
    public AiMessageFeedbackResult requestMessageFeedback(AiMessageFeedbackRequest request) {
        return new AiMessageFeedbackResult(
                request.sessionId(),
                request.messageId(),
                ProcessingStatus.PREPARING
        );
    }

    /** 로컬 환경에서 사용할 고정 세션 최종 피드백을 반환한다. */
    @Override
    public AiSessionFeedbackResult generateSessionFeedback(AiSessionFeedbackRequest request) {
        return new AiSessionFeedbackResult(
                request.sessionId(),
                90,
                new BigDecimal("3.0"),
                "You clearly communicated your main idea.",
                "Keep practicing complete sentences with clear reasons.",
                request.expectedMessageIds().stream()
                        .map(messageId -> new AiSessionMessageFeedbackResult(
                                messageId,
                                FeedbackType.GOOD,
                                "한국어로 이유를 덧붙여 자연스럽게 말한 것과 비슷해요.",
                                null,
                                "Your message clearly communicates the main idea.",
                                null,
                                null,
                                "Your message clearly communicates the main idea."
                        ))
                        .toList()
        );
    }
}
