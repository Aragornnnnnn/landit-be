// 실제 DB 트랜잭션에서 푸시 재시도 선점 잠금의 동시성을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.domain.PushDevice;
import com.landit.landitbe.feature.notification.repository.PushDeliveryRepository;
import com.landit.landitbe.feature.notification.repository.PushDeviceRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 실제 DB 트랜잭션에서 푸시 재시도 선점 잠금의 동시성을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class PushDeliveryRetryConcurrencyIntegrationTests {

  private static final long USER_ID = 996002L;
  private static final UUID INSTALLATION_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440022");
  private static final String EXPO_PUSH_TOKEN = "ExponentPushToken[retry-concurrency-token]";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private PlatformTransactionManager transactionManager;

  @Autowired private PushDeviceRepository pushDeviceRepository;

  @Autowired private PushDeliveryRepository pushDeliveryRepository;

  @Autowired private PushDeliveryService pushDeliveryService;

  private ExecutorService executor;
  private PreparePushDeliveryCommand command;

  /** 커밋된 재시도 가능 발송 이력과 동시 실행용 Executor를 준비한다. */
  @BeforeEach
  void setUp() {
    cleanDatabase();
    executor = Executors.newFixedThreadPool(2);
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level,
            push_permission_status, status, created_at, updated_at
        )
        values (?, 'retry-concurrency-user', 'EN', 'KR', 1, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID);
    PushDevice pushDevice =
        pushDeviceRepository.saveAndFlush(
            PushDevice.create(USER_ID, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN));
    command =
        new PreparePushDeliveryCommand(
            USER_ID,
            pushDevice.getId(),
            NotificationType.REVIEW_LEARNING,
            "review-reminder:"
                + LocalDate.of(2026, 7, 24)
                + ":"
                + USER_ID
                + ":"
                + pushDevice.getId(),
            "복습할 시간이에요",
            "오늘의 표현을 다시 볼까요?",
            "/expressions");
    PreparedPushDelivery prepared = pushDeliveryService.prepare(command).orElseThrow();
    pushDeliveryService.markRetryable(prepared.pushDeliveryId());
  }

  /** Executor를 종료하고 테스트가 커밋한 발송 이력과 설치, 사용자를 제거한다. */
  @AfterEach
  void tearDown() {
    executor.shutdownNow();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시성 테스트 Executor가 제한시간 안에 종료되지 않았습니다.");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("동시성 테스트 Executor 종료 대기가 중단됐습니다.", exception);
    } finally {
      cleanDatabase();
    }
  }

  /** 첫 트랜잭션의 재시도 선점이 커밋될 때까지 두 번째 트랜잭션이 같은 행 잠금을 기다린다. */
  @Test
  void allowsOnlyOneConcurrentRetryClaim() throws Exception {
    CountDownLatch firstPrepared = new CountDownLatch(1);
    CountDownLatch allowFirstCommit = new CountDownLatch(1);
    CountDownLatch secondStarted = new CountDownLatch(1);
    Future<Optional<PreparedPushDelivery>> first = null;
    Future<Optional<PreparedPushDelivery>> second = null;

    try {
      first =
          executor.submit(
              () ->
                  new TransactionTemplate(transactionManager)
                      .execute(
                          status -> {
                            Optional<PreparedPushDelivery> result =
                                pushDeliveryService.prepare(command);
                            firstPrepared.countDown();
                            awaitLatch(allowFirstCommit);
                            return result;
                          }));
      assertThat(firstPrepared.await(5, TimeUnit.SECONDS)).isTrue();

      second =
          executor.submit(
              () ->
                  new TransactionTemplate(transactionManager)
                      .execute(
                          status -> {
                            secondStarted.countDown();
                            return pushDeliveryService.prepare(command);
                          }));
      assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
      Future<Optional<PreparedPushDelivery>> waitingSecond = second;
      assertThatThrownBy(() -> waitingSecond.get(300, TimeUnit.MILLISECONDS))
          .isInstanceOf(TimeoutException.class);

      allowFirstCommit.countDown();
      Optional<PreparedPushDelivery> firstResult = first.get(5, TimeUnit.SECONDS);
      Optional<PreparedPushDelivery> secondResult = second.get(5, TimeUnit.SECONDS);

      assertThat(firstResult).isPresent();
      assertThat(secondResult).isEmpty();
      assertThat(pushDeliveryRepository.count()).isEqualTo(1);
    } finally {
      allowFirstCommit.countDown();
      cancelWhenStillRunning(first);
      cancelWhenStillRunning(second);
    }
  }

  /** 같은 설치와 중복 키로 동시에 최초 발송을 선점해도 발송 이력은 한 건만 추가한다. */
  @Test
  void createsOnlyOneDeliveryForConcurrentFirstSend() throws Exception {
    PreparePushDeliveryCommand firstSendCommand =
        new PreparePushDeliveryCommand(
            USER_ID,
            command.pushDeviceId(),
            NotificationType.REVIEW_LEARNING,
            command.deduplicationKey() + ":first-send",
            command.title(),
            command.body(),
            command.deepLink());
    CountDownLatch start = new CountDownLatch(1);
    Future<Optional<PreparedPushDelivery>> first =
        executor.submit(
            () -> {
              awaitLatch(start);
              return pushDeliveryService.prepare(firstSendCommand);
            });
    Future<Optional<PreparedPushDelivery>> second =
        executor.submit(
            () -> {
              awaitLatch(start);
              return pushDeliveryService.prepare(firstSendCommand);
            });

    start.countDown();

    assertThat(
            first.get(5, TimeUnit.SECONDS).isPresent()
                ^ second.get(5, TimeUnit.SECONDS).isPresent())
        .isTrue();
    assertThat(pushDeliveryRepository.count()).isEqualTo(2);
  }

  /** 제한시간 안에 동시 실행 단계가 열리지 않으면 테스트를 실패시킨다. */
  private void awaitLatch(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시성 테스트 단계가 제한시간 안에 열리지 않았습니다.");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("동시성 테스트 대기가 중단됐습니다.", exception);
    }
  }

  /** 완료되지 않은 Future만 취소해 실패 경로에서도 Executor가 종료되게 한다. */
  private void cancelWhenStillRunning(Future<?> future) {
    if (future != null && !future.isDone()) {
      future.cancel(true);
    }
  }

  /** 테스트가 사용하는 발송 이력, 설치, 사용자 순서로 DB 데이터를 제거한다. */
  private void cleanDatabase() {
    jdbcTemplate.update("delete from push_delivery where user_profile_id = ?", USER_ID);
    jdbcTemplate.update("delete from user_push_token where user_profile_id = ?", USER_ID);
    jdbcTemplate.update("delete from user_profile where id = ?", USER_ID);
  }
}
