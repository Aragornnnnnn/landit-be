// 세션 히스토리 메시지 Repository를 소유하고 메시지 상태 변경을 제공한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.exception.SessionException;
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

  /**
   * 세션 히스토리의 메시지를 순서대로 조회한다.
   *
   * @param sessionHistoryId 세션 히스토리 ID
   * @return 메시지 목록
   */
  public List<SessionHistoryMessage> findAll(long sessionHistoryId) {
    return sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(
        sessionHistoryId);
  }

  /**
   * 메시지 ID로 메시지를 조회한다.
   *
   * @param messageId 메시지 ID
   * @return 조회한 메시지
   * @throws ApiException 메시지가 없을 때
   */
  public SessionHistoryMessage require(long messageId) {
    return sessionHistoryMessageRepository
        .findById(messageId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  /**
   * 세션 히스토리에 속한 메시지를 조회한다.
   *
   * @param messageId 메시지 ID
   * @param sessionHistoryId 세션 히스토리 ID
   * @return 조회한 메시지
   * @throws ApiException 메시지가 없을 때
   */
  public SessionHistoryMessage requireInHistory(long messageId, long sessionHistoryId) {
    return sessionHistoryMessageRepository
        .findByIdAndSessionHistoryId(messageId, sessionHistoryId)
        .orElseThrow(() -> new SessionException(SessionErrorCode.RESOURCE_NOT_FOUND));
  }

  /**
   * 메시지를 저장한다.
   *
   * @param message 저장할 메시지
   * @return 저장된 메시지
   */
  public SessionHistoryMessage save(SessionHistoryMessage message) {
    return sessionHistoryMessageRepository.save(message);
  }

  /**
   * 메시지를 저장하고 즉시 반영한다.
   *
   * @param message 저장할 메시지
   * @return 저장된 메시지
   */
  public SessionHistoryMessage saveAndFlush(SessionHistoryMessage message) {
    return sessionHistoryMessageRepository.saveAndFlush(message);
  }

  /**
   * 메시지를 삭제한다.
   *
   * @param message 삭제할 메시지
   */
  public void delete(SessionHistoryMessage message) {
    sessionHistoryMessageRepository.delete(message);
  }

  /**
   * 존재하는 메시지를 ID로 삭제하고 즉시 반영한다.
   *
   * @param messageId 삭제할 메시지 ID
   */
  public void deleteIfExists(long messageId) {
    sessionHistoryMessageRepository
        .findById(messageId)
        .ifPresent(sessionHistoryMessageRepository::delete);
    sessionHistoryMessageRepository.flush();
  }

  /**
   * 특정 발화자의 메시지 수를 조회한다.
   *
   * @param sessionHistoryId 세션 히스토리 ID
   * @param role 발화자 역할
   * @return 조건에 맞는 메시지 수
   */
  public long countByRole(long sessionHistoryId, ConversationSpeaker role) {
    return sessionHistoryMessageRepository.countBySessionHistoryIdAndRole(sessionHistoryId, role);
  }

  /**
   * 준비 중인 속마음 결과를 완료 처리한다.
   *
   * @param messageId 메시지 ID
   * @param innerThought 생성된 속마음
   * @param innerThoughtType 속마음 유형
   * @return 갱신된 row 수
   */
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

  /**
   * 준비 중인 속마음 처리를 실패로 변경한다.
   *
   * @param messageId 메시지 ID
   * @return 갱신된 row 수
   */
  @Transactional
  public int failInnerThought(long messageId) {
    return sessionHistoryMessageRepository.markInnerThoughtFailedIfPreparing(
        messageId, ProcessingStatus.FAILED, ProcessingStatus.PREPARING);
  }

  /**
   * 준비 중인 메시지 피드백 처리를 실패로 변경한다.
   *
   * @param messageId 메시지 ID
   * @return 갱신된 row 수
   */
  @Transactional
  public int failFeedback(long messageId) {
    return sessionHistoryMessageRepository.markFeedbackFailedIfPreparing(
        messageId, ProcessingStatus.FAILED, ProcessingStatus.PREPARING);
  }

  /**
   * 준비 중인 메시지 피드백을 완료 상태로 변경한다.
   *
   * @param messageIds 완료할 메시지 ID 목록
   * @return 갱신된 row 수
   */
  @Transactional
  public int completeFeedback(List<Long> messageIds) {
    return sessionHistoryMessageRepository.markFeedbackCompletedIfPreparing(
        messageIds, ProcessingStatus.COMPLETED, ProcessingStatus.PREPARING);
  }
}
