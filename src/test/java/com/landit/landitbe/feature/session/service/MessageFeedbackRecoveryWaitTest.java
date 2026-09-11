// 피드백 복구 대기가 제한 시간과 서버 종료를 존중하는지 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.client.ai.AiMessageFeedbackEvaluationContext;
import com.landit.landitbe.feature.session.client.ai.AiMessageFeedbackEvaluationContextType;
import com.landit.landitbe.feature.session.client.ai.AiScenarioContext;
import com.landit.landitbe.feature.session.domain.MessageFeedbackWork;
import com.landit.landitbe.feature.session.repository.MessageFeedbackWorkRepository;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

class MessageFeedbackRecoveryWaitTest {
  private final MessageFeedbackWorkRepository repository =
      mock(MessageFeedbackWorkRepository.class);
  private final LoadedSessionFeedbackContext context =
      new LoadedSessionFeedbackContext(
          100L,
          101L,
          Locale.EN,
          Locale.KR,
          com.landit.landitbe.feature.content.domain.ContentLearningLevel.LEVEL_4_TO_5,
          new AiScenarioContext(1L, "test", "test", "test", "friend", "KOREAN_LEARNER"),
          List.of(
              new UserMessageContext(
                  200L,
                  1,
                  "Hello",
                  new AiMessageFeedbackEvaluationContext(
                      AiMessageFeedbackEvaluationContextType.AI_MESSAGE, "Hi", "안녕"),
                  com.landit.landitbe.feature.content.domain.ResponseDemand.HIGH,
                  List.of())),
          Optional.empty());

  @Test
  void queuedOrLeasedWorkCannotHoldTheRequestForever() {
    when(repository.findAllById(List.of(200L)))
        .thenReturn(List.of(new MessageFeedbackWork(200L, 100L, "{}", LocalDateTime.now())));
    MessageFeedbackWorkService service = service(task -> {});
    assertTimeoutPreemptively(
        Duration.ofSeconds(1),
        () ->
            assertThatThrownBy(() -> service.awaitRecovery(context, Duration.ofMillis(25)))
                .isInstanceOfSatisfying(
                    ApiException.class,
                    error ->
                        assertThat(error.getErrorCode())
                            .isEqualTo(ErrorCode.FEEDBACK_GENERATION_FAILED)));
  }

  @Test
  void shutdownInterruptionIsPreserved() {
    when(repository.findAllById(List.of(200L))).thenReturn(List.of());
    MessageFeedbackWorkService service = service(task -> {});
    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(() -> service.awaitRecovery(context, Duration.ofSeconds(1)))
          .isInstanceOf(ApiException.class);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void stoppedExecutorReturnsTheExistingUnavailableError() {
    MessageFeedbackWorkService service =
        service(
            task -> {
              throw new TaskRejectedException("stopped");
            });
    assertThatThrownBy(() -> service.awaitRecovery(context, Duration.ofSeconds(1)))
        .isInstanceOfSatisfying(
            ApiException.class,
            error ->
                assertThat(error.getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_GENERATION_FAILED));
  }

  private MessageFeedbackWorkService service(TaskExecutor executor) {
    Duration timeout = Duration.ofSeconds(1);
    return new MessageFeedbackWorkService(
        repository,
        mock(AiConversationClient.class),
        mock(SessionMessageService.class),
        new JsonMapper(),
        Clock.systemUTC(),
        new AiClientProperties(
            "http://localhost", "local", "test", timeout, timeout, timeout, timeout),
        mock(PlatformTransactionManager.class),
        executor,
        mock(LearningSessionService.class));
  }
}
