// 대화 진행에 필요한 AI 서버 호출을 추상화한다.
package com.landit.landitbe.session.application.port;

public interface AiConversationClient {

    /** 다음 AI 메시지를 생성한다. */
    AiNextMessageResult generateNextMessage(AiNextMessageRequest request);

    /** 대화 종료 메시지를 생성한다. */
    AiClosingMessageResult generateClosingMessage(AiClosingMessageRequest request);
}
