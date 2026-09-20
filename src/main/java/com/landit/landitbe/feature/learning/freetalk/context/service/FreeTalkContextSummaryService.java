// 프리톡 세션 요약을 선점·생성·저장하고 다음 요청용 문맥을 제공한다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.config.learning.FreeTalkContextProperties;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkContextSummaryRequest;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkContextSummaryResult;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkContextSummarySourceMessage;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkContextWindow;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkSessionSummary;
import com.landit.landitbe.feature.learning.freetalk.context.client.ai.AiFreeTalkSessionSummaryContent;
import com.landit.landitbe.feature.learning.freetalk.context.domain.FreeTalkContextSummary;
import com.landit.landitbe.feature.learning.freetalk.context.repository.FreeTalkContextSummaryRepository;
import com.landit.landitbe.feature.learning.freetalk.message.dto.FreeTalkMessageReservation;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** 프리톡 원문을 보존한 채 파생 요약을 best-effort로 갱신한다. */
@Slf4j
@Service
public class FreeTalkContextSummaryService {

  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final FreeTalkContextSummaryRepository repository;
  private final ConversationMessageService conversationMessageService;
  private final AiFreeTalkClient aiFreeTalkClient;
  private final FreeTalkContextProperties properties;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final JsonMapper SOURCE_MAPPER = JsonMapper.builder().build();
  private final TransactionTemplate transactionTemplate;
  private final FreeTalkContextExecutionService executor;
  private final FreeTalkContextLifecycleService lifecycle;

  /**
   * 요약 저장소와 원문 조회·AI 호출 의존성을 구성한다.
   *
   * @param repository 요약 상태 저장소
   * @param conversationMessageService 원문 이력 조회 서비스
   * @param aiFreeTalkClient 요약 생성 AI 클라이언트
   * @param properties 요약 정책 설정
   * @param transactionManager 요약 선점과 저장 트랜잭션 관리자
   * @param taskExecutor best-effort 요약 작업 실행기
   * @param lifecycle 사용자와 세션의 유효성 잠금 서비스
   */
  public FreeTalkContextSummaryService(
      FreeTalkContextSummaryRepository repository,
      ConversationMessageService conversationMessageService,
      AiFreeTalkClient aiFreeTalkClient,
      FreeTalkContextProperties properties,
      org.springframework.transaction.PlatformTransactionManager transactionManager,
      FreeTalkContextExecutionService taskExecutor,
      FreeTalkContextLifecycleService lifecycle) {
    this.repository = repository;
    this.conversationMessageService = conversationMessageService;
    this.aiFreeTalkClient = aiFreeTalkClient;
    this.properties = properties;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.executor = taskExecutor;
    this.lifecycle = lifecycle;
  }

  /**
   * 새 세션의 생성 트랜잭션에서만 요약 정책을 고정한다.
   *
   * @param userId 새 세션 소유자
   * @param freeTalkSessionId 새 프리톡 세션 ID
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void initialize(long userId, long freeTalkSessionId) {
    if (eligible(userId)) {
      repository.save(
          FreeTalkContextSummary.start(
              freeTalkSessionId, "v1", properties.summarySourceMaxBytes()));
    }
  }

  /** 활성화된 사용자에게 저장된 요약 문맥을 제공한다. */
  public AiFreeTalkContextWindow snapshot(long userId, long freeTalkSessionId) {
    if (!eligible(userId)) {
      return AiFreeTalkContextWindow.disabled();
    }
    return repository
        .findById(freeTalkSessionId)
        .map(this::toWindow)
        .orElse(AiFreeTalkContextWindow.disabled());
  }

  /** 현재 응답이 완료된 뒤 요약 작업을 비동기로 등록한다. */
  public void dispatchIfNeeded(FreeTalkMessageReservation reservation) {
    if (!eligible(reservation.userId())) {
      return;
    }
    try {
      executor.execute(
          () -> {
            try {
              summarize(reservation);
            } catch (RuntimeException exception) {
              log.info(
                  "프리톡 컨텍스트 요약 작업 상태를 처리하지 못했습니다. sessionId={}", reservation.freeTalkSessionId());
            }
          });
    } catch (RuntimeException exception) {
      log.info("프리톡 컨텍스트 요약 작업을 등록하지 못했습니다. sessionId={}", reservation.freeTalkSessionId());
    }
  }

  private void summarize(FreeTalkMessageReservation reservation) {
    List<SessionHistoryMessageSnapshot> messages =
        conversationMessageService.findAll(reservation.historyId());
    PendingSummary pending = prepare(reservation, messages);
    if (pending == null) {
      return;
    }
    try {
      AiFreeTalkContextSummaryResult result =
          aiFreeTalkClient.generateContextSummary(pending.request());
      complete(pending, result);
    } catch (RuntimeException exception) {
      handleFailure(pending, exception);
      log.info("프리톡 컨텍스트 요약 생성에 실패했습니다. sessionId={}", reservation.freeTalkSessionId());
    }
  }

  private PendingSummary prepare(
      FreeTalkMessageReservation reservation, List<SessionHistoryMessageSnapshot> messages) {
    return transactionTemplate.execute(
        status -> {
          if (!eligible(reservation.userId())
              || !lifecycle.lockActive(
                  reservation.userId(),
                  reservation.learningSessionId(),
                  reservation.freeTalkSessionId())) {
            return null;
          }
          FreeTalkContextSummary state =
              repository.findByIdForUpdate(reservation.freeTalkSessionId()).orElse(null);
          if (state == null) {
            return null;
          }
          Instant now = repository.currentTime();
          List<List<SessionHistoryMessageSnapshot>> rounds =
              FreeTalkSummaryWindow.rounds(messages, state.getCoveredThroughSequence());
          if (!shouldSummarize(rounds)
              || state.getSuspendedReason() != null
              || activeLease(state, now)
              || (state.getNextAttemptAt() != null && state.getNextAttemptAt().isAfter(now))) {
            return null;
          }
          List<SessionHistoryMessageSnapshot> source = sourceMessages(messages, state);
          if (source.isEmpty()) {
            return null;
          }
          String token = UUID.randomUUID().toString();
          state.claim(token, now.plusSeconds(properties.leaseSeconds()));
          repository.save(state);
          AiFreeTalkContextSummaryRequest request =
              new AiFreeTalkContextSummaryRequest(
                  reservation.freeTalkSessionId(),
                  "v1",
                  state.getRevision(),
                  toContent(state.getSummaryContent()),
                  state.getCoveredThroughSequence(),
                  source.getLast().getMessageSequence(),
                  "Asia/Seoul",
                  source.stream().map(this::toSourceMessage).toList());
          return new PendingSummary(
              reservation.freeTalkSessionId(),
              token,
              state.getRevision(),
              request,
              FreeTalkSummaryWindow.rounds(source, state.getCoveredThroughSequence()).size(),
              sourceBytes(rounds.getFirst()),
              reservation.userId(),
              reservation.learningSessionId());
        });
  }

  private void complete(PendingSummary pending, AiFreeTalkContextSummaryResult result) {
    if (!compatible(pending, result)) {
      defer(pending);
      log.info("프리톡 컨텍스트 요약 경계 검증에 실패했습니다. sessionId={}", pending.sessionId());
      return;
    }
    transactionTemplate.executeWithoutResult(
        status -> {
          if (!eligible(pending.userId())
              || !lifecycle.lockActive(
                  pending.userId(), pending.learningSessionId(), pending.sessionId())) {
            return;
          }
          repository
              .findByIdForUpdate(pending.sessionId())
              .filter(state -> current(pending, state))
              .ifPresent(
                  state -> {
                    state.complete(
                        OBJECT_MAPPER.valueToTree(result.summary()),
                        pending.request().targetThroughSequence(),
                        properties.summarySourceMaxBytes());
                    repository.save(state);
                  });
        });
  }

  private boolean current(PendingSummary pending, FreeTalkContextSummary state) {
    return state.ownsLease(pending.leaseToken(), repository.currentTime())
        && state.getRevision() == pending.revision()
        && state.getPolicyVersion().equals(pending.request().policyVersion())
        && state.getCoveredThroughSequence() == pending.request().coveredThroughSequence();
  }

  private boolean compatible(PendingSummary pending, AiFreeTalkContextSummaryResult result) {
    AiFreeTalkContextSummaryRequest request = pending.request();
    return result != null
        && request.policyVersion().equals(result.policyVersion())
        && request.baseRevision() == result.baseRevision()
        && request.targetThroughSequence() == result.coveredThroughSequence()
        && result.summary() != null;
  }

  private void defer(PendingSummary pending) {
    transactionTemplate.executeWithoutResult(
        status ->
            repository
                .findByIdForUpdate(pending.sessionId())
                .filter(state -> current(pending, state))
                .ifPresent(
                    state ->
                        state.defer(
                            repository.currentTime().plusSeconds(properties.retryDelaySeconds()))));
  }

  private boolean shouldSummarize(List<List<SessionHistoryMessageSnapshot>> rounds) {
    if (rounds.size() <= properties.recentRounds()) {
      return false;
    }
    return rounds.size() >= properties.summaryTriggerRounds()
        || sourceBytes(rounds.stream().flatMap(List::stream).toList()) >= 12000;
  }

  private List<SessionHistoryMessageSnapshot> sourceMessages(
      List<SessionHistoryMessageSnapshot> messages, FreeTalkContextSummary state) {
    List<List<SessionHistoryMessageSnapshot>> rounds =
        FreeTalkSummaryWindow.rounds(messages, state.getCoveredThroughSequence());
    int count = Math.max(0, rounds.size() - properties.recentRounds());
    return FreeTalkSummaryWindow.source(
        rounds.subList(0, count), state.getSourceByteLimit(), this::sourceBytes);
  }

  private int sourceBytes(List<SessionHistoryMessageSnapshot> messages) {
    return SOURCE_MAPPER.writeValueAsBytes(messages.stream().map(this::toSourceMessage).toList())
        .length;
  }

  private void handleFailure(PendingSummary pending, RuntimeException exception) {
    if (!(exception instanceof ApiException apiException)
        || apiException.getErrorCode() != ErrorCode.FREE_TALK_SUMMARY_INPUT_TOO_LARGE) {
      defer(pending);
      return;
    }
    transactionTemplate.executeWithoutResult(
        status ->
            repository
                .findByIdForUpdate(pending.sessionId())
                .filter(state -> current(pending, state))
                .ifPresent(
                    state -> {
                      if (pending.rounds() == 1) {
                        state.suspend("OVERSIZED_UNIT");
                      } else {
                        int limit =
                            Math.max(
                                pending.firstRoundBytes(),
                                SOURCE_MAPPER.writeValueAsBytes(pending.request().sourceMessages())
                                        .length
                                    / 2);
                        state.reduceSourceLimit(
                            limit,
                            repository.currentTime().plusSeconds(properties.retryDelaySeconds()));
                      }
                    }));
  }

  private AiFreeTalkContextSummarySourceMessage toSourceMessage(
      SessionHistoryMessageSnapshot message) {
    return new AiFreeTalkContextSummarySourceMessage(
        message.getMessageSequence(),
        message.getId(),
        message.getTurnNumber(),
        message.getRole().name(),
        message.getContent(),
        message.getCreatedAt().atZone(KOREA_ZONE_ID).toOffsetDateTime());
  }

  private AiFreeTalkContextWindow toWindow(FreeTalkContextSummary state) {
    if (state.getSummaryContent() == null) {
      return new AiFreeTalkContextWindow(state.getPolicyVersion(), null, false);
    }
    AiFreeTalkSessionSummaryContent content = toContent(state.getSummaryContent());
    if (content == null) {
      return new AiFreeTalkContextWindow(state.getPolicyVersion(), null, true);
    }
    return new AiFreeTalkContextWindow(
        state.getPolicyVersion(),
        new AiFreeTalkSessionSummary(
            state.getRevision(), state.getCoveredThroughSequence(), content),
        false);
  }

  private AiFreeTalkSessionSummaryContent toContent(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    try {
      return OBJECT_MAPPER.treeToValue(node, AiFreeTalkSessionSummaryContent.class);
    } catch (com.fasterxml.jackson.core.JacksonException exception) {
      return null;
    }
  }

  private boolean eligible(long userId) {
    return properties.enabled() && properties.allowedUserIds().contains(userId);
  }

  private boolean activeLease(FreeTalkContextSummary state, Instant now) {
    return state.getLeaseToken() != null
        && state.getLeaseUntil() != null
        && state.getLeaseUntil().isAfter(now);
  }

  private record PendingSummary(
      long sessionId,
      String leaseToken,
      int revision,
      AiFreeTalkContextSummaryRequest request,
      int rounds,
      int firstRoundBytes,
      long userId,
      long learningSessionId) {}
}
