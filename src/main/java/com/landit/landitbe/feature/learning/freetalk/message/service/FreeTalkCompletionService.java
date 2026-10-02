// 프리톡 직접 완료를 확정하고 교정과 기존 완료 후속 작업을 비동기로 시작한다.

package com.landit.landitbe.feature.learning.freetalk.message.service;

import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.expression.service.FreeTalkExpressionGenerationDispatcher;
import com.landit.landitbe.feature.learning.freetalk.feedback.service.FreeTalkCorrectionRequestService;
import com.landit.landitbe.feature.learning.freetalk.memory.service.FreeTalkMemoryGenerationDispatchService;
import com.landit.landitbe.shared.observability.FailureObservation;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

/** 직접 완료 트랜잭션이 커밋된 뒤 기존 생성 서비스를 실행한다. */
@Service
public class FreeTalkCompletionService {

  private final FreeTalkSubmittedMessageService submittedMessageService;
  private final FreeTalkExpressionGenerationDispatcher expressionDispatcher;
  private final FreeTalkMemoryGenerationDispatchService memoryDispatcher;
  private final FreeTalkCorrectionRequestService correctionRequestService;
  private final FreeTalkTurnResultService turnResultService;
  private final AiFreeTalkClient aiClient;
  private final TaskExecutor taskExecutor;

  FreeTalkCompletionService(
      FreeTalkSubmittedMessageService submittedMessageService,
      FreeTalkExpressionGenerationDispatcher expressionDispatcher,
      FreeTalkMemoryGenerationDispatchService memoryDispatcher,
      FreeTalkCorrectionRequestService correctionRequestService,
      FreeTalkTurnResultService turnResultService,
      AiFreeTalkClient aiClient,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
    this.submittedMessageService = submittedMessageService;
    this.expressionDispatcher = expressionDispatcher;
    this.memoryDispatcher = memoryDispatcher;
    this.correctionRequestService = correctionRequestService;
    this.turnResultService = turnResultService;
    this.aiClient = aiClient;
    this.taskExecutor = taskExecutor;
  }

  /**
   * 사용자 요청으로 세션을 완료하고 미완료 턴 교정·맞춤 표현·장기기억 생성을 시작한다.
   *
   * <p>요약 총평은 기존 summary 조회에서 교정 완료를 기다린 뒤 계산한다. 반복 요청은 후속 작업을 중복 등록하지 않는다.
   *
   * @param userId 요청 사용자 ID
   * @param sessionId 프리톡 학습 세션 ID
   * @throws com.landit.landitbe.shared.exception.ApiException 세션이 없거나 소유자가 아니거나 중단된 상태일 때
   */
  public void complete(long userId, long sessionId) {
    List<Long> corrections = submittedMessageService.completeDirectly(userId, sessionId);
    if (corrections == null) {
      return;
    }
    for (long messageId : corrections) {
      dispatchCorrection(messageId);
    }
    expressionDispatcher.dispatch(sessionId);
    memoryDispatcher.dispatch(sessionId);
  }

  private void dispatchCorrection(long messageId) {
    try {
      taskExecutor.execute(() -> completeCorrection(messageId));
    } catch (TaskRejectedException exception) {
      turnResultService.fail(messageId);
    }
  }

  private void completeCorrection(long messageId) {
    try {
      var request = correctionRequestService.rebuild(messageId).orElseThrow();
      var result = aiClient.generateInnerThought(request);
      turnResultService.complete(
          messageId, result.innerThought(), result.innerThoughtType(), result.correction());
    } catch (RuntimeException exception) {
      FailureObservation.failed(
          "direct_completion_correction", "generation", "result_missing", exception);
      turnResultService.fail(messageId);
    }
  }
}
