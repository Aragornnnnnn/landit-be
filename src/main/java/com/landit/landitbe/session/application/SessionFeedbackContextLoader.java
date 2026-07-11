// 최종 피드백 생성에 필요한 완료 세션 컨텍스트를 불변 값으로 조회한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.common.domain.Locale;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.application.port.AiConversationHistoryMessage;
import com.landit.landitbe.session.application.port.AiMessageFeedbackEvaluationContext;
import com.landit.landitbe.session.application.port.AiScenarioContext;
import com.landit.landitbe.session.domain.ProcessingStatus;
import com.landit.landitbe.session.domain.SessionHistoryMessage;
import com.landit.landitbe.session.domain.SessionHistorySummaryFeedback;
import com.landit.landitbe.session.infrastructure.ScenarioSessionMessageContextRow;
import com.landit.landitbe.session.infrastructure.ScenarioSessionMessageQueryRepository;
import com.landit.landitbe.session.infrastructure.SessionHistoryMessageRepository;
import com.landit.landitbe.session.infrastructure.SessionHistoryRepository;
import com.landit.landitbe.session.infrastructure.SessionHistorySummaryFeedbackRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
class SessionFeedbackContextLoader {

    private final LearningSessionFinder learningSessionFinder;
    private final SessionHistoryRepository sessionHistoryRepository;
    private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
    private final ScenarioSessionMessageQueryRepository scenarioSessionMessageQueryRepository;
    private final SessionHistorySummaryFeedbackRepository sessionHistorySummaryFeedbackRepository;
    private final AiScenarioContextMapper aiScenarioContextMapper;

    /** 소유한 완료 시나리오 세션의 최종 피드백 입력을 불변 값으로 조회한다. */
    @Transactional(readOnly = true)
    public LoadedSessionFeedbackContext load(long userId, long sessionId) {
        var learningSession = learningSessionFinder.findOwnedCompleted(userId, sessionId);
        var sessionHistory = sessionHistoryRepository.findByLearningSessionId(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
        ScenarioSessionMessageContextRow scenarioContext = scenarioSessionMessageQueryRepository
                .findContextByLearningSessionId(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
        List<SessionHistoryMessage> historyMessages = sessionHistoryMessageRepository
                .findBySessionHistoryIdOrderByMessageSequenceAsc(sessionHistory.getId());

        // 이후 AI 호출과 응답 조립에 필요한 값을 트랜잭션 안에서 모두 읽어 불변 컨텍스트로 넘긴다.
        return new LoadedSessionFeedbackContext(
                learningSession.getId(),
                sessionHistory.getId(),
                learningSession.getTargetLocale(),
                learningSession.getBaseLocale(),
                aiScenarioContextMapper.map(scenarioContext),
                userMessages(historyMessages, scenarioContext),
                sessionHistorySummaryFeedbackRepository.findBySessionHistoryId(sessionHistory.getId())
                        .map(ExistingSummaryFeedbackContext::from)
        );
    }

    /** 세션 전체 히스토리에서 사용자 메시지와 평가 당시 기준 컨텍스트를 순서대로 구성한다. */
    private List<UserMessageContext> userMessages(
            List<SessionHistoryMessage> historyMessages,
            ScenarioSessionMessageContextRow scenarioContext
    ) {
        List<AiConversationHistoryMessage> conversationHistory = historyMessages.stream()
                .map(message -> new AiConversationHistoryMessage(
                        message.getId(),
                        message.getTurnNumber(),
                        message.getRole().name(),
                        message.getContent(),
                        message.getTranslatedContent()
                ))
                .toList();
        List<UserMessageContext> userMessages = new ArrayList<>();
        for (int index = 0; index < historyMessages.size(); index++) {
            SessionHistoryMessage message = historyMessages.get(index);
            if (message.getRole() != ConversationSpeaker.USER) {
                continue;
            }
            userMessages.add(new UserMessageContext(
                    message.getId(),
                    message.getTurnNumber(),
                    message.getContent(),
                    SessionMessageFeedbackRequester.evaluationContext(
                            scenarioContext,
                            conversationHistory.subList(0, index + 1)
                    )
            ));
        }
        if (userMessages.isEmpty()) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return List.copyOf(userMessages);
    }
}

record LoadedSessionFeedbackContext(
        Long sessionId,
        Long sessionHistoryId,
        Locale targetLocale,
        Locale baseLocale,
        AiScenarioContext scenario,
        List<UserMessageContext> userMessages,
        Optional<ExistingSummaryFeedbackContext> existingSummary
) {
}

record UserMessageContext(
        Long messageId,
        int turnNumber,
        String content,
        AiMessageFeedbackEvaluationContext evaluationContext
) {
}

record ExistingSummaryFeedbackContext(
        Long summaryFeedbackId
) {

    /** 완료 상태의 저장된 summary만 기존 최종 피드백 결과로 허용한다. */
    static ExistingSummaryFeedbackContext from(SessionHistorySummaryFeedback summaryFeedback) {
        if (summaryFeedback.getProcessingStatus() != ProcessingStatus.COMPLETED) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return new ExistingSummaryFeedbackContext(
                summaryFeedback.getId()
        );
    }
}
