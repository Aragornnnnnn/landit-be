// 세션 히스토리 메시지 Repository를 소유하고 메시지 상태 변경을 제공한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 세션 히스토리 메시지 Repository를 소유하고 메시지 상태 변경을 제공한다. */
@Service
@RequiredArgsConstructor
public class SessionMessageService {

  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

  /** 세션 히스토리의 메시지를 순서대로 조회한다. */
  public List<SessionHistoryMessage> findAll(long sessionHistoryId) {
    return sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
        sessionHistoryId);
  }

  /** 메시지 ID로 메시지를 조회한다. */
  public SessionHistoryMessage require(long messageId) {
    return sessionHistoryMessageRepository
        .findById(messageId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  /** 세션 히스토리에 속한 메시지를 조회한다. */
  public SessionHistoryMessage requireInHistory(long messageId, long sessionHistoryId) {
    return sessionHistoryMessageRepository
        .findByIdAndSessionHistoryId(messageId, sessionHistoryId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  /** 메시지를 저장한다. */
  public SessionHistoryMessage save(SessionHistoryMessage message) {
    return sessionHistoryMessageRepository.save(message);
  }

  /** 메시지를 저장하고 즉시 반영한다. */
  public SessionHistoryMessage saveAndFlush(SessionHistoryMessage message) {
    return sessionHistoryMessageRepository.saveAndFlush(message);
  }

  /** 메시지를 삭제한다. */
  public void delete(SessionHistoryMessage message) {
    sessionHistoryMessageRepository.delete(message);
  }

  /** 존재하는 메시지를 ID로 삭제하고 즉시 반영한다. */
  public void deleteIfExists(long messageId) {
    sessionHistoryMessageRepository
        .findById(messageId)
        .ifPresent(sessionHistoryMessageRepository::delete);
    sessionHistoryMessageRepository.flush();
  }

  /** 특정 발화자의 메시지 수를 조회한다. */
  public long countByRole(long sessionHistoryId, ConversationSpeaker role) {
    return sessionHistoryMessageRepository.countBySessionHistoryIdAndRole(sessionHistoryId, role);
  }

  /** 준비 중인 속마음 결과를 완료 처리한다. */
  @Transactional
  public int completeInnerThought(
      long messageId, String innerThought, InnerThoughtType innerThoughtType) {
    return sessionHistoryMessageRepository.completeInnerThoughtIfPreparing(
        messageId,
        innerThought,
        innerThoughtType,
        ProcessingStatus.COMPLETED,
        ProcessingStatus.PREPARING);
  }

  /** 준비 중인 속마음 처리를 실패로 변경한다. */
  @Transactional
  public int failInnerThought(long messageId) {
    return sessionHistoryMessageRepository.markInnerThoughtFailedIfPreparing(
        messageId, ProcessingStatus.FAILED, ProcessingStatus.PREPARING);
  }

  /** 준비 중인 메시지 피드백 처리를 실패로 변경한다. */
  @Transactional
  public int failFeedback(long messageId) {
    return sessionHistoryMessageRepository.markFeedbackFailedIfPreparing(
        messageId, ProcessingStatus.FAILED, ProcessingStatus.PREPARING);
  }

  /** 준비 중인 메시지 피드백을 완료 상태로 변경한다. */
  @Transactional
  public int completeFeedback(List<Long> messageIds) {
    return sessionHistoryMessageRepository.markFeedbackCompletedIfPreparing(
        messageIds, ProcessingStatus.COMPLETED, ProcessingStatus.PREPARING);
  }
}
