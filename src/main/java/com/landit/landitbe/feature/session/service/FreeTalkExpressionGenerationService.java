// 완료 프리톡에 기존 공용 표현을 추천하고 저장한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.service.ExpressionQueryService;
import com.landit.landitbe.feature.session.client.ai.AiConversationEmbeddingsRequest;
import com.landit.landitbe.feature.session.client.ai.AiConversationEmbeddingsResult;
import com.landit.landitbe.feature.session.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExistingExpression;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionRecommendation;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
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
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 완료 프리톡에 기존 공용 표현을 추천하고 저장한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkExpressionGenerationService {

  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final LearningSessionRepository learningSessionRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository;
  private final ExpressionQueryService expressionQueryService;
  private final ExpressionCandidateSelectionService candidateSelectionService;
  private final AiFreeTalkClient aiFreeTalkClient;
  private final PlatformTransactionManager transactionManager;

  /**
   * 완료된 프리톡의 표현 생성 작업을 한 번 실행한다.
   *
   * @param learningSessionId 표현을 생성할 완료된 학습 세션 ID
   */
  public void generate(long learningSessionId) {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    // 중복 실행을 막는 상태 전이는 짧은 트랜잭션에서 먼저 확정한다.
    GenerationContext context = transactionTemplate.execute(status -> prepare(learningSessionId));
    if (context == null) {
      return;
    }

    try {
      // 대화에서 학습 가치가 있는 사용자 발화를 추출하고 임베딩한다.
      AiConversationEmbeddingsResult conversationEmbeddings =
          aiFreeTalkClient.extractConversationEmbeddings(
              new AiConversationEmbeddingsRequest(
                  context.learningSessionId(),
                  context.targetLocale().name(),
                  context.baseLocale().name(),
                  context.history()));
      // 임베딩 유사도 검색으로 전체 풀 대신 소수 후보만 추린다.
      List<Long> candidateIds =
          candidateSelectionService.selectCandidateIds(
              conversationEmbeddings.excerpts(),
              context.userProfileId(),
              context.targetLocale(),
              context.baseLocale());
      List<AiFreeTalkExistingExpression> existingExpressions =
          expressionQueryService
              .getExpressionCandidatesByIds(
                  candidateIds, context.targetLocale(), context.baseLocale())
              .stream()
              .map(
                  candidate ->
                      new AiFreeTalkExistingExpression(
                          candidate.expressionId(),
                          candidate.targetExpressionText(),
                          candidate.baseExpressionMeaningText(),
                          candidate.usageSummary()))
              .toList();
      if (existingExpressions.isEmpty()) {
        // 후보 선정과 재검증 사이에 후보가 전부 비활성화되면 재시도할 수 있게 실패로 전환한다.
        throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
      }
      // 전체 대화와 유사도 순 후보를 바탕으로 이번 프리톡에 적합한 표현을 추천한다.
      List<AiFreeTalkExpressionRecommendation> recommendations =
          aiFreeTalkClient
              .recommendExpressions(
                  new AiFreeTalkExpressionRecommendationsRequest(
                      context.learningSessionId(),
                      context.targetLocale().name(),
                      context.baseLocale().name(),
                      context.history(),
                      existingExpressions))
              .recommendations();

      // 모든 외부 호출이 끝난 뒤 추천 결과를 한 트랜잭션으로 저장한다.
      transactionTemplate.executeWithoutResult(status -> persistReady(context, recommendations));
    } catch (RuntimeException exception) {
      // 부분 결과를 남기지 않고 재시도할 수 있도록 실패 상태만 기록한다.
      transactionTemplate.executeWithoutResult(status -> fail(learningSessionId));
    }
  }

  /**
   * 제출 실패 등으로 실행하지 못한 표현 생성 작업을 실패 상태로 전환한다.
   *
   * @param learningSessionId 실패로 전환할 학습 세션 ID
   */
  public void markFailed(long learningSessionId) {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.executeWithoutResult(status -> fail(learningSessionId));
  }

  // 완료된 프리톡의 표현 생성 작업을 선점하고 AI 요청 문맥을 준비한다.
  private GenerationContext prepare(long learningSessionId) {
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    LearningSession learningSession =
        learningSessionRepository.findById(learningSessionId).orElseThrow();

    // 완료된 프리톡에서 아직 실행되지 않은 PREPARING 작업만 선점한다.
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

    // 트랜잭션 밖 AI 호출에 필요한 값만 불변 컨텍스트로 반환한다.
    return new GenerationContext(
        learningSessionId,
        freeTalkSession.getId(),
        learningSession.getUserProfileId(),
        learningSession.getTargetLocale(),
        learningSession.getBaseLocale(),
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
            .toList());
  }

  // AI 추천 결과를 세션 표현으로 저장하고 생성 상태를 완료한다.
  private void persistReady(
      GenerationContext context, List<AiFreeTalkExpressionRecommendation> recommendations) {
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(context.learningSessionId())
            .orElseThrow();
    if (freeTalkSession.getExpressionGenerationStatus() != ExpressionGenerationStatus.PREPARING) {
      return;
    }
    sessionExpressionRepository.deleteByFreeTalkSessionId(context.freeTalkSessionId());
    for (AiFreeTalkExpressionRecommendation recommendation : recommendations) {
      sessionExpressionRepository.save(existingSessionExpression(context, recommendation));
    }
    freeTalkSession.completeExpressionGeneration();
  }

  // 진행 중인 표현 생성 작업을 실패 상태로 전환한다.
  private void fail(long learningSessionId) {
    freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(learningSessionId)
        .filter(
            session ->
                session.getExpressionGenerationStatus() == ExpressionGenerationStatus.PREPARING)
        .ifPresent(FreeTalkSession::failExpressionGeneration);
  }

  // 기존 표현 추천을 검증하고 세션 연결 엔티티로 변환한다.
  private FreeTalkSessionExpression existingSessionExpression(
      GenerationContext context, AiFreeTalkExpressionRecommendation recommendation) {
    expressionQueryService.validatePublicFreeTalkExpression(
        recommendation.existingExpressionId(), context.targetLocale(), context.baseLocale());
    return FreeTalkSessionExpression.link(
        context.freeTalkSessionId(),
        recommendation.existingExpressionId(),
        recommendation.displayOrder());
  }

  private record GenerationContext(
      long learningSessionId,
      long freeTalkSessionId,
      long userProfileId,
      Locale targetLocale,
      Locale baseLocale,
      List<AiConversationHistoryMessage> history) {}
}
