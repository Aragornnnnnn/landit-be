// 속마음 후처리 실행기가 요청을 거절할 때 실패 상태와 관측을 검증한다.

package com.landit.landitbe.feature.learning.scenario.session.message.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.scenario.assessment.service.SessionLevelAssessmentGenerationService;
import com.landit.landitbe.feature.learning.scenario.session.innerthought.client.ai.AiInnerThoughtResult;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.observability.FailureObservation;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.PlatformTransactionManager;

class SessionMessageSubmitDispatchTest {
  private final ConversationMessageService messages = mock(ConversationMessageService.class);
  private final Logger logger = (Logger) LoggerFactory.getLogger(FailureObservation.class);
  private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

  @BeforeEach
  void startLogs() {
    logs.start();
    logger.addAppender(logs);
  }

  @AfterEach
  void stopLogs() {
    logger.detachAppender(logs);
    logs.stop();
  }

  @Test
  void rejectedContinuationMarksInnerThoughtFailed() {
    service()
        .recordInnerThoughtAfterMessageGeneration(
            42L, CompletableFuture.completedFuture(mock(AiInnerThoughtResult.class)));

    verify(messages).failInnerThought(42L);
    assertThat(errorReasons()).containsExactly("executor_unavailable");
  }

  @Test
  void failedStateStorageIsObservedSeparately() {
    when(messages.failInnerThought(42L)).thenThrow(new IllegalStateException("secret-storage"));

    service()
        .recordInnerThoughtAfterMessageGeneration(
            42L, CompletableFuture.completedFuture(mock(AiInnerThoughtResult.class)));

    assertThat(errorReasons()).containsExactly("executor_unavailable", "storage_failed");
    assertThat(logs.list)
        .noneSatisfy(event -> assertThat(event.getFormattedMessage()).contains("secret-storage"));
  }

  private SessionMessageSubmitService service() {
    TaskExecutor rejected =
        task -> {
          throw new TaskRejectedException("queue full");
        };
    return new SessionMessageSubmitService(
        mock(SubmittedMessageService.class),
        mock(SessionMessageAiGenerator.class),
        mock(SessionInnerThoughtGenerator.class),
        messages,
        mock(SessionMessageFeedbackRequester.class),
        mock(SessionLevelAssessmentGenerationService.class),
        mock(GeneratedMessageService.class),
        mock(UserProfileService.class),
        mock(PlatformTransactionManager.class),
        rejected);
  }

  private java.util.List<String> errorReasons() {
    return logs.list.stream()
        .filter(event -> event.getLevel() == Level.ERROR)
        .map(event -> event.getMDCPropertyMap().get("reason"))
        .toList();
  }
}
