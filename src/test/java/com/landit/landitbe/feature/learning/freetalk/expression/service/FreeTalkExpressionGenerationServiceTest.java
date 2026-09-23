// 표현 생성이 배운 표현 후보를 추천 요청에 싣고, 후보를 읽지 못해도 추천을 이어 가는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.expression.recommendation.dto.ExpressionRecommendationCandidate;
import com.landit.landitbe.feature.content.expression.recommendation.service.ExpressionRecommendationService;
import com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiConversationEmbeddingsResult;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiConversationExcerpt;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendation;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsResult;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkLearnedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkUsedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.expression.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuseSource;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.repository.FreeTalkExpressionReuseRepository;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.service.FreeTalkExpressionReuseAssemblyService;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.service.FreeTalkLearnedExpressionSelectionService;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.profile.learning.service.ProfileLearningService;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.Locale;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

/** 표현 생성이 배운 표현 후보를 추천 요청에 싣고, 후보를 읽지 못해도 추천을 이어 가는지 검증한다. */
class FreeTalkExpressionGenerationServiceTest {

  private static final long LEARNING_SESSION_ID = 300L;
  private static final long USER_PROFILE_ID = 41L;

  private final FreeTalkSessionRepository freeTalkSessionRepository =
      mock(FreeTalkSessionRepository.class);
  private final LearningSessionService learningSessionService = mock(LearningSessionService.class);
  private final SessionHistoryService sessionHistoryService = mock(SessionHistoryService.class);
  private final ConversationMessageService conversationMessageService =
      mock(ConversationMessageService.class);
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository =
      mock(FreeTalkSessionExpressionRepository.class);
  private final ExpressionRecommendationService expressionRecommendationService =
      mock(ExpressionRecommendationService.class);
  private final ExpressionCandidateSelectionService candidateSelectionService =
      mock(ExpressionCandidateSelectionService.class);
  private final FreeTalkLearnedExpressionSelectionService learnedExpressionSelectionService =
      mock(FreeTalkLearnedExpressionSelectionService.class);
  private final FreeTalkExpressionReuseAssemblyService reuseAssemblyService =
      mock(FreeTalkExpressionReuseAssemblyService.class);
  private final FreeTalkExpressionReuseRepository reuseRepository =
      mock(FreeTalkExpressionReuseRepository.class);
  private final ProfileLearningService profileLearningService = mock(ProfileLearningService.class);
  private final AiFreeTalkClient aiFreeTalkClient = mock(AiFreeTalkClient.class);
  private final FreeTalkSession freeTalkSession = mock(FreeTalkSession.class);

  private final FreeTalkExpressionGenerationService service =
      new FreeTalkExpressionGenerationService(
          freeTalkSessionRepository,
          learningSessionService,
          sessionHistoryService,
          conversationMessageService,
          sessionExpressionRepository,
          expressionRecommendationService,
          candidateSelectionService,
          learnedExpressionSelectionService,
          reuseAssemblyService,
          reuseRepository,
          profileLearningService,
          aiFreeTalkClient,
          mock(PlatformTransactionManager.class));

  @BeforeEach
  void stubPipeline() {
    when(freeTalkSession.getId()).thenReturn(301L);
    when(freeTalkSession.getConversationStatus()).thenReturn(FreeTalkConversationStatus.COMPLETED);
    when(freeTalkSession.getExpressionGenerationStatus())
        .thenReturn(ExpressionGenerationStatus.PREPARING);
    when(freeTalkSession.getExpressionGenerationAttempt()).thenReturn(1);
    when(freeTalkSessionRepository.findByLearningSessionIdForUpdate(LEARNING_SESSION_ID))
        .thenReturn(Optional.of(freeTalkSession));
    LearningSessionSnapshot learningSession = mock(LearningSessionSnapshot.class);
    when(learningSession.getStatus()).thenReturn(LearningSessionStatus.COMPLETED);
    when(learningSession.getUserProfileId()).thenReturn(USER_PROFILE_ID);
    when(learningSession.getTargetLocale()).thenReturn(Locale.EN);
    when(learningSession.getBaseLocale()).thenReturn(Locale.KR);
    when(learningSessionService.findSession(LEARNING_SESSION_ID))
        .thenReturn(Optional.of(learningSession));
    SessionHistorySnapshot history = mock(SessionHistorySnapshot.class);
    when(history.getId()).thenReturn(302L);
    when(sessionHistoryService.findByLearningSessionId(LEARNING_SESSION_ID))
        .thenReturn(Optional.of(history));
    List<SessionHistoryMessageSnapshot> messages =
        List.of(
            message(5503L, ConversationSpeaker.AI, "What did you do today?"),
            message(5504L, ConversationSpeaker.USER, "I grabbed a coffee with a friend."));
    when(conversationMessageService.findAll(302L)).thenReturn(messages);
    when(profileLearningService.findLearningLevel(USER_PROFILE_ID)).thenReturn(Optional.empty());
    when(aiFreeTalkClient.extractConversationEmbeddings(any()))
        .thenReturn(
            new AiConversationEmbeddingsResult(
                List.of(
                    new AiConversationExcerpt(
                        "I grabbed a coffee with a friend.",
                        Collections.nCopies(AiConversationExcerpt.EMBEDDING_DIMENSION, 0.0f)))));
    when(candidateSelectionService.selectCandidateIds(anyList(), anyLong(), any(), any(), anyInt()))
        .thenReturn(List.of(7L));
    when(expressionRecommendationService.getExpressionCandidatesByIds(
            List.of(7L), Locale.EN, Locale.KR))
        .thenReturn(
            List.of(new ExpressionRecommendationCandidate(7L, "catch up", "밀린 얘기를 하다", "근황")));
    when(aiFreeTalkClient.recommendExpressions(any()))
        .thenReturn(
            new AiFreeTalkExpressionRecommendationsResult(
                List.of(new AiFreeTalkExpressionRecommendation(1, 7L))));
  }

  @DisplayName("사용자 발화만으로 고른 배운 표현 후보를 ID·원문·뜻만 추려 추천 요청에 싣는다.")
  @Test
  void sendsSelectedLearnedExpressionsWithRecommendationRequest() {
    when(learnedExpressionSelectionService.select(
            USER_PROFILE_ID, Locale.EN, Locale.KR, List.of("I grabbed a coffee with a friend.")))
        .thenReturn(
            List.of(
                new FreeTalkLearnedExpression(
                    812L,
                    "grab a coffee",
                    "커피 한잔하다",
                    FreeTalkExpressionReuseSource.SCENARIO,
                    41L,
                    LocalDate.of(2026, 9, 10))));

    service.generate(LEARNING_SESSION_ID);

    assertThat(recommendationRequest().learnedExpressions())
        .containsExactly(new AiFreeTalkLearnedExpression(812L, "grab a coffee", "커피 한잔하다"));
    verify(freeTalkSession).completeExpressionGeneration();
  }

  @DisplayName("배운 표현 후보를 읽다 실패해도 후보 없이 추천을 끝까지 진행한다.")
  @Test
  void continuesRecommendationWhenLearnedExpressionSelectionFails() {
    when(learnedExpressionSelectionService.select(anyLong(), any(), any(), any()))
        .thenThrow(new IllegalStateException("learned expressions unavailable"));

    service.generate(LEARNING_SESSION_ID);

    assertThat(recommendationRequest().learnedExpressions()).isEmpty();
    verify(freeTalkSession).completeExpressionGeneration();
    verify(freeTalkSession, never()).failExpressionGeneration();
  }

  @DisplayName("고른 후보가 AI 서버 계약을 어기면(표현 ID 중복) 후보를 통째로 버리고 추천은 끝까지 진행한다.")
  @Test
  void dropsLearnedExpressionsThatViolateTheAiContract() {
    when(learnedExpressionSelectionService.select(anyLong(), any(), any(), any()))
        .thenReturn(List.of(learnedExpression(), learnedExpression()));

    service.generate(LEARNING_SESSION_ID);

    assertThat(recommendationRequest().learnedExpressions()).isEmpty();
    verify(freeTalkSession).completeExpressionGeneration();
    verify(freeTalkSession, never()).failExpressionGeneration();
  }

  @DisplayName("AI가 다시 썼다고 판정한 표현은 다시 확인한 기록으로 바꿔 추천과 함께 저장한다.")
  @Test
  void savesAssembledReusesWithRecommendations() {
    List<FreeTalkLearnedExpression> learned = List.of(learnedExpression());
    List<AiFreeTalkUsedExpression> used =
        List.of(new AiFreeTalkUsedExpression(812L, 5504L, "grabbed a coffee"));
    List<FreeTalkExpressionReuse> reuses = List.of(mock(FreeTalkExpressionReuse.class));
    when(learnedExpressionSelectionService.select(anyLong(), any(), any(), any()))
        .thenReturn(learned);
    when(aiFreeTalkClient.recommendExpressions(any()))
        .thenReturn(
            new AiFreeTalkExpressionRecommendationsResult(
                List.of(new AiFreeTalkExpressionRecommendation(1, 7L)), used));
    when(reuseAssemblyService.assemble(
            eq(USER_PROFILE_ID),
            eq(301L),
            eq(Locale.EN),
            eq(Locale.KR),
            any(),
            eq(learned),
            eq(used)))
        .thenReturn(reuses);

    service.generate(LEARNING_SESSION_ID);

    verify(reuseRepository).saveAll(reuses);
    verify(freeTalkSession).completeExpressionGeneration();
  }

  @DisplayName("세션에 재사용 기록이 이미 있으면 다시 넣지 않는다.")
  @Test
  void skipsReusesWhenSessionAlreadyHasThem() {
    stubUsedExpressionsAssembledInto(List.of(mock(FreeTalkExpressionReuse.class)));
    when(reuseRepository.existsByFreeTalkSessionId(301L)).thenReturn(true);

    service.generate(LEARNING_SESSION_ID);

    verify(reuseRepository, never()).saveAll(any());
    verify(freeTalkSession).completeExpressionGeneration();
  }

  @DisplayName("재사용 기록을 만들다 실패해도 추천은 저장하고 완료한다.")
  @Test
  void savesRecommendationsWhenReuseAssemblyFails() {
    stubUsedExpressionsAssembledInto(null);
    when(reuseAssemblyService.assemble(anyLong(), anyLong(), any(), any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("source titles unavailable"));

    service.generate(LEARNING_SESSION_ID);

    verify(reuseRepository, never()).saveAll(any());
    verify(sessionExpressionRepository).save(any());
    verify(freeTalkSession).completeExpressionGeneration();
    verify(freeTalkSession, never()).failExpressionGeneration();
  }

  @DisplayName("AI가 다시 쓴 표현이 없다고 하면 재사용 기록을 만들지도 넣지도 않는다.")
  @Test
  void skipsReuseAssemblyWhenNothingWasUsed() {
    service.generate(LEARNING_SESSION_ID);

    verifyNoInteractions(reuseAssemblyService, reuseRepository);
  }

  private void stubUsedExpressionsAssembledInto(List<FreeTalkExpressionReuse> reuses) {
    when(aiFreeTalkClient.recommendExpressions(any()))
        .thenReturn(
            new AiFreeTalkExpressionRecommendationsResult(
                List.of(new AiFreeTalkExpressionRecommendation(1, 7L)),
                List.of(new AiFreeTalkUsedExpression(812L, 5504L, "grabbed a coffee"))));
    when(reuseAssemblyService.assemble(anyLong(), anyLong(), any(), any(), any(), any(), any()))
        .thenReturn(reuses);
  }

  private static FreeTalkLearnedExpression learnedExpression() {
    return new FreeTalkLearnedExpression(
        812L,
        "grab a coffee",
        "커피 한잔하다",
        FreeTalkExpressionReuseSource.SCENARIO,
        41L,
        LocalDate.of(2026, 9, 10));
  }

  private AiFreeTalkExpressionRecommendationsRequest recommendationRequest() {
    ArgumentCaptor<AiFreeTalkExpressionRecommendationsRequest> request =
        ArgumentCaptor.forClass(AiFreeTalkExpressionRecommendationsRequest.class);
    verify(aiFreeTalkClient).recommendExpressions(request.capture());
    return request.getValue();
  }

  private static SessionHistoryMessageSnapshot message(
      long id, ConversationSpeaker role, String content) {
    SessionHistoryMessageSnapshot message = mock(SessionHistoryMessageSnapshot.class);
    when(message.getId()).thenReturn(id);
    when(message.getTurnNumber()).thenReturn(1);
    when(message.getRole()).thenReturn(role);
    when(message.getContent()).thenReturn(content);
    return message;
  }
}
