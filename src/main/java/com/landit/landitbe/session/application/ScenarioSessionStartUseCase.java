// 시나리오 세션 시작 유스케이스를 조율한다.
package com.landit.landitbe.session.application;

import com.landit.landitbe.auth.domain.UserProfile;
import com.landit.landitbe.auth.infrastructure.UserProfileRepository;
import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.learning.domain.UserScenarioProgress;
import com.landit.landitbe.learning.domain.UserScenarioProgressStatus;
import com.landit.landitbe.learning.infrastructure.UserScenarioProgressRepository;
import com.landit.landitbe.session.api.dto.SessionStartResponse;
import com.landit.landitbe.session.api.dto.SessionStartResponse.CurrentMessageResponse;
import com.landit.landitbe.session.api.dto.SessionStartResponse.SessionProgressResponse;
import com.landit.landitbe.session.domain.LearningSession;
import com.landit.landitbe.session.domain.ScenarioSession;
import com.landit.landitbe.session.domain.SessionHistory;
import com.landit.landitbe.session.domain.SessionHistoryMessage;
import com.landit.landitbe.session.domain.SessionType;
import com.landit.landitbe.session.infrastructure.LearningSessionRepository;
import com.landit.landitbe.session.infrastructure.ScenarioSessionRepository;
import com.landit.landitbe.session.infrastructure.ScenarioSessionLockRow;
import com.landit.landitbe.session.infrastructure.ScenarioSessionStartQueryRepository;
import com.landit.landitbe.session.infrastructure.ScenarioSessionStartRow;
import com.landit.landitbe.session.infrastructure.SessionHistoryMessageRepository;
import com.landit.landitbe.session.infrastructure.SessionHistoryRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ScenarioSessionStartUseCase {

    private static final String PREVIOUS_SCENARIO_NOT_COMPLETED = "PREVIOUS_SCENARIO_NOT_COMPLETED";

    // DB Repository는 현재 기준상 Port로 감싸지 않고
    // UseCase 트랜잭션에서 직접 조율한다.
    private final UserProfileRepository userProfileRepository;
    private final ScenarioSessionStartQueryRepository scenarioSessionStartQueryRepository;
    private final UserScenarioProgressRepository userScenarioProgressRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final ScenarioSessionRepository scenarioSessionRepository;
    private final SessionHistoryRepository sessionHistoryRepository;
    private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

    /** 선택한 시나리오로 학습 세션을 시작한다. */
    @Transactional
    public SessionStartResponse startScenarioSession(long userId, long scenarioId) {
        UserProfile userProfile = findActiveUser(userId);
        ScenarioSessionStartRow startRow = findStartRow(userId, scenarioId);
        assertPlayable(userId, startRow);

        LocalDateTime now = LocalDateTime.now();
        ensureProgress(userProfile, startRow, now);
        LearningSession learningSession = createLearningSession(userId, userProfile, startRow, now);
        CurrentMessageResponse currentMessage = null;
        if (startRow.firstSpeaker() == ConversationSpeaker.AI) {
            currentMessage = saveAiOpeningMessage(
                    learningSession.getId(),
                    userProfile,
                    startRow,
                    now
            );
        }

        return toStartResponse(learningSession, startRow, currentMessage);
    }

    private UserProfile findActiveUser(long userId) {
        // 같은 사용자의 동시 세션 시작 요청이 progress row 생성 구간을
        // 동시에 통과하지 못하도록 사용자 row를 잠근다.
        return userProfileRepository.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));
    }

    private ScenarioSessionStartRow findStartRow(long userId, long scenarioId) {
        return scenarioSessionStartQueryRepository
                .findStartRow(userId, scenarioId)
                .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));
    }

    private Long requireAiTutorId(UserProfile userProfile) {
        if (userProfile.getAiTutorId() == null) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "AI 튜터가 설정되지 않았습니다."
            );
        }
        return userProfile.getAiTutorId();
    }

    private void assertPlayable(long userId, ScenarioSessionStartRow startRow) {
        assertContentActive(startRow);
        assertPreviousScenarioCleared(userId, startRow);
    }

    /**
     * 카테고리 잠금과 시나리오 비활성 상태를 API 오류 코드로 변환한다.
     */
    private void assertContentActive(ScenarioSessionStartRow startRow) {
        if (inactive(startRow.categoryStatus())) {
            throw new ApiException(ErrorCode.CATEGORY_LOCKED);
        }
        if (inactive(startRow.scenarioStatus()) || inactive(startRow.variantStatus())) {
            throw new ApiException(ErrorCode.SCENARIO_LOCKED);
        }
    }

    /**
     * 같은 카테고리 안에서 displayOrder 기준 직전 시나리오
     * 완료 여부만 확인한다.
     */
    private void assertPreviousScenarioCleared(long userId, ScenarioSessionStartRow startRow) {
        Optional<ScenarioSessionLockRow> previousScenario = scenarioSessionStartQueryRepository
                .findPreviousScenarioLockRow(userId, startRow.scenarioId());
        if (previousScenario.isEmpty()) {
            return;
        }
        if (previousScenario.get().progressStatus() != UserScenarioProgressStatus.CLEARED) {
            throw new ApiException(
                    ErrorCode.SCENARIO_LOCKED,
                    PREVIOUS_SCENARIO_NOT_COMPLETED
            );
        }
    }

    /**
     * 최초 시작과 재시도를 같은 흐름으로 처리하되,
     * 기존 완료 성과는 유지한다.
     */
    private void ensureProgress(
            UserProfile userProfile,
            ScenarioSessionStartRow startRow,
            LocalDateTime startedAt
    ) {
        userScenarioProgressRepository
                .findByUserProfileIdAndScenarioIdAndTargetLocale(
                        userProfile.getId(),
                        startRow.scenarioId(),
                        userProfile.getTargetLocale()
                )
                .ifPresentOrElse(
                        progress -> progress.markStarted(startedAt),
                        () -> userScenarioProgressRepository.save(UserScenarioProgress.start(
                                userProfile.getId(),
                                startRow.scenarioId(),
                                userProfile.getTargetLocale(),
                                startedAt
                        ))
                );
    }

    private LearningSession createLearningSession(
            long userId,
            UserProfile userProfile,
            ScenarioSessionStartRow startRow,
            LocalDateTime startedAt
    ) {
        LearningSession learningSession = learningSessionRepository.save(
                LearningSession.startScenario(
                        userId,
                        requireAiTutorId(userProfile),
                        userProfile.getTargetLocale(),
                        userProfile.getBaseLocale(),
                        startedAt
                )
        );
        scenarioSessionRepository.save(ScenarioSession.start(
                learningSession.getId(),
                startRow.variantId()
        ));
        return learningSession;
    }

    /**
     * AI first 시나리오는 세션 시작과 동시에
     * 히스토리와 첫 AI 메시지를 저장한다.
     */
    private CurrentMessageResponse saveAiOpeningMessage(
            Long learningSessionId,
            UserProfile userProfile,
            ScenarioSessionStartRow startRow,
            LocalDateTime startedAt
    ) {
        assertAiOpeningMessageConfigured(startRow);
        SessionHistoryMessage message = saveAiOpeningHistoryMessage(
                learningSessionId,
                userProfile,
                startRow,
                startedAt
        );
        return toCurrentMessageResponse(message);
    }

    /** AI first 시작 데이터가 비어 있으면 콘텐츠 설정 오류로 본다. */
    private void assertAiOpeningMessageConfigured(ScenarioSessionStartRow startRow) {
        if (startRow.aiOpeningMessage() == null || startRow.aiOpeningMessage().isBlank()) {
            throw new ApiException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "AI 시작 메시지가 설정되지 않았습니다."
            );
        }
    }

    private SessionHistoryMessage saveAiOpeningHistoryMessage(
            Long learningSessionId,
            UserProfile userProfile,
            ScenarioSessionStartRow startRow,
            LocalDateTime startedAt
    ) {
        SessionHistory sessionHistory = sessionHistoryRepository.save(
                SessionHistory.startedScenario(
                        learningSessionId,
                        userProfile.getId(),
                        userProfile.getTargetLocale(),
                        userProfile.getBaseLocale(),
                        startedAt
                )
        );
        SessionHistoryMessage message = sessionHistoryMessageRepository.save(
                SessionHistoryMessage.aiOpening(
                        sessionHistory.getId(),
                        startRow.aiOpeningMessage(),
                        startRow.aiOpeningMessageTranslation(),
                        startRow.aiOpeningInnerThought(),
                        startRow.aiOpeningInnerThoughtType()
                )
        );
        return message;
    }

    private CurrentMessageResponse toCurrentMessageResponse(SessionHistoryMessage message) {
        return new CurrentMessageResponse(
                message.getId(),
                message.getTurnNumber(),
                message.getMessageSequence(),
                message.getRole().name(),
                message.getContent(),
                message.getTranslatedContent(),
                message.getInnerThought(),
                message.getInnerThoughtType() == null ? null : message.getInnerThoughtType().name()
        );
    }

    /**
     * firstSpeaker에 맞춰 AI 메시지 또는 USER first 시작 안내만 응답에 담는다.
     */
    private SessionStartResponse toStartResponse(
            LearningSession learningSession,
            ScenarioSessionStartRow startRow,
            CurrentMessageResponse currentMessage
    ) {
        String userOpeningInstruction = null;
        if (startRow.firstSpeaker() == ConversationSpeaker.USER) {
            userOpeningInstruction = startRow.userOpeningInstruction();
        }
        return new SessionStartResponse(
                learningSession.getId(),
                startRow.scenarioId(),
                SessionType.SCENARIO.name(),
                startRow.firstSpeaker().name(),
                userOpeningInstruction,
                startRow.ttsVoiceSetId(),
                currentMessage,
                new SessionProgressResponse(1, startRow.totalQuestionCount(), false)
        );
    }

    private boolean inactive(ActiveStatus status) {
        return status != ActiveStatus.ACTIVE;
    }
}
