// 같은 AI 응답으로 온 프리톡 속마음과 턴 교정을 한 경계에서 확정한다.

package com.landit.landitbe.feature.learning.freetalk.message.service;

import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.feedback.service.FreeTalkMessageFeedbackService;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 사용자 발화 한 턴의 비동기 AI 판정 결과(속마음과 턴 교정)를 함께 저장한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkTurnResultService {

  private final ConversationMessageService conversationMessageService;
  private final FreeTalkMessageFeedbackService messageFeedbackService;

  /**
   * 속마음과 턴 교정을 한 트랜잭션에서 확정한다.
   *
   * <p>둘은 각자 준비 상태일 때만 반영되어 한 턴에 한 번만 저장된다. 교정은 속마음의 처리 여부와 무관하게 반영을 시도한다. 속마음 폴링이 시간 초과로 속마음만 먼저
   * 실패 처리한 발화에 응답이 늦게 도착해도 교정은 버리지 않기 위함이다.
   *
   * @param messageId 사용자 발화 ID
   * @param innerThought 생성된 속마음
   * @param innerThoughtType 속마음 유형
   * @param correction 턴 교정 판정. 판정 실패({@code FAILED})여도 속마음은 완료로 저장한다. AI가 판정을 돌려주지 못한 실패면 교정은 준비
   *     상태로 남아 다시 시도된다
   */
  @Transactional
  public void complete(
      long messageId,
      String innerThought,
      InnerThoughtType innerThoughtType,
      FreeTalkTurnCorrection correction) {
    conversationMessageService.completeInnerThought(messageId, innerThought, innerThoughtType);
    messageFeedbackService.completeFirstAttempt(messageId, correction);
  }

  /**
   * AI 호출에 실패한 발화의 속마음을 실패로 확정한다. 함께 기다리던 턴 교정은 실패로 끝내지 않고 다음 시도를 기다리게 한다.
   *
   * <p>속마음은 대화 중에만 의미가 있어 다시 만들지 않는다. 교정은 세션이 끝난 뒤에 보이므로 늦더라도 만들어지는 쪽이 낫다.
   *
   * @param messageId 사용자 발화 ID
   */
  @Transactional
  public void fail(long messageId) {
    conversationMessageService.failInnerThought(messageId);
    messageFeedbackService.failFirstAttempt(messageId);
  }
}
