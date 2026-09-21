// AI의 표현 재사용 판정을 다시 확인해 맞는 것만 기록으로 만드는지, 조각이 든 문장을 바르게 잘라 내는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.scenario.schedule.dto.ScenarioTitle;
import com.landit.landitbe.feature.content.scenario.service.ScenarioCatalogService;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkUsedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** AI의 표현 재사용 판정을 다시 확인해 맞는 것만 기록으로 만드는지, 조각이 든 문장을 바르게 잘라 내는지 검증한다. */
class FreeTalkExpressionReuseAssemblyServiceTest {

  private static final long USER_PROFILE_ID = 1207L;
  private static final long FREE_TALK_SESSION_ID = 30L;
  private static final long USER_MESSAGE_ID = 5504L;
  private static final long AI_MESSAGE_ID = 5503L;
  private static final long EXPRESSION_ID = 812L;
  private static final long SCENARIO_ID = 41L;
  private static final String USER_CONTENT =
      "I was tired. Let's grab a coffee after work! See you.";

  private final ScenarioCatalogService scenarioCatalogService = mock(ScenarioCatalogService.class);
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository =
      mock(FreeTalkSessionExpressionRepository.class);
  private final FreeTalkSessionRepository freeTalkSessionRepository =
      mock(FreeTalkSessionRepository.class);

  private final FreeTalkExpressionReuseAssemblyService service =
      new FreeTalkExpressionReuseAssemblyService(
          scenarioCatalogService, sessionExpressionRepository, freeTalkSessionRepository);

  @DisplayName("확인을 통과한 판정은 표현·출처의 지금 값과 조각이 든 문장을 담은 기록이 된다.")
  @Test
  void assemblesReuseWithSnapshotValues() {
    when(scenarioCatalogService.findTitles(Set.of(SCENARIO_ID), Locale.EN, Locale.KR))
        .thenReturn(List.of(new ScenarioTitle(SCENARIO_ID, "주말 계획")));

    List<FreeTalkExpressionReuse> reuses = assemble(used(EXPRESSION_ID, USER_MESSAGE_ID, "grab"));

    assertThat(reuses).hasSize(1);
    FreeTalkExpressionReuse reuse = reuses.getFirst();
    assertThat(reuse.getUserProfileId()).isEqualTo(USER_PROFILE_ID);
    assertThat(reuse.getFreeTalkSessionId()).isEqualTo(FREE_TALK_SESSION_ID);
    assertThat(reuse.getSessionHistoryMessageId()).isEqualTo(USER_MESSAGE_ID);
    assertThat(reuse.getWritingExpressionId()).isEqualTo(EXPRESSION_ID);
    assertThat(reuse.getExpressionText()).isEqualTo("grab a coffee");
    assertThat(reuse.getExpressionMeaning()).isEqualTo("커피 한잔하다");
    assertThat(reuse.getSourceType()).isEqualTo(FreeTalkExpressionReuseSource.SCENARIO);
    assertThat(reuse.getSourceTitle()).isEqualTo("주말 계획");
    assertThat(reuse.getSourceLearnedOn()).isEqualTo(LocalDate.of(2026, 9, 10));
    assertThat(reuse.getMatchedText()).isEqualTo("grab");
    assertThat(reuse.getQuotedSentence()).isEqualTo("Let's grab a coffee after work!");
  }

  @DisplayName("출처 시나리오의 제목을 찾지 못하면 제목 없이 기록한다.")
  @Test
  void keepsReuseWithoutTitleWhenScenarioTitleIsMissing() {
    when(scenarioCatalogService.findTitles(any(), any(), any())).thenReturn(List.of());

    List<FreeTalkExpressionReuse> reuses = assemble(used(EXPRESSION_ID, USER_MESSAGE_ID, "grab"));

    assertThat(reuses).hasSize(1);
    assertThat(reuses.getFirst().getSourceTitle()).isNull();
  }

  @DisplayName("맞지 않는 판정은 버리고 나머지 판정은 AI가 돌려준 순서대로 살린다.")
  @Test
  void dropsInvalidJudgmentsAndKeepsTheRest() {
    when(scenarioCatalogService.findTitles(any(), any(), any())).thenReturn(List.of());

    List<FreeTalkExpressionReuse> reuses =
        assemble(
            used(EXPRESSION_ID, USER_MESSAGE_ID, " "),
            used(EXPRESSION_ID, USER_MESSAGE_ID, null),
            used(999L, USER_MESSAGE_ID, "grab"),
            used(null, USER_MESSAGE_ID, "grab"),
            used(EXPRESSION_ID, 9999L, "grab"),
            used(EXPRESSION_ID, null, "grab"),
            // AI 발화에는 있는 말이어도 사용자가 쓴 것이 아니다.
            used(EXPRESSION_ID, AI_MESSAGE_ID, "coffee"),
            used(EXPRESSION_ID, USER_MESSAGE_ID, "grabbed a coffee"),
            // 화면이 대소문자까지 그대로 찾으므로 대소문자가 다르면 원문에 없는 것이다.
            used(EXPRESSION_ID, USER_MESSAGE_ID, "Grab a coffee"),
            used(EXPRESSION_ID, USER_MESSAGE_ID, "grab a coffee"),
            used(EXPRESSION_ID, USER_MESSAGE_ID, "coffee"));

    assertThat(reuses)
        .extracting(FreeTalkExpressionReuse::getMatchedText)
        .containsExactly("grab a coffee");
  }

  @DisplayName("받아들일 판정이 없으면 출처를 읽지 않는다.")
  @Test
  void skipsSourceLookupWhenNothingIsAccepted() {
    assertThat(assemble(used(999L, USER_MESSAGE_ID, "grab"))).isEmpty();

    verifyNoInteractions(scenarioCatalogService, sessionExpressionRepository);
  }

  @DisplayName("같은 표현을 다른 발화에서 또 썼으면 발화마다 기록한다.")
  @Test
  void keepsSameExpressionUsedInDifferentMessages() {
    when(scenarioCatalogService.findTitles(any(), any(), any())).thenReturn(List.of());
    List<AiConversationHistoryMessage> history =
        List.of(
            new AiConversationHistoryMessage(5504L, 1, "USER", "Let's grab a coffee.", null),
            new AiConversationHistoryMessage(
                5506L, 2, "USER", "We could grab a coffee again.", null));

    List<FreeTalkExpressionReuse> reuses =
        service.assemble(
            USER_PROFILE_ID,
            FREE_TALK_SESSION_ID,
            Locale.EN,
            Locale.KR,
            history,
            List.of(scenarioLearned()),
            List.of(
                used(EXPRESSION_ID, 5504L, "grab a coffee"),
                used(EXPRESSION_ID, 5506L, "grab a coffee")));

    assertThat(reuses)
        .extracting(FreeTalkExpressionReuse::getSessionHistoryMessageId)
        .containsExactly(5504L, 5506L);
  }

  @DisplayName("조각이 든 문장만 잘라 낸다. 조각이 두 문장에 걸치면 둘 다 담고, 문장 부호가 없으면 발화 전체를 담는다.")
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "I was tired. Let's grab a coffee after work! See you.|grab a coffee|Let's grab a coffee"
            + " after work!",
        "Let's grab a coffee|grab a coffee|Let's grab a coffee",
        "Really?! I did not know.|Really|Really?!",
        "I was tired. So I left. Bye.|tired. So|I was tired. So I left.",
        "I left early. Bye.|I left early.|I left early.",
        "It was great... I loved it.|loved|I loved it.",
      })
  void cutsTheSentenceContainingTheMatch(String content, String matchedText, String expected) {
    assertThat(FreeTalkExpressionReuseAssemblyService.sentenceContaining(content, matchedText))
        .isEqualTo(expected);
  }

  @DisplayName("줄바꿈도 문장의 경계로 본다.")
  @Test
  void treatsLineBreakAsSentenceBoundary() {
    assertThat(
            FreeTalkExpressionReuseAssemblyService.sentenceContaining(
                "first line\nlet's grab a coffee\nlast line", "grab"))
        .isEqualTo("let's grab a coffee");
  }

  private List<FreeTalkExpressionReuse> assemble(AiFreeTalkUsedExpression... usedExpressions) {
    return service.assemble(
        USER_PROFILE_ID,
        FREE_TALK_SESSION_ID,
        Locale.EN,
        Locale.KR,
        List.of(
            new AiConversationHistoryMessage(
                AI_MESSAGE_ID, 1, "AI", "Do you want some coffee?", null),
            new AiConversationHistoryMessage(USER_MESSAGE_ID, 1, "USER", USER_CONTENT, null)),
        List.of(scenarioLearned()),
        Arrays.asList(usedExpressions));
  }

  private static FreeTalkLearnedExpression scenarioLearned() {
    return new FreeTalkLearnedExpression(
        EXPRESSION_ID,
        "grab a coffee",
        "커피 한잔하다",
        FreeTalkExpressionReuseSource.SCENARIO,
        SCENARIO_ID,
        LocalDate.of(2026, 9, 10));
  }

  private static AiFreeTalkUsedExpression used(Long expressionId, Long messageId, String text) {
    return new AiFreeTalkUsedExpression(expressionId, messageId, text);
  }
}
