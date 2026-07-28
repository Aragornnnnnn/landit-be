// 프리톡 발화와 종료 확인의 AI 호출 흐름을 조정한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingReason;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkResponseMode;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.session.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.session.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 프리톡 발화와 종료 확인의 AI 호출 흐름을 조정한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkMessageService {

  private final FreeTalkSubmittedMessageService submittedMessageService;
  private final AiFreeTalkClient aiFreeTalkClient;

  /** 사용자 발화를 처리하고 AI 후속 메시지 또는 종료 확인 상태를 반환한다. */
  public FreeTalkMessageSubmitResponse submit(
      long userId, long learningSessionId, FreeTalkMessageSubmitRequest request) {
    FreeTalkSubmittedMessageService.Reservation reservation =
        submittedMessageService.reserve(userId, learningSessionId, request);
    try {
      if (reservation.timeLimitReached()) {
        return submittedMessageService.finalizeTimeLimit(
            reservation,
            aiFreeTalkClient.generateClosing(
                closingRequest(reservation, AiFreeTalkClosingReason.TIME_LIMIT_REACHED)));
      }
      return submittedMessageService.finalizeTurn(
          reservation,
          aiFreeTalkClient.generateTurn(turnRequest(reservation, AiFreeTalkResponseMode.NORMAL)));
    } catch (RuntimeException exception) {
      submittedMessageService.compensate(reservation);
      throw exception;
    }
  }

  /** 종료 의사 확인에 대한 사용자의 선택을 처리한다. */
  public FreeTalkMessageSubmitResponse decideExit(
      long userId, long learningSessionId, FreeTalkExitDecisionRequest request) {
    FreeTalkSubmittedMessageService.DecisionReservation reservation =
        submittedMessageService.reserveDecision(
            userId, learningSessionId, request.submittedMessageId(), request.decision());
    try {
      if (reservation.decision() == FreeTalkExitDecision.END) {
        return submittedMessageService.finalizeEnd(
            reservation,
            aiFreeTalkClient.generateClosing(
                closingRequestForDecision(reservation, AiFreeTalkClosingReason.USER_CONFIRMED)));
      }
      return submittedMessageService.finalizeContinue(
          reservation,
          aiFreeTalkClient.generateTurn(
              turnRequestForDecision(
                  reservation, AiFreeTalkResponseMode.CONTINUE_AFTER_EXIT_DECLINED)));
    } catch (RuntimeException exception) {
      submittedMessageService.compensateDecision(reservation);
      throw exception;
    }
  }

  private AiFreeTalkTurnRequest turnRequest(
      FreeTalkSubmittedMessageService.Reservation reservation,
      AiFreeTalkResponseMode responseMode) {
    return new AiFreeTalkTurnRequest(
        reservation.freeTalkSessionId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.partner().displayName(),
        reservation.partner().accentLocale(),
        responseMode,
        reservation.firstUserTurn(),
        reservation.topic(),
        reservation.history());
  }

  private AiFreeTalkClosingRequest closingRequest(
      FreeTalkSubmittedMessageService.Reservation reservation,
      AiFreeTalkClosingReason closingReason) {
    return new AiFreeTalkClosingRequest(
        reservation.freeTalkSessionId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.partner().displayName(),
        reservation.partner().accentLocale(),
        closingReason,
        reservation.topic(),
        reservation.history());
  }

  private AiFreeTalkTurnRequest turnRequestForDecision(
      FreeTalkSubmittedMessageService.DecisionReservation reservation,
      AiFreeTalkResponseMode responseMode) {
    return new AiFreeTalkTurnRequest(
        reservation.freeTalkSessionId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.partner().displayName(),
        reservation.partner().accentLocale(),
        responseMode,
        false,
        reservation.topic(),
        reservation.history());
  }

  private AiFreeTalkClosingRequest closingRequestForDecision(
      FreeTalkSubmittedMessageService.DecisionReservation reservation,
      AiFreeTalkClosingReason closingReason) {
    return new AiFreeTalkClosingRequest(
        reservation.freeTalkSessionId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.partner().displayName(),
        reservation.partner().accentLocale(),
        closingReason,
        reservation.topic(),
        reservation.history());
  }
}
