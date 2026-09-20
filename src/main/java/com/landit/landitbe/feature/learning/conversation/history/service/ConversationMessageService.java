// 세션 히스토리 메시지 Repository를 소유하고 메시지 상태 변경을 제공한다.

package com.landit.landitbe.feature.learning.conversation.history.service;

import com.landit.landitbe.feature.learning.conversation.domain.CharacterEmotion;
import com.landit.landitbe.feature.learning.conversation.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.domain.SessionMessageInputType;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.exception.SessionErrorCode;
import com.landit.landitbe.feature.learning.conversation.exception.SessionException;
import com.landit.landitbe.feature.learning.conversation.history.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.history.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import com.landit.landitbe.shared.exception.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 세션 히스토리 메시지 Repository를 소유하고 메시지 상태 변경을 제공한다. */
@Service
@RequiredArgsConstructor
public class ConversationMessageService {

  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;

  /**
   * 평가 저장 트랜잭션에서 부모 메시지를 먼저 잠근다.
   *
   * @param messageId 평가 대상 메시지 ID
   * @return 삭제되지 않은 메시지를 잠갔으면 true
   */
  @Transactional
  public boolean lockForFeedbackResult(long messageId) {
    return sessionHistoryMessageRepository.findByIdForFeedbackUpdate(messageId).isPresent();
  }

  /**
   * 세션 히스토리의 메시지를 순서대로 조회한다.
   *
   * @param sessionHistoryId 세션 히스토리 ID
   * @return 메시지 목록
   */
  public List<SessionHistoryMessageSnapshot> findAll(long sessionHistoryId) {
    return sessionHistoryMessageRepository
        .findBySessionHistoryIdOrderByMessageSequenceAsc(sessionHistoryId)
        .stream()
        .map(SessionHistoryMessageSnapshot::from)
        .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
  }

  /**
   * 메시지 ID로 메시지를 조회한다.
   *
   * @param messageId 메시지 ID
   * @return 조회한 메시지
   * @throws ApiException 메시지가 없을 때
   */
  public SessionHistoryMessageSnapshot require(long messageId) {
    return sessionHistoryMessageRepository
        .findById(messageId)
        .map(SessionHistoryMessageSnapshot::from)
        .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
  }

  /**
   * 세션 히스토리에 속한 메시지를 조회한다.
   *
   * @param messageId 메시지 ID
   * @param sessionHistoryId 세션 히스토리 ID
   * @return 조회한 메시지
   * @throws ApiException 메시지가 없을 때
   */
  public SessionHistoryMessageSnapshot requireInHistory(long messageId, long sessionHistoryId) {
    return sessionHistoryMessageRepository
        .findByIdAndSessionHistoryId(messageId, sessionHistoryId)
        .map(SessionHistoryMessageSnapshot::from)
        .orElseThrow(() -> new SessionException(SessionErrorCode.RESOURCE_NOT_FOUND));
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

  /** 재시도에서 복구된 평가만 실패 상태에서 되돌린다. */
  @Transactional
  public void retryFeedback(long messageId) {
    sessionHistoryMessageRepository.retryFeedback(
        messageId, ProcessingStatus.PREPARING, ProcessingStatus.FAILED);
  }

  /**
   * 메시지를 저장하고 조회 값을 반환한다.
   *
   * @param sessionHistoryId 대화 이력 ID
   * @param content 발화 본문
   * @param translatedContent 번역 본문
   * @param innerThought 속마음
   * @param innerThoughtType 속마음 종류
   * @return 조회 또는 변경한 메시지 값
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistoryMessageSnapshot recordAiOpening(
      Long sessionHistoryId,
      String content,
      String translatedContent,
      String innerThought,
      InnerThoughtType innerThoughtType) {
    return SessionHistoryMessageSnapshot.from(
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.aiOpening(
                sessionHistoryId, content, translatedContent, innerThought, innerThoughtType)));
  }

  /**
   * 메시지를 저장하고 조회 값을 반환한다.
   *
   * @param sessionHistoryId 대화 이력 ID
   * @param messageSequence 메시지 순서
   * @param turnNumber 대화 턴
   * @param content 발화 본문
   * @param inputType 입력 방식
   * @return 조회 또는 변경한 메시지 값
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistoryMessageSnapshot recordUser(
      Long sessionHistoryId,
      int messageSequence,
      int turnNumber,
      String content,
      SessionMessageInputType inputType) {
    return SessionHistoryMessageSnapshot.from(
        sessionHistoryMessageRepository.saveAndFlush(
            SessionHistoryMessage.user(
                sessionHistoryId, messageSequence, turnNumber, content, inputType)));
  }

  /**
   * 메시지를 저장하고 조회 값을 반환한다.
   *
   * @param sessionHistoryId 대화 이력 ID
   * @param messageSequence 메시지 순서
   * @param turnNumber 대화 턴
   * @param clientMessageId 재전송 식별자
   * @param content 발화 본문
   * @param inputType 입력 방식
   * @param utteranceDurationMs 발화 시간 밀리초
   * @return 조회 또는 변경한 메시지 값
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistoryMessageSnapshot recordFreeTalkUser(
      Long sessionHistoryId,
      int messageSequence,
      int turnNumber,
      String clientMessageId,
      String content,
      SessionMessageInputType inputType,
      long utteranceDurationMs) {
    return SessionHistoryMessageSnapshot.from(
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.freeTalkUser(
                sessionHistoryId,
                messageSequence,
                turnNumber,
                clientMessageId,
                content,
                inputType,
                utteranceDurationMs)));
  }

  /**
   * 메시지를 저장하고 조회 값을 반환한다.
   *
   * @param sessionHistoryId 대화 이력 ID
   * @param messageSequence 메시지 순서
   * @param turnNumber 대화 턴
   * @param content 발화 본문
   * @param translatedContent 번역 본문
   * @param emotion 캐릭터 감정
   * @return 조회 또는 변경한 메시지 값
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistoryMessageSnapshot recordFreeTalkAi(
      Long sessionHistoryId,
      int messageSequence,
      int turnNumber,
      String content,
      String translatedContent,
      CharacterEmotion emotion) {
    return SessionHistoryMessageSnapshot.from(
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.freeTalkAi(
                sessionHistoryId,
                messageSequence,
                turnNumber,
                content,
                translatedContent,
                emotion)));
  }

  /**
   * 메시지를 저장하고 조회 값을 반환한다.
   *
   * @param sessionHistoryId 대화 이력 ID
   * @param messageSequence 메시지 순서
   * @param turnNumber 대화 턴
   * @param content 발화 본문
   * @param translatedContent 번역 본문
   * @return 조회 또는 변경한 메시지 값
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistoryMessageSnapshot recordAiGenerated(
      Long sessionHistoryId,
      int messageSequence,
      int turnNumber,
      String content,
      String translatedContent) {
    return SessionHistoryMessageSnapshot.from(
        sessionHistoryMessageRepository.save(
            SessionHistoryMessage.aiGenerated(
                sessionHistoryId, messageSequence, turnNumber, content, translatedContent)));
  }

  /**
   * 호출자가 검증한 메시지의 처리 상태를 같은 트랜잭션에서 기록한다.
   *
   * @param messageId 이미 검증한 메시지 ID
   * @param clientMessageId 재전송 식별자
   * @param until 생성 선점 만료
   * @return 생성 선점 식별자
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public String claimScenarioGeneration(
      long messageId, String clientMessageId, LocalDateTime until) {
    return requireEntity(messageId).claimScenarioGeneration(clientMessageId, until);
  }

  /**
   * 호출자가 검증한 메시지의 처리 상태를 같은 트랜잭션에서 기록한다.
   *
   * @param messageId 이미 검증한 메시지 ID
   * @param token 생성 시도 식별자
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void releaseScenarioAttempt(long messageId, String token) {
    requireEntity(messageId).releaseScenarioAttempt(token);
  }

  /**
   * 호출자가 검증한 메시지의 처리 상태를 같은 트랜잭션에서 기록한다.
   *
   * @param messageId 이미 검증한 메시지 ID
   * @param response 재전송 응답
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordScenarioResponse(long messageId, String response) {
    requireEntity(messageId).recordScenarioResponse(response);
  }

  /**
   * 호출자가 검증한 메시지의 처리 상태를 같은 트랜잭션에서 기록한다.
   *
   * @param messageId 이미 검증한 메시지 ID
   * @param innerThought 속마음
   * @param innerThoughtType 속마음 종류
   * @return 조회 또는 변경한 메시지 값
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistoryMessageSnapshot recordInnerThought(
      long messageId, String innerThought, InnerThoughtType innerThoughtType) {
    SessionHistoryMessage message = requireEntity(messageId);
    message.recordInnerThought(innerThought, innerThoughtType);
    return SessionHistoryMessageSnapshot.from(message);
  }

  /**
   * 호출자가 검증한 메시지의 처리 상태를 같은 트랜잭션에서 기록한다.
   *
   * @param messageId 이미 검증한 메시지 ID
   * @return 조회 또는 변경한 메시지 값
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistoryMessageSnapshot markFeedbackFailed(long messageId) {
    SessionHistoryMessage message = requireEntity(messageId);
    message.markFeedbackFailed();
    return SessionHistoryMessageSnapshot.from(message);
  }

  /**
   * 호출자가 검증한 메시지의 처리 상태를 같은 트랜잭션에서 기록한다.
   *
   * @param messageId 이미 검증한 메시지 ID
   * @param status 턴 처리 결과
   * @param prepareInnerThought 속마음 생성 준비 여부
   * @return 조회 또는 변경한 메시지 값
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SessionHistoryMessageSnapshot recordFreeTalkTurnResult(
      long messageId, FreeTalkTurnStatus status, boolean prepareInnerThought) {
    SessionHistoryMessage message = requireEntity(messageId);
    message.recordFreeTalkTurnStatus(status);
    if (prepareInnerThought) {
      message.prepareInnerThought();
    }
    return SessionHistoryMessageSnapshot.from(message);
  }

  /**
   * 존재하는 메시지의 조회 상태를 제공한다.
   *
   * @param messageId 이미 검증한 메시지 ID
   * @return 조회 또는 변경한 메시지 값
   */
  public Optional<SessionHistoryMessageSnapshot> findMessage(long messageId) {
    return sessionHistoryMessageRepository
        .findById(messageId)
        .map(SessionHistoryMessageSnapshot::from);
  }

  /**
   * 존재하는 메시지의 조회 상태를 제공한다.
   *
   * @param messageId 이미 검증한 메시지 ID
   * @param historyId 대화 이력 ID
   * @return 조회 또는 변경한 메시지 값
   */
  public Optional<SessionHistoryMessageSnapshot> findMessageInHistory(
      long messageId, long historyId) {
    return sessionHistoryMessageRepository
        .findByIdAndSessionHistoryId(messageId, historyId)
        .map(SessionHistoryMessageSnapshot::from);
  }

  /**
   * 존재하는 메시지의 조회 상태를 제공한다.
   *
   * @param historyId 대화 이력 ID
   * @param clientMessageId 재전송 식별자
   * @return 조회 또는 변경한 메시지 값
   */
  public Optional<SessionHistoryMessageSnapshot> findByClientMessage(
      long historyId, String clientMessageId) {
    return sessionHistoryMessageRepository
        .findBySessionHistoryIdAndClientMessageId(historyId, clientMessageId)
        .map(SessionHistoryMessageSnapshot::from);
  }

  /**
   * 시작 실패로 생성한 메시지를 이력 삭제 전에 제거한다.
   *
   * @param historyId 대화 이력 ID
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void deleteHistoryMessages(long historyId) {
    sessionHistoryMessageRepository.deleteAll(
        sessionHistoryMessageRepository.findBySessionHistoryIdOrderByMessageSequenceAsc(historyId));
    sessionHistoryMessageRepository.flush();
  }

  private SessionHistoryMessage requireEntity(long messageId) {
    return sessionHistoryMessageRepository
        .findById(messageId)
        .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
  }
}
