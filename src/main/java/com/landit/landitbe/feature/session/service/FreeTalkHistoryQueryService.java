// 완료된 프리톡의 목록과 상세 기록을 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionDetailResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionListResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 완료된 프리톡의 목록과 상세 기록을 조회한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkHistoryQueryService {

  private final LearningSessionRepository learningSessionRepository;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

  /** 완료 프리톡을 최신순 페이지로 조회한다. */
  @Transactional(readOnly = true)
  public FreeTalkSessionListResponse getSessions(long userId, int page, int size) {
    Page<FreeTalkSession> sessions =
        freeTalkSessionRepository.findCompletedByUserProfileId(userId, PageRequest.of(page, size));
    List<FreeTalkSessionListResponse.Item> items =
        sessions.getContent().stream().map(this::toListItem).toList();
    return new FreeTalkSessionListResponse(items, page, size, sessions.hasNext());
  }

  /** 사용자가 소유한 완료 프리톡의 상세 대화를 조회한다. */
  @Transactional(readOnly = true)
  public FreeTalkSessionDetailResponse getSession(long userId, long learningSessionId) {
    CompletedSession completedSession = requireCompleted(userId, learningSessionId);
    FreeTalkSession session = completedSession.freeTalkSession();
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    List<FreeTalkSessionDetailResponse.Message> messages =
        sessionHistoryMessageRepository
            .findBySessionHistoryIdOrderByMessageSequenceAsc(history.getId())
            .stream()
            .map(
                message ->
                    new FreeTalkSessionDetailResponse.Message(
                        message.getId(),
                        message.getTurnNumber(),
                        message.getMessageSequence(),
                        message.getRole().name(),
                        message.getContent(),
                        message.getTranslatedContent(),
                        message.getEmotion(),
                        message.getInnerThought(),
                        message.getInnerThoughtType()))
            .toList();
    return new FreeTalkSessionDetailResponse(
        learningSessionId,
        session.getTitle(),
        completedSession.learningSession().getStartedAt(),
        completedSession.learningSession().getEndedAt(),
        session.getAccumulatedSpeakingDurationMs(),
        messages);
  }

  private FreeTalkSessionListResponse.Item toListItem(FreeTalkSession session) {
    LearningSession learningSession =
        learningSessionRepository.findById(session.getLearningSessionId()).orElseThrow();
    return new FreeTalkSessionListResponse.Item(
        learningSession.getId(),
        session.getTitle(),
        learningSession.getStartedAt(),
        learningSession.getEndedAt(),
        session.getAccumulatedSpeakingDurationMs());
  }

  private CompletedSession requireCompleted(long userId, long learningSessionId) {
    LearningSession learningSession =
        learningSessionRepository
            .findById(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    if (!Long.valueOf(userId).equals(learningSession.getUserProfileId())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED
        || freeTalkSession.getConversationStatus() != FreeTalkConversationStatus.COMPLETED) {
      throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
    }
    return new CompletedSession(learningSession, freeTalkSession);
  }

  private record CompletedSession(
      LearningSession learningSession, FreeTalkSession freeTalkSession) {}
}
