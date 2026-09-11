// 메시지 평가를 기존 백그라운드에서 실행하고 재시작 이후에도 복구한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import com.landit.landitbe.feature.session.client.ai.AiMessageFeedbackRequest;
import com.landit.landitbe.feature.session.client.ai.AiMessageFeedbackResult;
import com.landit.landitbe.feature.session.domain.MessageFeedbackWork;
import com.landit.landitbe.feature.session.domain.ProcessingStatus;
import com.landit.landitbe.feature.session.repository.MessageFeedbackWorkRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** DB에 보관한 평가 입력을 재실행하고 현재 시도의 결과만 확정한다. */
@Slf4j
@Service
public class MessageFeedbackWorkService {
  private final MessageFeedbackWorkRepository repository;
  private final AiConversationClient client;
  private final SessionMessageService messages;
  private final JsonMapper mapper;
  private final Clock clock;
  private final Duration leaseDuration;
  private final TransactionTemplate transaction;
  private final TaskExecutor executor;
  private final LearningSessionService learningSessions;

  /** 짧은 DB 트랜잭션과 기존 AI 실행기를 연결한다. */
  public MessageFeedbackWorkService(
      MessageFeedbackWorkRepository repository,
      AiConversationClient client,
      SessionMessageService messages,
      JsonMapper mapper,
      Clock clock,
      AiClientProperties properties,
      PlatformTransactionManager transactionManager,
      @Qualifier("applicationTaskExecutor") TaskExecutor executor,
      LearningSessionService learningSessions) {
    this.repository = repository;
    this.client = client;
    this.messages = messages;
    this.mapper = mapper;
    this.clock = clock;
    this.leaseDuration = properties.requestTimeout().plusSeconds(30);
    this.transaction = new TransactionTemplate(transactionManager);
    this.executor = executor;
    this.learningSessions = learningSessions;
  }

  /**
   * 사용자 메시지 저장 트랜잭션에 평가 요청을 함께 예약한다.
   *
   * @param request 저장할 AI 평가 요청
   */
  @Transactional
  public void prepare(AiMessageFeedbackRequest request) {
    if (!repository.existsById(request.messageId())) {
      repository.save(
          new MessageFeedbackWork(
              request.messageId(),
              request.sessionId(),
              mapper.writeValueAsString(request),
              LocalDateTime.now(clock)));
    }
  }

  /**
   * 기존 백그라운드 호출에서 현재 시도를 맡아 평가한다.
   *
   * @param messageId 사용자 메시지 ID
   * @return 현재 실행 시도의 처리 상태
   */
  public ProcessingStatus generate(long messageId) {
    Claim claim = claim(messageId);
    if (claim == null) {
      return repository.existsById(messageId)
          ? ProcessingStatus.PREPARING
          : messages.require(messageId).getFeedbackProcessingStatus();
    }
    return execute(claim);
  }

  /**
   * 모든 메시지 결과가 저장됐을 때만 순서대로 최종 AI 요청에 제공한다.
   *
   * @param sessionId 학습 세션 ID
   * @param messageIds 대화 순서의 사용자 메시지 ID 목록
   * @return 순서가 유지된 완성 결과 또는 구버전 호환을 위한 null
   */
  @Transactional(readOnly = true)
  public List<JsonNode> completedResults(long sessionId, List<Long> messageIds) {
    List<MessageFeedbackWork> works = repository.findAllById(messageIds);
    var byId =
        works.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    MessageFeedbackWork::getMessageId, java.util.function.Function.identity()));
    if (messageIds.stream()
        .anyMatch(
            id ->
                !byId.containsKey(id)
                    || byId.get(id).getSessionId() != sessionId
                    || byId.get(id).getResultPayload() == null)) {
      return null; // 구버전 AI가 응답하는 공존 기간에는 기존 최종 요청을 유지한다.
    }
    return messageIds.stream().map(id -> mapper.readTree(byId.get(id).getResultPayload())).toList();
  }

  /** 임대가 만료된 작업만 다시 실행하며 다른 인스턴스의 진행 작업은 유지한다. */
  @Scheduled(fixedDelayString = "${landit.ai.feedback-recovery-interval-ms:30000}")
  public void recover() {
    for (MessageFeedbackWork exhausted :
        repository.findExhausted(LocalDateTime.now(clock), PageRequest.of(0, 10))) {
      transaction.executeWithoutResult(
          status -> {
            if (!messages.lockForFeedbackResult(exhausted.getMessageId())) {
              return;
            }
            if (repository.finish(
                    exhausted.getMessageId(),
                    exhausted.getAttemptToken(),
                    null,
                    false,
                    true,
                    LocalDateTime.now(clock))
                > 0) {
              messages.failFeedback(exhausted.getMessageId());
            }
          });
    }
    for (MessageFeedbackWork work :
        repository.findRecoverable(LocalDateTime.now(clock), PageRequest.of(0, 10))) {
      Claim claim = claim(work.getMessageId());
      if (claim != null) {
        try {
          executor.execute(() -> execute(claim));
        } catch (RuntimeException exception) {
          log.warn("message feedback executor unavailable: messageId={}", work.getMessageId());
        }
      }
    }
  }

  /** 구버전 캐시가 사라진 완료 세션의 저장 발화로 평가 요청을 다시 만든다. */
  @Transactional
  void recoverMissing(long userId, LoadedSessionFeedbackContext context) {
    learningSessions.findOwnedCompletedForUpdate(userId, context.sessionId());
    // 같은 완료 세션의 동시 조회가 없는 작업을 중복 등록하지 못하도록 메시지별 준비를 직렬화한다.
    for (var message : context.userMessages()) {
      var stored = messages.require(message.messageId());
      prepare(
          new AiMessageFeedbackRequest(
              context.sessionId(),
              message.messageId(),
              message.turnNumber(),
              stored.getMessageSequence(),
              context.scenario(),
              message.evaluationContext(),
              message.content()));
    }
    repository.retryMissing(context.sessionId(), LocalDateTime.now(clock));
  }

  /**
   * 누락 평가를 즉시 실행하고 완료까지 트랜잭션 없이 제한 시간만 기다린다.
   *
   * @param context 완료된 학습의 평가 입력
   * @param timeout 복구 대기에 허용되는 시간
   */
  void awaitRecovery(LoadedSessionFeedbackContext context, Duration timeout) {
    long deadline = System.nanoTime() + timeout.toNanos();
    List<Long> ids = context.userMessages().stream().map(UserMessageContext::messageId).toList();
    List<CompletableFuture<Void>> recovery = new ArrayList<>();
    try {
      for (long id : ids) {
        recovery.add(CompletableFuture.runAsync(() -> recoverOrWarmCache(id), executor));
      }
    } catch (TaskRejectedException exception) {
      throw new ApiException(ErrorCode.FEEDBACK_GENERATION_FAILED);
    }
    while (System.nanoTime() < deadline) {
      if (recovery.stream().anyMatch(CompletableFuture::isCompletedExceptionally)) {
        break;
      }
      List<MessageFeedbackWork> works = repository.findAllById(ids);
      if (works.size() == ids.size()
          && recovery.stream().allMatch(CompletableFuture::isDone)
          && works.stream().allMatch(w -> w.getResultPayload() != null || w.isLegacyCompleted())) {
        return;
      }
      if (works.stream().anyMatch(MessageFeedbackWork::isTerminalFailed)) {
        break;
      }
      try {
        TimeUnit.NANOSECONDS.sleep(
            Math.min(
                TimeUnit.MILLISECONDS.toNanos(100), Math.max(0, deadline - System.nanoTime())));
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new ApiException(ErrorCode.FEEDBACK_GENERATION_FAILED);
      }
    }
    throw new ApiException(ErrorCode.FEEDBACK_GENERATION_FAILED);
  }

  private void recoverOrWarmCache(long messageId) {
    MessageFeedbackWork work = repository.findById(messageId).orElseThrow();
    if (work.getResultPayload() == null) {
      generate(messageId);
      return;
    }
    // 구 AI는 완성 결과 필드를 무시하므로 기존 DB 결과를 보존한 채 캐시만 재생성한다.
    AiMessageFeedbackRequest request =
        mapper.readValue(work.getRequestPayload(), AiMessageFeedbackRequest.class);
    AiMessageFeedbackResult result = client.requestMessageFeedback(request);
    validate(result, request);
    if (result.feedbackStatus() == ProcessingStatus.FAILED) {
      throw new ApiException(ErrorCode.FEEDBACK_GENERATION_FAILED);
    }
  }

  private Claim claim(long messageId) {
    return transaction.execute(
        status -> {
          LocalDateTime now = LocalDateTime.now(clock);
          String token = UUID.randomUUID().toString();
          if (repository.claim(messageId, token, now, now.plus(leaseDuration)) == 0) {
            return null;
          }
          MessageFeedbackWork work = repository.findById(messageId).orElseThrow();
          return new Claim(
              mapper.readValue(work.getRequestPayload(), AiMessageFeedbackRequest.class),
              token,
              work.getAttempts());
        });
  }

  private ProcessingStatus execute(Claim claim) {
    String payload = null;
    boolean failed = false;
    boolean legacy = false;
    try {
      AiMessageFeedbackResult result = client.requestMessageFeedback(claim.request());
      validate(result, claim.request());
      failed = result.feedbackStatus() == ProcessingStatus.FAILED;
      legacy =
          !failed && (result.completedFeedback() == null || result.completedFeedback().isNull());
      if (result.completedFeedback() != null && !result.completedFeedback().isNull()) {
        payload = mapper.writeValueAsString(result.completedFeedback());
      }
    } catch (RuntimeException exception) {
      failed = true;
      log.warn(
          "message feedback attempt failed: messageId={}, attempt={}",
          claim.request().messageId(),
          claim.attempt());
    }
    String completed = payload;
    boolean legacyCompleted = legacy;
    boolean attemptFailed = failed;
    boolean exhausted = failed && claim.attempt() >= 3;
    transaction.executeWithoutResult(
        status -> {
          if (!messages.lockForFeedbackResult(claim.request().messageId())) {
            return;
          }
          int updated =
              repository.finish(
                  claim.request().messageId(),
                  claim.token(),
                  completed,
                  legacyCompleted,
                  exhausted,
                  LocalDateTime.now(clock).plusSeconds(30));
          if (updated > 0 && attemptFailed) {
            messages.failFeedback(claim.request().messageId());
          } else if (updated > 0 && (completed != null || legacyCompleted)) {
            messages.retryFeedback(claim.request().messageId());
          }
        });
    return attemptFailed ? ProcessingStatus.FAILED : ProcessingStatus.PREPARING;
  }

  private void validate(AiMessageFeedbackResult result, AiMessageFeedbackRequest request) {
    if (result == null
        || !request.sessionId().equals(result.sessionId())
        || !request.messageId().equals(result.messageId())
        || (result.feedbackStatus() != ProcessingStatus.PREPARING
            && result.feedbackStatus() != ProcessingStatus.FAILED)) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
    JsonNode value = result.completedFeedback();
    if (value == null || value.isNull()) {
      return;
    }
    if (result.feedbackStatus() == ProcessingStatus.FAILED
        || !value.path("schemaVersion").isIntegralNumber()
        || value.path("schemaVersion").asInt() != 1
        || value.path("sessionId").asLong(-1) != request.sessionId()
        || value.path("feedback").path("messageId").asLong(-1) != request.messageId()
        || !request.userMessage().equals(value.path("userMessage").asText())
        || !value.path("adjudicationEvidence").isObject()) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
    for (String field : List.of("contextFit", "clarity", "languageAccuracy")) {
      JsonNode score = value.path("scoreEvidence").path(field);
      if (!score.isIntegralNumber() || score.asInt() < 0 || score.asInt() > 2) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
    }
    for (String field : List.of("candidateWasRepaired", "copyWasRepaired", "copyWasFallback")) {
      if (!value.path(field).isBoolean()) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
    }
  }

  private record Claim(AiMessageFeedbackRequest request, String token, int attempt) {}
}
