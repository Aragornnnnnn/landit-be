// 피드백 재시도·최종 실패·저장 결함의 관측 경계를 검증한다.

package com.landit.landitbe.feature.learning.scenario.session.message.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.scenario.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackRequest;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.client.ai.AiMessageFeedbackResult;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.domain.MessageFeedbackWork;
import com.landit.landitbe.feature.learning.scenario.session.message.feedback.repository.MessageFeedbackWorkRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.landit.landitbe.shared.observability.FailureObservation;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

class MessageFeedbackObservationTest {
  private final MessageFeedbackWorkRepository repository =
      mock(MessageFeedbackWorkRepository.class);
  private final AiConversationClient client = mock(AiConversationClient.class);
  private final ConversationMessageService messages = mock(ConversationMessageService.class);
  private final Logger logger = (Logger) LoggerFactory.getLogger(FailureObservation.class);
  private final ListAppender<ILoggingEvent> appender =
      new ListAppender<>() {
        @Override
        protected void append(ILoggingEvent event) {
          event.prepareForDeferredProcessing();
          super.append(event);
        }
      };
  private final JsonMapper mapper = new JsonMapper();
  private final AiMessageFeedbackRequest request =
      new AiMessageFeedbackRequest(1L, 2L, 1, 1, null, null, "private-message");
  private final MessageFeedbackWork work =
      new MessageFeedbackWork(2L, 1L, mapper.writeValueAsString(request), LocalDateTime.now());
  private final Duration timeout = Duration.ofSeconds(1);
  private final MessageFeedbackWorkService service =
      new MessageFeedbackWorkService(
          repository,
          client,
          messages,
          mapper,
          Clock.systemUTC(),
          new AiClientProperties(
              "http://localhost", "local", "test", timeout, timeout, timeout, timeout),
          mock(PlatformTransactionManager.class),
          Runnable::run,
          mock(LearningSessionService.class));

  @BeforeEach
  void prepare() {
    appender.start();
    logger.addAppender(appender);
    when(repository.claim(eq(2L), anyString(), any(), any())).thenReturn(1);
    when(repository.findById(2L)).thenReturn(Optional.of(work));
    when(messages.lockForFeedbackResult(2L)).thenReturn(true);
    finishReturns(1);
    when(client.requestMessageFeedback(any()))
        .thenReturn(new AiMessageFeedbackResult(1L, 2L, ProcessingStatus.FAILED));
  }

  @AfterEach
  void detach() {
    logger.detachAppender(appender);
    appender.stop();
  }

  @Test
  void failedBodyIsObservedUntilFinalAttempt() {
    ReflectionTestUtils.setField(work, "attempts", 1);
    assertThat(service.generate(2L)).isEqualTo(ProcessingStatus.FAILED);
    assertThat(errors()).isEmpty();
    assertThat(appender.list)
        .anySatisfy(event -> assertThat(event.getFormattedMessage()).contains("outcome=retrying"));
    ReflectionTestUtils.setField(work, "attempts", 3);
    service.generate(2L);
    assertThat(errors())
        .singleElement()
        .satisfies(event -> assertThat(event.getFormattedMessage()).contains("attempts_exhausted"));
  }

  @Test
  void staleAttemptDoesNotReportAnotherFinalFailure() {
    ReflectionTestUtils.setField(work, "attempts", 3);
    finishReturns(0);
    service.generate(2L);
    assertThat(errors()).isEmpty();
  }

  @Test
  void codeAndConfigurationDefectsAreReportedBeforeRetryExhaustion() {
    ReflectionTestUtils.setField(work, "attempts", 1);
    when(client.requestMessageFeedback(any()))
        .thenThrow(
            ApiException.causedBy(
                ErrorCode.AI_GENERATION_FAILED, new IllegalStateException("private-config")));
    service.generate(2L);
    assertThat(errors())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.getFormattedMessage())
                  .contains("code_or_configuration_defect")
                  .doesNotContain("private-");
              assertThat(event.getThrowableProxy().getCause().getMessage())
                  .doesNotContain("private-");
            });
  }

  @Test
  void connectionFailureRemainsReportableWhenExhausted() {
    ReflectionTestUtils.setField(work, "attempts", 3);
    when(client.requestMessageFeedback(any()))
        .thenThrow(
            ApiException.causedBy(
                ErrorCode.AI_GENERATION_FAILED, new java.net.ConnectException("private-host")));
    service.generate(2L);
    assertThat(errors())
        .singleElement()
        .satisfies(
            event ->
                assertThat(event.getThrowableProxy().getCause().getMessage())
                    .contains("ConnectException"));
  }

  @Test
  void storageFailureIsReportedEvenWhenGenerationWillRetry() {
    when(repository.finish(
            anyLong(), anyString(), nullable(String.class), anyBoolean(), anyBoolean(), any()))
        .thenThrow(new IllegalStateException("private-sql"));
    assertThatThrownBy(() -> service.generate(2L)).isInstanceOf(IllegalStateException.class);
    assertThat(errors())
        .singleElement()
        .satisfies(
            event -> assertThat(event.getFormattedMessage()).contains("failure_stage=persistence"));
  }

  @Test
  void alreadyTerminalRecoverySnapshotDoesNotReportAgain() {
    ReflectionTestUtils.setField(work, "terminalFailed", true);
    when(repository.findExhausted(any(), any())).thenReturn(List.of(work));
    when(repository.findRecoverable(any(), any())).thenReturn(List.of());
    service.recover();
    assertThat(errors()).isEmpty();
  }

  @Test
  void recoveryReportsEachDatabaseItemWithoutLeakingThePreviousSession() {
    MessageFeedbackWork second = new MessageFeedbackWork(4L, 3L, "{}", LocalDateTime.now());
    when(repository.findById(4L)).thenReturn(Optional.of(second));
    when(messages.lockForFeedbackResult(4L)).thenReturn(true);
    when(repository.findExhausted(any(), any())).thenReturn(List.of(work, second));
    when(repository.findRecoverable(any(), any())).thenReturn(List.of());
    service.recover();
    assertThat(errors()).hasSize(2);
    assertThat(errors().get(0).getMDCPropertyMap())
        .containsEntry("learning_session_id", "1")
        .containsEntry("message_id", "2");
    assertThat(errors().get(1).getMDCPropertyMap())
        .containsEntry("learning_session_id", "3")
        .containsEntry("message_id", "4")
        .doesNotContainKeys("user_id", "free_talk_session_id");
    assertThat(org.slf4j.MDC.get("learning_session_id")).isNull();
    assertThat(org.slf4j.MDC.get("message_id")).isNull();
  }

  private void finishReturns(int count) {
    when(repository.finish(
            anyLong(),
            nullable(String.class),
            nullable(String.class),
            anyBoolean(),
            anyBoolean(),
            any()))
        .thenReturn(count);
  }

  private List<ILoggingEvent> errors() {
    return appender.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
  }
}
