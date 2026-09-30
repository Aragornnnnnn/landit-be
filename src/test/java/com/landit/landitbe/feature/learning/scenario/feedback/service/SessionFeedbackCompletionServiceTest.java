// AI의 표현 재사용 주장이 원문과 학습 표현에 모두 근거할 때만 저장되는지 검증한다.

package com.landit.landitbe.feature.learning.scenario.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.expression.progress.domain.ExpressionLearningSource;
import com.landit.landitbe.feature.learning.scenario.feedback.client.ai.AiSessionFeedbackResult;
import com.landit.landitbe.feature.learning.scenario.feedback.client.ai.AiSessionMessageFeedbackResult;
import com.landit.landitbe.feature.learning.scenario.feedback.domain.FeedbackType;
import com.landit.landitbe.feature.learning.scenario.feedback.domain.SessionHistorySummaryFeedback;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.LoadedSessionFeedbackContext;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.ScenarioFeedbackEvidence;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.UserMessageContext;
import com.landit.landitbe.feature.learning.scenario.progress.service.ScenarioProgressService;
import com.landit.landitbe.feature.learning.scenario.session.client.ai.AiScenarioContext;
import com.landit.landitbe.shared.domain.Locale;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class SessionFeedbackCompletionServiceTest {

  @DisplayName("원문에 있어도 후보 표현과 무관한 인용은 재사용으로 저장하지 않는다.")
  @ParameterizedTest
  @ValueSource(strings = {"yesterday", "go hiking", "made up", ""})
  void dropsUnrelatedOrUnquotedExpression(String matchedText) {
    JsonNode payload = recordExpressionReuse(matchedText);
    assertThat(payload.get("pending").asBoolean()).isFalse();
    assertThat(payload.get("items").isEmpty()).isTrue();
  }

  @DisplayName("후보 표현과 이를 포함하는 긴 원문 인용을 재사용으로 저장한다.")
  @ParameterizedTest
  @ValueSource(strings = {"used to", "used to go", "I used to go hiking yesterday."})
  void savesExactExpressionWithinLongerQuote(String matchedText) {
    JsonNode payload = recordExpressionReuse(matchedText);
    assertThat(payload.get("items").size()).isEqualTo(1);
    JsonNode item = payload.get("items").get(0);
    assertThat(item.get("expressionId").asLong()).isEqualTo(812L);
    assertThat(item.get("matchedText").asText()).isEqualTo(matchedText);
  }

  /** 총평 확정 경로를 실행해 저장 서비스로 전달된 표현 재사용 JSON을 확인한다. */
  private JsonNode recordExpressionReuse(String matchedText) {
    LearningSessionService sessions = mock(LearningSessionService.class);
    SessionHistoryService histories = mock(SessionHistoryService.class);
    SessionFeedbackDataService data = mock(SessionFeedbackDataService.class);
    final SessionFeedbackCompletionService service =
        new SessionFeedbackCompletionService(
            sessions,
            histories,
            data,
            mock(ConversationMessageService.class),
            mock(ScenarioProgressService.class));
    when(sessions.findOwnedCompletedForUpdate(1L, 10L))
        .thenReturn(mock(LearningSessionSnapshot.class));
    when(histories.require(20L)).thenReturn(mock(SessionHistorySnapshot.class));
    when(data.saveSummary(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var message =
        new UserMessageContext(100L, 1, "I used to go hiking yesterday.", null, null, List.of());
    var evidence =
        new ScenarioFeedbackEvidence(
            null,
            List.of(),
            List.of(
                new ScenarioFeedbackEvidence.LearnedExpressionCandidate(
                    812L,
                    "used to",
                    "예전에 ~하곤 했다",
                    ExpressionLearningSource.SCENARIO,
                    LocalDate.of(2026, 9, 28))));
    var context =
        new LoadedSessionFeedbackContext(
            10L,
            20L,
            Locale.EN,
            Locale.KR,
            null,
            mock(AiScenarioContext.class),
            List.of(message),
            evidence,
            Optional.empty());
    var feedback =
        new AiSessionMessageFeedbackResult(
            100L, FeedbackType.GOOD, "비유", null, "설명", null, null, null);
    var result =
        new AiSessionFeedbackResult(
            10L,
            80,
            new BigDecimal("2.0"),
            "강조",
            "총평",
            List.of(feedback),
            null,
            List.of(new AiSessionFeedbackResult.UsedExpression(812L, 100L, matchedText)));

    service.record(1L, context, result);

    var saved = ArgumentCaptor.forClass(SessionHistorySummaryFeedback.class);
    verify(data).saveSummary(saved.capture());
    return saved.getValue().getExpressionReusePayload();
  }
}
