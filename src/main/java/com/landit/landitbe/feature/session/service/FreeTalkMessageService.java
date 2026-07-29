// 프리톡 발화와 종료 확인의 AI 호출 흐름을 조정한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingReason;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkResponseMode;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.session.domain.FreeTalkExitDecision;
import com.landit.landitbe.feature.session.dto.FreeTalkExitDecisionRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitRequest;
import com.landit.landitbe.feature.session.dto.FreeTalkMessageSubmitResponse;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/** 프리톡 발화와 종료 확인의 AI 호출 흐름을 조정한다. */
@Service
@Slf4j
public class FreeTalkMessageService {

  private final FreeTalkSubmittedMessageService submittedMessageService;
  private final AiFreeTalkClient aiFreeTalkClient;
  private final SessionMessageService sessionMessageService;
  private final TaskExecutor taskExecutor;

  FreeTalkMessageService(
      FreeTalkSubmittedMessageService submittedMessageService,
      AiFreeTalkClient aiFreeTalkClient,
      SessionMessageService sessionMessageService,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
    this.submittedMessageService = submittedMessageService;
    this.aiFreeTalkClient = aiFreeTalkClient;
    this.sessionMessageService = sessionMessageService;
    this.taskExecutor = taskExecutor;
  }

  /** 사용자 발화를 처리하고 AI 후속 메시지 또는 종료 확인 상태를 반환한다. */
  public FreeTalkMessageSubmitResponse submit(
      long userId, long learningSessionId, FreeTalkMessageSubmitRequest request) {
    FreeTalkMessageSubmitResponse replayedResponse =
        submittedMessageService.findCompletedResponse(userId, learningSessionId, request);
    if (replayedResponse != null) {
      return replayedResponse;
    }
    FreeTalkSubmittedMessageService.Reservation reservation =
        submittedMessageService.reserve(userId, learningSessionId, request);
    try {
      FreeTalkMessageSubmitResponse response;
      if (reservation.timeLimitReached()) {
        response =
            submittedMessageService.finalizeTimeLimit(
                reservation,
                aiFreeTalkClient.generateClosing(
                    closingRequest(reservation, AiFreeTalkClosingReason.TIME_LIMIT_REACHED)));
      } else {
        response =
            submittedMessageService.finalizeTurn(
                reservation,
                aiFreeTalkClient.generateTurn(
                    turnRequest(reservation, AiFreeTalkResponseMode.NORMAL)));
      }
      if (response.turnStatus()
          != com.landit.landitbe.feature.session.domain.FreeTalkTurnStatus
              .EXIT_CONFIRMATION_REQUIRED) {
        generateInnerThought(innerThoughtRequest(reservation));
      }
      return response;
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
      FreeTalkMessageSubmitResponse response;
      if (reservation.decision() == FreeTalkExitDecision.END) {
        response =
            submittedMessageService.finalizeEnd(
                reservation,
                aiFreeTalkClient.generateClosing(
                    closingRequestForDecision(
                        reservation, AiFreeTalkClosingReason.USER_CONFIRMED)));
      } else {
        response =
            submittedMessageService.finalizeContinue(
                reservation,
                aiFreeTalkClient.generateTurn(
                    turnRequestForDecision(
                        reservation, AiFreeTalkResponseMode.CONTINUE_AFTER_EXIT_DECLINED)));
      }
      generateInnerThought(innerThoughtRequest(reservation));
      return response;
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
        closingReason,
        reservation.topic(),
        reservation.history());
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest(
      FreeTalkSubmittedMessageService.Reservation reservation) {
    return new AiFreeTalkInnerThoughtRequest(
        reservation.freeTalkSessionId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.topic(),
        reservation.history());
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest(
      FreeTalkSubmittedMessageService.DecisionReservation reservation) {
    return new AiFreeTalkInnerThoughtRequest(
        reservation.freeTalkSessionId(),
        reservation.userMessageId(),
        reservation.history().getLast().turnNumber(),
        reservation.targetLocale(),
        reservation.baseLocale(),
        reservation.topic(),
        reservation.history());
  }

  private void generateInnerThought(AiFreeTalkInnerThoughtRequest request) {
    try {
      CompletableFuture.supplyAsync(
              () -> aiFreeTalkClient.generateInnerThought(request), taskExecutor)
          .whenCompleteAsync(
              (result, exception) -> {
                if (exception == null) {
                  sessionMessageService.completeInnerThought(
                      request.submittedMessageId(),
                      result.innerThought(),
                      result.innerThoughtType());
                  return;
                }
                log.warn(
                    "프리톡 속마음 생성에 실패했습니다. messageId={}", request.submittedMessageId(), exception);
                sessionMessageService.failInnerThought(request.submittedMessageId());
              },
              taskExecutor);
    } catch (RuntimeException exception) {
      log.warn("프리톡 속마음 작업을 시작하지 못했습니다. messageId={}", request.submittedMessageId(), exception);
      sessionMessageService.failInnerThought(request.submittedMessageId());
    }
  }
}
