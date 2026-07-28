// 완료 프리톡에서 맞춤 표현을 생성하고 저장한다.

package com.landit.landitbe.feature.session.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExistingExpression;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionLearningContent;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionLearningContentRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionRecommendation;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkLearningExpression;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkExpressionSourceType;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 완료 프리톡에서 맞춤 표현을 생성하고 저장한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkExpressionGenerationService {

  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final LearningSessionRepository learningSessionRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository;
  private final WritingExpressionRepository writingExpressionRepository;
  private final AiFreeTalkClient aiFreeTalkClient;
  private final PlatformTransactionManager transactionManager;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 완료된 프리톡의 표현 생성 작업을 한 번 실행한다. */
  public void generate(long learningSessionId) {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    GenerationContext context = transactionTemplate.execute(status -> prepare(learningSessionId));
    if (context == null) {
      return;
    }
    try {
      List<AiFreeTalkExpressionRecommendation> recommendations =
          aiFreeTalkClient
              .recommendExpressions(
                  new AiFreeTalkExpressionRecommendationsRequest(
                      context.learningSessionId(),
                      context.targetLocale(),
                      context.baseLocale(),
                      context.history(),
                      context.existingExpressions()))
              .recommendations();
      List<AiFreeTalkLearningExpression> newExpressions =
          recommendations.stream()
              .filter(
                  recommendation -> recommendation.sourceType() == FreeTalkExpressionSourceType.NEW)
              .map(
                  recommendation ->
                      new AiFreeTalkLearningExpression(
                          recommendation.targetExpressionText(),
                          recommendation.baseExpressionMeaningText(),
                          recommendation.usageSummary()))
              .toList();
      List<AiFreeTalkExpressionLearningContent> learningContents =
          newExpressions.isEmpty()
              ? List.of()
              : aiFreeTalkClient
                  .generateExpressionLearningContent(
                      new AiFreeTalkExpressionLearningContentRequest(
                          context.learningSessionId(),
                          context.targetLocale(),
                          context.baseLocale(),
                          newExpressions))
                  .expressions();
      transactionTemplate.executeWithoutResult(
          status -> persistReady(context, recommendations, learningContents));
    } catch (RuntimeException exception) {
      transactionTemplate.executeWithoutResult(status -> fail(learningSessionId));
    }
  }

  private GenerationContext prepare(long learningSessionId) {
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    LearningSession learningSession =
        learningSessionRepository.findById(learningSessionId).orElseThrow();
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED
        || freeTalkSession.getConversationStatus() != FreeTalkConversationStatus.COMPLETED
        || freeTalkSession.getExpressionGenerationStatus() != ExpressionGenerationStatus.PREPARING
        || freeTalkSession.getExpressionGenerationStartedAt() != null) {
      return null;
    }
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    freeTalkSession.startExpressionGeneration();
    List<AiFreeTalkExistingExpression> existingExpressions =
        writingExpressionRepository
            .findByTargetLocaleAndBaseLocaleAndStatus(
                learningSession.getTargetLocale(),
                learningSession.getBaseLocale(),
                ActiveStatus.ACTIVE)
            .stream()
            .map(
                expression ->
                    new AiFreeTalkExistingExpression(
                        expression.getId(),
                        expression.getTargetExpressionText(),
                        expression.getBaseExpressionMeaningText(),
                        expression.getUsageSummary()))
            .toList();
    return new GenerationContext(
        learningSessionId,
        freeTalkSession.getId(),
        learningSession.getTargetLocale().name(),
        learningSession.getBaseLocale().name(),
        sessionHistoryMessageRepository
            .findBySessionHistoryIdOrderByMessageSequenceAsc(history.getId())
            .stream()
            .map(
                message ->
                    new AiConversationHistoryMessage(
                        message.getId(),
                        message.getTurnNumber(),
                        message.getRole().name(),
                        message.getContent(),
                        message.getTranslatedContent()))
            .toList(),
        existingExpressions);
  }

  private void persistReady(
      GenerationContext context,
      List<AiFreeTalkExpressionRecommendation> recommendations,
      List<AiFreeTalkExpressionLearningContent> learningContents) {
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(context.learningSessionId())
            .orElseThrow();
    if (freeTalkSession.getExpressionGenerationStatus() != ExpressionGenerationStatus.PREPARING) {
      return;
    }
    Map<String, AiFreeTalkExpressionLearningContent> contentsByText =
        learningContents.stream()
            .collect(
                Collectors.toMap(
                    AiFreeTalkExpressionLearningContent::targetExpressionText, content -> content));
    sessionExpressionRepository.deleteByFreeTalkSessionId(context.freeTalkSessionId());
    for (AiFreeTalkExpressionRecommendation recommendation : recommendations) {
      sessionExpressionRepository.save(
          recommendation.sourceType() == FreeTalkExpressionSourceType.EXISTING
              ? existingSessionExpression(context, recommendation)
              : generatedSessionExpression(context, recommendation, contentsByText));
    }
    freeTalkSession.completeExpressionGeneration();
  }

  private void fail(long learningSessionId) {
    freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(learningSessionId)
        .filter(
            session ->
                session.getExpressionGenerationStatus() == ExpressionGenerationStatus.PREPARING)
        .ifPresent(FreeTalkSession::failExpressionGeneration);
  }

  private FreeTalkSessionExpression existingSessionExpression(
      GenerationContext context, AiFreeTalkExpressionRecommendation recommendation) {
    WritingExpression writingExpression =
        writingExpressionRepository
            .findByIdAndStatus(recommendation.existingExpressionId(), ActiveStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.AI_RESPONSE_INVALID));
    return FreeTalkSessionExpression.existing(
        context.freeTalkSessionId(),
        writingExpression.getId(),
        recommendation.displayOrder(),
        recommendation.contextualExample().sentenceText(),
        recommendation.contextualExample().sentenceTranslation());
  }

  private FreeTalkSessionExpression generatedSessionExpression(
      GenerationContext context,
      AiFreeTalkExpressionRecommendation recommendation,
      Map<String, AiFreeTalkExpressionLearningContent> contentsByText) {
    AiFreeTalkExpressionLearningContent content =
        contentsByText.get(recommendation.targetExpressionText());
    if (content == null) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
    return FreeTalkSessionExpression.generated(
        context.freeTalkSessionId(),
        recommendation.displayOrder(),
        recommendation.contextualExample().sentenceText(),
        recommendation.contextualExample().sentenceTranslation(),
        objectMapper.valueToTree(content));
  }

  private record GenerationContext(
      long learningSessionId,
      long freeTalkSessionId,
      String targetLocale,
      String baseLocale,
      List<AiConversationHistoryMessage> history,
      List<AiFreeTalkExistingExpression> existingExpressions) {}
}
