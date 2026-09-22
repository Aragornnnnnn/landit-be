// 프리톡 사용자 발화의 턴 교정을 준비·확정하고 대화 기록 단위로 조회한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMessageFeedback;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.feedback.repository.FreeTalkMessageFeedbackRepository;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 턴 교정의 저장과 조회를 담당한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkMessageFeedbackService {

  private final FreeTalkMessageFeedbackRepository feedbackRepository;
  private final ConversationMessageService conversationMessageService;

  /**
   * 속마음 준비를 건 같은 트랜잭션에서 교정 판정을 기다리는 행을 만든다.
   *
   * <p>교정은 부가 기능이라 준비가 턴 확정을 막아서는 안 된다. 교정 도입 전에 저장되어 실패 행이 먼저 채워진 발화가 도입 뒤에 확정되면 새 행을 만들지 않고 그 행을
   * 준비 상태로 되돌린다. 대화 기록 ID는 호출자에게 받지 않고 발화에서 읽어 둘이 어긋나지 않게 한다.
   *
   * @param messageId 교정 대상 사용자 발화 ID
   * @throws org.springframework.transaction.IllegalTransactionStateException 턴 확정 트랜잭션 밖에서 호출했을 때
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void prepareCorrection(long messageId) {
    feedbackRepository
        .findBySessionHistoryMessageId(messageId)
        .ifPresentOrElse(
            FreeTalkMessageFeedback::restartPreparing,
            () ->
                feedbackRepository.save(
                    FreeTalkMessageFeedback.preparing(
                        messageId,
                        conversationMessageService.require(messageId).getSessionHistoryId())));
  }

  /**
   * 준비 상태인 교정에만 판정 결과를 반영한다. 같은 발화에 두 번 호출해도 한 번만 저장된다.
   *
   * @param messageId 교정 대상 사용자 발화 ID
   * @param correction 교정 판정 결과
   * @return 갱신된 row 수. 이미 판정이 끝난 교정이면 0
   */
  @Transactional
  public int completeIfPreparing(long messageId, FreeTalkTurnCorrection correction) {
    FreeTalkTurnCorrection.Sentence sentence = correction.sentence();
    return feedbackRepository.updateIfPreparing(
        messageId,
        correction.status(),
        correction.reactedToPartner(),
        sentence == null ? null : sentence.originalSentence(),
        sentence == null ? null : sentence.betterSentence(),
        sentence == null ? null : sentence.reason(),
        sentence == null ? null : sentence.mistakePattern(),
        ProcessingStatus.PREPARING);
  }

  /**
   * 준비 상태인 교정을 실패로 확정한다.
   *
   * @param messageId 교정 대상 사용자 발화 ID
   * @return 갱신된 row 수. 이미 판정이 끝난 교정이면 0
   */
  @Transactional
  public int failIfPreparing(long messageId) {
    return completeIfPreparing(messageId, FreeTalkTurnCorrection.failed());
  }

  /**
   * 대화 기록 하나의 교정을 사용자 발화 ID로 묶어 돌려준다.
   *
   * @param sessionHistoryId 대화 기록 ID
   * @return 사용자 발화 ID별 교정 판정. 교정 대상이 아닌 메시지는 들어 있지 않다
   */
  @Transactional(readOnly = true)
  public Map<Long, FreeTalkTurnCorrection> findBySessionHistoryId(long sessionHistoryId) {
    return feedbackRepository.findBySessionHistoryId(sessionHistoryId).stream()
        .collect(
            Collectors.toMap(
                FreeTalkMessageFeedback::getSessionHistoryMessageId,
                FreeTalkMessageFeedback::toCorrection));
  }
}
