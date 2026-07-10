// 로컬 개발과 테스트에서 사용할 결정적 AI 대체 클라이언트다.
package com.landit.landitbe.session.infrastructure.ai;

import com.landit.landitbe.common.domain.InnerThoughtType;
import com.landit.landitbe.session.application.port.AiClosingMessageRequest;
import com.landit.landitbe.session.application.port.AiClosingMessageResult;
import com.landit.landitbe.session.application.port.AiConversationClient;
import com.landit.landitbe.session.application.port.AiNextMessageRequest;
import com.landit.landitbe.session.application.port.AiNextMessageResult;
import com.landit.landitbe.session.domain.GoalCompletionStatus;
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
                "사용자가 답변을 이어줘서 다음 질문으로 자연스럽게 "
                        + "넘어가면 좋겠다.",
                InnerThoughtType.NORMAL,
                GoalCompletionStatus.PARTIAL
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
}
