// 완료 프리톡에 기존 공용 표현을 추천하고 저장한다.

package com.landit.landitbe.feature.learning.freetalk.expression.service;

import com.landit.landitbe.feature.content.expression.domain.ExpressionDifficultyPolicy;
import com.landit.landitbe.feature.content.expression.recommendation.service.ExpressionRecommendationService;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.exception.SessionErrorCode;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiConversationEmbeddingsRequest;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiConversationEmbeddingsResult;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExistingExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendation;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsResult;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkLearnedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkUsedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.expression.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto.FreeTalkLearnedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.repository.FreeTalkExpressionReuseRepository;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.service.FreeTalkExpressionReuseAssemblyService;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.service.FreeTalkLearnedExpressionSelectionService;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.profile.learning.service.ProfileLearningService;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.landit.landitbe.shared.observability.FailureObservation;
import com.landit.landitbe.shared.observability.ObservationContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 완료 프리톡에 기존 공용 표현을 추천하고 저장한다. */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkExpressionGenerationService {

  private static final String EMBEDDINGS_STAGE = "conversationEmbeddings";
  private static final String CANDIDATE_SEARCH_STAGE = "candidateSearch";
  private static final String CANDIDATE_LOAD_STAGE = "candidateLoad";
  private static final String LEARNED_EXPRESSION_STAGE = "learnedExpressionLoad";
  private static final String RECOMMENDATION_STAGE = "recommendation";
  private static final String REUSE_ASSEMBLY_STAGE = "reuseAssembly";
  private static final String PERSIST_STAGE = "persist";

  private static final String GENERATION_COMPLETED_LOG =
      "프리톡 표현 생성 완료. learningSessionId={}, historyMessageCount={}, excerptCount={},"
          + " candidateCount={}, recommendationCount={}, learnedExpressionCount={},"
          + " usedExpressionCount={}, reuseCount={}, totalMs={}, stages=[{}]";
  private static final String LEARNED_EXPRESSION_FAILED_LOG =
      "프리톡 배운 표현 후보 조회 실패. 표현 재사용 판정 없이 추천만 진행한다. learningSessionId={}";
  private static final String REUSE_ASSEMBLY_FAILED_LOG =
      "프리톡 표현 재사용 기록 조립 실패. 재사용 기록 없이 추천만 저장한다. learningSessionId={}";
  private static final String REUSE_ALREADY_RECORDED_LOG =
      "프리톡 표현 재사용 기록이 이미 있어 새 기록을 버린다. learningSessionId={}, droppedCount={}";

  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final LearningSessionService learningSessionService;
  private final SessionHistoryService sessionHistoryService;
  private final ConversationMessageService conversationMessageService;
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository;
  private final ExpressionRecommendationService expressionRecommendationService;
  private final ExpressionCandidateSelectionService candidateSelectionService;
  private final FreeTalkLearnedExpressionSelectionService learnedExpressionSelectionService;
  private final FreeTalkExpressionReuseAssemblyService reuseAssemblyService;
  private final FreeTalkExpressionReuseRepository reuseRepository;
  private final ProfileLearningService profileLearningService;
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
    GenerationContext context;
    try {
      context = transactionTemplate.execute(status -> prepare(learningSessionId));
    } catch (RuntimeException exception) {
      FailureObservation.failed("expression", "claim", "storage_failed", exception);
      throw exception;
    }
    if (context == null) {
      return;
    }

    ObservationContext.run(
        context.learningSessionId(),
        context.freeTalkSessionId(),
        null,
        () -> generateInContext(context, transactionTemplate));
  }

  private void generateInContext(
      GenerationContext context, TransactionTemplate transactionTemplate) {
    long learningSessionId = context.learningSessionId();
    StageTimings timings = new StageTimings();
    long startNanos = System.nanoTime();
    try {
      GenerationOutcome outcome = runPipeline(context, transactionTemplate, timings);
      log.info(
          GENERATION_COMPLETED_LOG,
          learningSessionId,
          outcome.historyMessageCount(),
          outcome.excerptCount(),
          outcome.candidateCount(),
          outcome.recommendationCount(),
          outcome.learnedExpressionCount(),
          outcome.usedExpressionCount(),
          outcome.reuseCount(),
          elapsedMillis(startNanos),
          timings);
    } catch (RuntimeException exception) {
      // 부분 결과를 남기지 않고 재시도할 수 있도록 실패 상태만 기록한다.
      FailureObservation.failed("expression", timings.activeStage, "operation_failed", exception);
      try {
        transactionTemplate.executeWithoutResult(
            status -> fail(learningSessionId, context.attempt()));
      } catch (RuntimeException persistenceException) {
        FailureObservation.failed(
            "expression", "failure_state_persistence", "storage_failed", persistenceException);
        throw persistenceException;
      }
    }
  }

  /**
   * 제출 실패 등으로 실행하지 못한 표현 생성 작업을 실패 상태로 전환한다.
   *
   * @param learningSessionId 실패로 전환할 학습 세션 ID
   */
  public void markFailed(long learningSessionId) {
    ObservationContext.run(
        learningSessionId, null, null, () -> markFailedInContext(learningSessionId));
  }

  private void markFailedInContext(long learningSessionId) {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    FailureObservation.failed("expression", "dispatch", "executor_unavailable", null);
    try {
      transactionTemplate.executeWithoutResult(status -> fail(learningSessionId, null));
    } catch (RuntimeException exception) {
      FailureObservation.failed(
          "expression", "failure_state_persistence", "storage_failed", exception);
      throw exception;
    }
  }

  // 대화 임베딩부터 추천 저장까지를 단계별 소요 시간과 함께 실행한다.
  private GenerationOutcome runPipeline(
      GenerationContext context, TransactionTemplate transactionTemplate, StageTimings timings) {
    // 대화에서 학습 가치가 있는 사용자 발화를 추출하고 임베딩한다.
    AiConversationEmbeddingsResult conversationEmbeddings =
        timings.measure(
            EMBEDDINGS_STAGE,
            () ->
                aiFreeTalkClient.extractConversationEmbeddings(
                    new AiConversationEmbeddingsRequest(
                        context.learningSessionId(),
                        context.targetLocale().name(),
                        context.baseLocale().name(),
                        context.history())));
    // 임베딩 유사도 검색으로 전체 풀 대신 소수 후보만 추린다.
    List<Long> candidateIds =
        timings.measure(
            CANDIDATE_SEARCH_STAGE,
            () ->
                candidateSelectionService.selectCandidateIds(
                    conversationEmbeddings.excerpts(),
                    context.userProfileId(),
                    context.targetLocale(),
                    context.baseLocale(),
                    context.maxDifficultyLevel()));
    List<AiFreeTalkExistingExpression> existingExpressions =
        timings.measure(CANDIDATE_LOAD_STAGE, () -> candidateExpressions(context, candidateIds));
    if (existingExpressions.isEmpty()) {
      // 후보 선정과 재검증 사이에 후보가 전부 비활성화되면 재시도할 수 있게 실패로 전환한다.
      throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
    }
    List<FreeTalkLearnedExpression> learnedExpressions =
        timings.measure(LEARNED_EXPRESSION_STAGE, () -> learnedExpressions(context));
    // 전체 대화와 유사도 순 후보를 바탕으로 이번 프리톡에 적합한 표현을 추천한다. 같은 호출이 배운 표현을 다시 썼는지도 판정한다.
    AiFreeTalkExpressionRecommendationsResult result =
        timings.measure(
            RECOMMENDATION_STAGE,
            () ->
                aiFreeTalkClient.recommendExpressions(
                    new AiFreeTalkExpressionRecommendationsRequest(
                        context.learningSessionId(),
                        context.targetLocale().name(),
                        context.baseLocale().name(),
                        context.history(),
                        existingExpressions,
                        toAiLearnedExpressions(learnedExpressions))));
    List<AiFreeTalkExpressionRecommendation> recommendations = result.recommendations();
    // 출처 제목 같은 읽기는 저장 트랜잭션 밖에서 끝낸다. 저장 트랜잭션은 추천과 재사용 기록을 넣기만 한다.
    List<FreeTalkExpressionReuse> reuses =
        timings.measure(
            REUSE_ASSEMBLY_STAGE,
            () -> reuses(context, learnedExpressions, result.usedExpressions()));

    // 모든 외부 호출이 끝난 뒤 추천 결과를 한 트랜잭션으로 저장한다.
    timings.measureVoid(
        PERSIST_STAGE,
        () ->
            transactionTemplate.executeWithoutResult(
                status -> persistReady(context, recommendations, reuses)));
    return new GenerationOutcome(
        context.history().size(),
        conversationEmbeddings.excerpts().size(),
        existingExpressions.size(),
        recommendations.size(),
        learnedExpressions.size(),
        result.usedExpressions().size(),
        reuses.size());
  }

  // 배운 표현 후보와 같은 이유로, 재사용 기록을 만들지 못해도 추천은 저장한다.
  private List<FreeTalkExpressionReuse> reuses(
      GenerationContext context,
      List<FreeTalkLearnedExpression> learnedExpressions,
      List<AiFreeTalkUsedExpression> usedExpressions) {
    if (usedExpressions.isEmpty()) {
      return List.of();
    }
    try {
      return reuseAssemblyService.assemble(
          context.userProfileId(),
          context.freeTalkSessionId(),
          context.targetLocale(),
          context.baseLocale(),
          context.history(),
          learnedExpressions,
          usedExpressions);
    } catch (RuntimeException exception) {
      log.warn(REUSE_ASSEMBLY_FAILED_LOG, context.learningSessionId(), exception);
      return List.of();
    }
  }

  // 표현 재사용은 추천에 얹는 부가 기능이다. 배운 표현을 읽지 못해도 추천은 계속하고, 그 세션은 다시 쓴 표현이 없는 것으로 남는다.
  private List<FreeTalkLearnedExpression> learnedExpressions(GenerationContext context) {
    try {
      List<FreeTalkLearnedExpression> learnedExpressions =
          learnedExpressionSelectionService.select(
              context.userProfileId(),
              context.targetLocale(),
              context.baseLocale(),
              context.history().stream()
                  .filter(message -> ConversationSpeaker.USER.name().equals(message.role()))
                  .map(AiConversationHistoryMessage::content)
                  .toList());
      // 후보가 AI 서버 계약(상한·ID 중복)을 어기면 요청을 만들 때 예외가 나 추천까지 실패하고, 다시 시도해도 같은 후보라 계속 실패한다.
      // 그래서 여기서 먼저 확인해 어긴 후보는 통째로 버린다.
      AiFreeTalkExpressionRecommendationsRequest.requireValidLearnedExpressions(
          toAiLearnedExpressions(learnedExpressions));
      return learnedExpressions;
    } catch (RuntimeException exception) {
      log.warn(LEARNED_EXPRESSION_FAILED_LOG, context.learningSessionId(), exception);
      return List.of();
    }
  }

  // AI에는 표현 ID·원문·뜻만 보낸다. 출처와 배운 날은 재사용 기록을 만들 때만 쓴다.
  private static List<AiFreeTalkLearnedExpression> toAiLearnedExpressions(
      List<FreeTalkLearnedExpression> learnedExpressions) {
    return learnedExpressions.stream()
        .map(
            learned ->
                new AiFreeTalkLearnedExpression(
                    learned.expressionId(), learned.text(), learned.meaning()))
        .toList();
  }

  // 후보 ID로 공용 활성 표현을 다시 읽어 추천 요청 형식으로 변환한다.
  private List<AiFreeTalkExistingExpression> candidateExpressions(
      GenerationContext context, List<Long> candidateIds) {
    return expressionRecommendationService
        .getExpressionCandidatesByIds(candidateIds, context.targetLocale(), context.baseLocale())
        .stream()
        .map(
            candidate ->
                new AiFreeTalkExistingExpression(
                    candidate.expressionId(),
                    candidate.targetExpressionText(),
                    candidate.baseExpressionMeaningText(),
                    candidate.usageSummary()))
        .toList();
  }

  // 완료된 프리톡의 표현 생성 작업을 선점하고 AI 요청 문맥을 준비한다.
  private GenerationContext prepare(long learningSessionId) {
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    LearningSessionSnapshot learningSession =
        learningSessionService.findSession(learningSessionId).orElseThrow();

    // 완료된 프리톡에서 아직 실행되지 않은 PREPARING 작업만 선점한다.
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED
        || freeTalkSession.getConversationStatus() != FreeTalkConversationStatus.COMPLETED
        || freeTalkSession.getExpressionGenerationStatus() != ExpressionGenerationStatus.PREPARING
        || freeTalkSession.getExpressionGenerationStartedAt() != null) {
      return null;
    }

    SessionHistorySnapshot history =
        sessionHistoryService
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    freeTalkSession.startExpressionGeneration();

    // 트랜잭션 밖 AI 호출에 필요한 값만 불변 컨텍스트로 반환한다.
    return new GenerationContext(
        learningSessionId,
        freeTalkSession.getExpressionGenerationAttempt(),
        freeTalkSession.getId(),
        learningSession.getUserProfileId(),
        learningSession.getTargetLocale(),
        learningSession.getBaseLocale(),
        ExpressionDifficultyPolicy.maxDifficultyFor(
            profileLearningService
                .findLearningLevel(learningSession.getUserProfileId())
                .orElse(null)),
        conversationMessageService.findAll(history.getId()).stream()
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

  // AI 추천 결과를 세션 표현으로, 다시 쓴 표현을 재사용 기록으로 저장하고 생성 상태를 완료한다.
  private void persistReady(
      GenerationContext context,
      List<AiFreeTalkExpressionRecommendation> recommendations,
      List<FreeTalkExpressionReuse> reuses) {
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(context.learningSessionId())
            .orElseThrow();
    if (freeTalkSession.getExpressionGenerationStatus() != ExpressionGenerationStatus.PREPARING
        || freeTalkSession.getExpressionGenerationAttempt() != context.attempt()) {
      return;
    }
    sessionExpressionRepository.deleteByFreeTalkSessionId(context.freeTalkSessionId());
    for (AiFreeTalkExpressionRecommendation recommendation : recommendations) {
      sessionExpressionRepository.save(existingSessionExpression(context, recommendation));
    }
    // 재사용 기록은 고치지 않는 기록이다. 이 세션에 이미 있으면 다시 넣지 않는다.
    // 기록은 READY와 같은 트랜잭션에서만 저장되고 재시도는 FAILED에서만 되므로 정상 흐름에서는 일어나지 않는다. 일어나면 조용히 버리지 않고 알린다.
    if (!reuses.isEmpty()) {
      if (reuseRepository.existsByFreeTalkSessionId(context.freeTalkSessionId())) {
        log.warn(REUSE_ALREADY_RECORDED_LOG, context.learningSessionId(), reuses.size());
      } else {
        reuseRepository.saveAll(reuses);
      }
    }
    freeTalkSession.completeExpressionGeneration();
  }

  // 진행 중인 표현 생성 작업을 실패 상태로 전환한다.
  private void fail(long learningSessionId, Integer attempt) {
    freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(learningSessionId)
        .filter(
            session ->
                session.getExpressionGenerationStatus() == ExpressionGenerationStatus.PREPARING)
        .filter(
            session ->
                attempt == null
                    ? session.getExpressionGenerationStartedAt() == null
                    : session.getExpressionGenerationAttempt() == attempt)
        .ifPresent(FreeTalkSession::failExpressionGeneration);
  }

  // 기존 표현 추천을 검증하고 세션 연결 엔티티로 변환한다.
  private FreeTalkSessionExpression existingSessionExpression(
      GenerationContext context, AiFreeTalkExpressionRecommendation recommendation) {
    expressionRecommendationService.validatePublicFreeTalkExpression(
        recommendation.existingExpressionId(), context.targetLocale(), context.baseLocale());
    return FreeTalkSessionExpression.link(
        context.freeTalkSessionId(),
        recommendation.existingExpressionId(),
        recommendation.displayOrder());
  }

  // 시작 시각 이후 경과 시간을 밀리초로 환산한다.
  private static long elapsedMillis(long startNanos) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
  }

  private record GenerationContext(
      long learningSessionId,
      int attempt,
      long freeTalkSessionId,
      long userProfileId,
      Locale targetLocale,
      Locale baseLocale,
      int maxDifficultyLevel,
      List<AiConversationHistoryMessage> history) {}

  private record GenerationOutcome(
      int historyMessageCount,
      int excerptCount,
      int candidateCount,
      int recommendationCount,
      int learnedExpressionCount,
      int usedExpressionCount,
      int reuseCount) {}

  /** 표현 생성 단계별 소요 시간을 실행 순서대로 모아 로그 한 줄로 표현한다. */
  private static final class StageTimings {

    private String activeStage = "generation";
    private final Map<String, Long> elapsedMillisByStage = new LinkedHashMap<>();

    // 값을 반환하는 단계를 실행하고 예외 발생 여부와 무관하게 소요 시간을 남긴다.
    private <T> T measure(String stageName, Supplier<T> stage) {
      activeStage = stageName;
      long startNanos = System.nanoTime();
      try {
        return stage.get();
      } finally {
        elapsedMillisByStage.put(stageName, elapsedMillis(startNanos));
      }
    }

    // 반환값이 없는 단계를 실행하고 예외 발생 여부와 무관하게 소요 시간을 남긴다.
    private void measureVoid(String stageName, Runnable stage) {
      activeStage = stageName;
      long startNanos = System.nanoTime();
      try {
        stage.run();
      } finally {
        elapsedMillisByStage.put(stageName, elapsedMillis(startNanos));
      }
    }

    @Override
    public String toString() {
      return elapsedMillisByStage.entrySet().stream()
          .map(stage -> stage.getKey() + "=" + stage.getValue() + "ms")
          .collect(Collectors.joining(", "));
    }
  }
}
