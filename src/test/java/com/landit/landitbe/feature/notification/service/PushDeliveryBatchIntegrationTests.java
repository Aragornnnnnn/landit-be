// 격리된 H2 또는 로컬 PostgreSQL에서 묶음 발송의 SQL 수·롤백·동시 선점을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.client.NotificationSender;
import com.landit.landitbe.feature.notification.client.PushMessage;
import com.landit.landitbe.feature.notification.client.PushNotificationException;
import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.domain.NotificationContentVariant;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.domain.PushDelivery;
import com.landit.landitbe.feature.notification.domain.PushDeliveryStatus;
import com.landit.landitbe.feature.notification.domain.UserPushToken;
import com.landit.landitbe.feature.notification.domain.UserPushTokenStatus;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.feature.notification.repository.PushDeliveryBatchRepository;
import com.landit.landitbe.feature.notification.repository.PushDeliveryRepository;
import com.landit.landitbe.feature.notification.repository.UserPushTokenRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** 외부 API 없이 실제 JDBC 실행과 트랜잭션 경합을 확인한다. */
@SpringJUnitConfig(PushDeliveryBatchIntegrationTests.Config.class)
class PushDeliveryBatchIntegrationTests {

  @Autowired private PushDeliveryService deliveries;
  @Autowired private PushDeliveryRepository repository;
  @Autowired private UserPushTokenRepository tokens;
  @Autowired private UserPushTokenDeliveryService tokenService;
  @Autowired private CountingDataSource dataSource;
  @Autowired private PlatformTransactionManager transactionManager;

  @BeforeEach
  void clean() {
    repository.deleteAllInBatch();
    tokens.deleteAllInBatch();
    dataSource.reset();
  }

  /** 동일 500토큰의 기존 경로와 새 dispatch 경로에서 실제 SQL·쓰기 커밋 수를 비교한다. */
  @Test
  void reducesFiveHundredTokenSqlFrom3501To32() {
    List<UserPushToken> seeded = seed(500);
    List<Long> users = seeded.stream().map(UserPushToken::getUserProfileId).toList();
    dataSource.reset();
    tokenService.findSendableTokenIdsByUserProfileIds(users);
    for (UserPushToken token : seeded) {
      deliveries.findAcceptedDeliveryIds("push:before:" + token.getUserProfileId() + ":");
      PreparedPushDelivery prepared = deliveries.prepare(command(token, "before")).orElseThrow();
      deliveries.recordTicketResult(prepared.pushDeliveryId(), PushTicketResult.accepted("ticket"));
    }
    assertThat(dataSource.sql).hasSize(3501);
    assertThat(dataSource.writeCommits.get()).isEqualTo(1000);

    NotificationSender sender = sender();
    PushQueuePublisher publisher = mock(PushQueuePublisher.class);
    NotificationDispatchService dispatch = dispatch(sender, publisher);
    dataSource.reset();
    NotificationDispatchResult result =
        dispatch.sendAll(seeded.stream().map(t -> sendCommand(t, "after")).toList());
    assertThat(result).isEqualTo(new NotificationDispatchResult(500, 5, 500, 0));
    assertThat(dataSource.sql).hasSize(32);
    assertThat(dataSource.writeCommits.get()).isEqualTo(10);
    assertThat(dataSource.sql.stream().filter(s -> s.startsWith("insert into push_delivery")))
        .hasSize(5);
    assertThat(dataSource.sql.stream().filter(s -> s.startsWith("update push_delivery")))
        .hasSize(5);
    verify(sender, times(5)).send(anyList());
    System.out.println("LAN468_SQL baseline=3501 batched=32 writeCommits=1000->10 tokens=500");
  }

  /** 새 행의 키·문구 스냅샷을 보존하고 Ticket 결과를 응답 순서로 연결한다. */
  @Test
  void mapsGeneratedKeysAndTicketResultsWithoutDependingOnLockOrder() {
    List<UserPushToken> seeded = seed(3);
    List<PreparePushDeliveryCommand> commands =
        seeded.reversed().stream().map(t -> command(t, "event")).toList();
    List<PreparedPushDelivery> prepared = deliveries.prepareAll(commands);
    assertThat(prepared)
        .extracting(PreparedPushDelivery::expoPushToken)
        .containsExactlyElementsOf(
            seeded.reversed().stream().map(UserPushToken::getExpoPushToken).toList());
    List<Long> ids =
        prepared.stream().map(PreparedPushDelivery::pushDeliveryId).toList().reversed();
    deliveries.recordTicketResults(
        ids,
        List.of(
            PushTicketResult.accepted("ticket-a"),
            PushTicketResult.failed("DeviceNotRegistered"),
            PushTicketResult.failed("DeveloperError")));
    assertThat(repository.findById(ids.get(0)).orElseThrow().getExpoTicketId())
        .isEqualTo("ticket-a");
    assertThat(repository.findById(ids.get(1)).orElseThrow().getStatus())
        .isEqualTo(PushDeliveryStatus.FAILED);
    assertThat(repository.findById(ids.get(2)).orElseThrow().getErrorCode())
        .isEqualTo("DeveloperError");
    assertThat(tokens.findById(seeded.get(1).getId()).orElseThrow().getStatus())
        .isEqualTo(UserPushTokenStatus.REVOKED);
    assertThat(tokens.findById(seeded.get(2).getId()).orElseThrow().getStatus())
        .isEqualTo(UserPushTokenStatus.ACTIVE);
    assertThat(repository.findAll())
        .allSatisfy(
            d -> {
              assertThat(d.getCreatedAt()).isNotNull();
              assertThat(d.getUpdatedAt()).isNotNull();
              assertThat(d.getContentVariant())
                  .isEqualTo(NotificationContentVariant.EXPRESSION_DYNAMIC);
            });
    deliveries.recordReceiptResult(ids.getFirst(), PushReceiptResult.delivered());
    deliveries.recordTicketResults(
        List.of(ids.getFirst()), List.of(PushTicketResult.failed("late")));
    assertThat(repository.findById(ids.getFirst()).orElseThrow().getStatus())
        .isEqualTo(PushDeliveryStatus.DELIVERED);
  }

  /** 소유자 변경·해제 Token은 제외하고 재시도 표식은 한 번만 소비한다. */
  @Test
  void preservesOwnerRevocationAndRetryGuards() {
    List<UserPushToken> seeded = seed(3);
    List<PreparePushDeliveryCommand> commands =
        seeded.stream().map(t -> command(t, "event")).toList();
    List<PreparedPushDelivery> prepared = deliveries.prepareAll(commands);
    prepared.forEach(p -> deliveries.markRetryable(p.pushDeliveryId()));
    seeded.get(0).claim(900L, AppPlatform.IOS);
    seeded.get(1).revoke();
    tokens.saveAllAndFlush(seeded);
    List<PreparedPushDelivery> retry = deliveries.prepareAll(commands);
    assertThat(retry).containsExactly(prepared.get(2));
    assertThat(deliveries.prepareAll(commands)).isEmpty();
    assertThat(
            deliveries.prepareAll(
                seeded.stream()
                    .map(t -> commandWithOwner(t, "new", 1L + seeded.indexOf(t)))
                    .toList()))
        .extracting(PreparedPushDelivery::expoPushToken)
        .containsExactly(seeded.get(2).getExpoPushToken());
  }

  /** 묶음 중 잘못된 Ticket이 있으면 앞선 전이도 롤백한다. */
  @Test
  void rollsBackWholeTicketBatchOnInvalidResult() {
    List<Long> ids =
        deliveries.prepareAll(seed(2).stream().map(t -> command(t, "event")).toList()).stream()
            .map(PreparedPushDelivery::pushDeliveryId)
            .toList();
    assertThatThrownBy(
            () ->
                deliveries.recordTicketResults(
                    ids,
                    List.of(
                        PushTicketResult.accepted("valid"), new PushTicketResult(true, "", null))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(repository.findAll())
        .allSatisfy(d -> assertThat(d.getStatus()).isEqualTo(PushDeliveryStatus.REQUESTED));
  }

  /** 사용자 두 명의 발송 선점 중 한 행이 실패하면 INSERT 전체가 롤백된다. */
  @Test
  void rollsBackWholeInsertBatch() {
    List<UserPushToken> seeded = seed(2);
    PreparePushDeliveryCommand valid = command(seeded.getFirst(), "event");
    PreparePushDeliveryCommand invalid =
        new PreparePushDeliveryCommand(
            2L,
            seeded.getLast().getId(),
            NotificationType.CONTINUE_EXPRESSION,
            "bad",
            "x".repeat(256),
            "body",
            "/home");
    assertThatThrownBy(() -> deliveries.prepareAll(List.of(valid, invalid)))
        .isInstanceOf(RuntimeException.class);
    assertThat(repository.count()).isZero();
  }

  /** JDBC 저장도 외부 Spring 트랜잭션의 롤백에 함께 참여한다. */
  @Test
  void rollsBackJdbcInsertAndUpdateWithEnclosingTransaction() {
    UserPushToken token = seed(1).getFirst();
    assertThatThrownBy(
            () ->
                new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                        status -> {
                          PreparedPushDelivery prepared =
                              deliveries.prepareAll(List.of(command(token, "event"))).getFirst();
                          deliveries.recordTicketResults(
                              List.of(prepared.pushDeliveryId()),
                              List.of(PushTicketResult.accepted("ticket")));
                          throw new IllegalStateException("rollback after JDBC writes");
                        }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("rollback after JDBC writes");
    assertThat(repository.count()).isZero();
  }

  /** Receipt 예약 실패 뒤 Token이 해제돼도 접수 이력으로 복구하며 Expo에 다시 보내지 않는다. */
  @Test
  void recoversAcceptedReceiptsAfterPublisherFailureWithoutResending() {
    List<UserPushToken> seeded = seed(2);
    NotificationSender sender = sender();
    PushQueuePublisher publisher = mock(PushQueuePublisher.class);
    doThrow(new PushNotificationException("partial failure"))
        .doNothing()
        .when(publisher)
        .scheduleReceiptChecks(anyList(), org.mockito.ArgumentMatchers.eq(1));
    NotificationDispatchService dispatch = dispatch(sender, publisher);
    List<SendPushNotificationCommand> commands =
        seeded.stream().map(t -> sendCommand(t, "event")).toList();
    assertThatThrownBy(() -> dispatch.sendAll(commands))
        .isInstanceOf(PushNotificationException.class);
    assertThat(repository.findAll())
        .allSatisfy(d -> assertThat(d.getStatus()).isEqualTo(PushDeliveryStatus.TICKET_ACCEPTED));
    seeded.forEach(UserPushToken::revoke);
    tokens.saveAllAndFlush(seeded);
    assertThat(dispatch.sendAll(commands).preparedDeliveries()).isZero();
    verify(sender, times(1)).send(anyList());
    verify(publisher, times(2))
        .scheduleReceiptChecks(anyList(), org.mockito.ArgumentMatchers.eq(1));
  }

  /** 접두어 안의 %, _를 패턴으로 해석하거나 다른 이벤트로 확대하지 않는다. */
  @Test
  void matchesEventPrefixesLiterally() {
    UserPushToken token = seed(1).getFirst();
    for (String event : List.of("a%_", "abc", "a%_extra")) {
      PreparedPushDelivery p = deliveries.prepareAll(List.of(command(token, event))).getFirst();
      deliveries.recordTicketResults(
          List.of(p.pushDeliveryId()), List.of(PushTicketResult.accepted(event)));
    }
    assertThat(deliveries.findAcceptedDeliveryIdsForEvents(List.of("push:a%_:1:"))).hasSize(1);
  }

  /** 같은 Token을 포함한 역순 신규 묶음은 이력을 한 번만 선점하고 교착되지 않는다. */
  @Test
  void claimsOverlappingNewBatchesOnlyOnce() throws Exception {
    List<PreparePushDeliveryCommand> commands =
        seed(3).stream().map(t -> command(t, "event")).toList();
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () -> {
                await(start);
                return deliveries.prepareAll(commands);
              });
      var second =
          executor.submit(
              () -> {
                await(start);
                return deliveries.prepareAll(commands.reversed());
              });
      start.countDown();
      List<Long> ids = new ArrayList<>();
      first.get(10, TimeUnit.SECONDS).forEach(p -> ids.add(p.pushDeliveryId()));
      second.get(10, TimeUnit.SECONDS).forEach(p -> ids.add(p.pushDeliveryId()));
      assertThat(ids).hasSize(3).doesNotHaveDuplicates();
      assertThat(repository.count()).isEqualTo(3);
    }
  }

  /** 재시도 묶음이 잠금을 보유하면 기존 단건 경로도 기다리고 표식을 중복 소비하지 않는다. */
  @Test
  void serializesBatchRetryWithLegacySinglePrepare() throws Exception {
    PreparePushDeliveryCommand command = command(seed(1).getFirst(), "event");
    PreparedPushDelivery original = deliveries.prepareAll(List.of(command)).getFirst();
    deliveries.markRetryable(original.pushDeliveryId());
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch commit = new CountDownLatch(1);
    CountDownLatch secondStarted = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () ->
                  new TransactionTemplate(transactionManager)
                      .execute(
                          status -> {
                            List<PreparedPushDelivery> result =
                                deliveries.prepareAll(List.of(command));
                            locked.countDown();
                            await(commit);
                            return result;
                          }));
      try {
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        var second =
            executor.submit(
                () -> {
                  secondStarted.countDown();
                  return deliveries.prepare(command);
                });
        assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
        commit.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS)).containsExactly(original);
        assertThat(second.get(5, TimeUnit.SECONDS)).isEmpty();
      } finally {
        commit.countDown();
      }
    }
  }

  private void await(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시성 테스트 제한시간 초과");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  /** Token 이전 트랜잭션이 먼저 잠갔다면 묶음 선점은 커밋된 새 소유자를 보고 제외한다. */
  @Test
  void waitsForTokenOwnershipChangeBeforePreparing() throws Exception {
    UserPushToken token = seed(1).getFirst();
    assertBlockedPreparationAfterTokenChange(
        token,
        () -> tokens.findByIdForUpdate(token.getId()).orElseThrow().claim(999L, AppPlatform.IOS));
    assertThat(repository.count()).isZero();
  }

  /** Receipt의 토큰 무효화와 신규 묶음 선점이 겹쳐도 무효 토큰을 다시 선점하지 않는다. */
  @Test
  void waitsForInvalidTokenReceiptBeforePreparingNewEvent() throws Exception {
    UserPushToken token = seed(1).getFirst();
    PreparedPushDelivery original =
        deliveries.prepareAll(List.of(command(token, "old"))).getFirst();
    deliveries.recordTicketResults(
        List.of(original.pushDeliveryId()), List.of(PushTicketResult.accepted("ticket")));
    assertBlockedPreparationAfterTokenChange(
        token,
        () ->
            deliveries.recordReceiptResult(
                original.pushDeliveryId(), PushReceiptResult.failed("BadDeviceToken")));
    assertThat(repository.count()).isEqualTo(1);
  }

  private void assertBlockedPreparationAfterTokenChange(UserPushToken token, Runnable change)
      throws Exception {
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch commit = new CountDownLatch(1);
    CountDownLatch started = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () ->
                  new TransactionTemplate(transactionManager)
                      .executeWithoutResult(
                          status -> {
                            change.run();
                            locked.countDown();
                            await(commit);
                          }));
      try {
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        var second =
            executor.submit(
                () -> {
                  started.countDown();
                  return deliveries.prepareAll(List.of(command(token, "new")));
                });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
        commit.countDown();
        first.get(5, TimeUnit.SECONDS);
        assertThat(second.get(5, TimeUnit.SECONDS)).isEmpty();
      } finally {
        commit.countDown();
      }
    }
  }

  /** 빈 입력은 SQL 없이 반환하고 제한 초과 입력은 DB에 접근하기 전에 거부한다. */
  @Test
  void validatesBatchBoundaries() {
    final UserPushToken token = seed(1).getFirst();
    dataSource.reset();
    assertThat(deliveries.prepareAll(List.of())).isEmpty();
    deliveries.recordTicketResults(List.of(), List.of());
    assertThatThrownBy(
            () ->
                deliveries.prepareAll(java.util.Collections.nCopies(101, command(token, "event"))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(dataSource.sql).isEmpty();
  }

  private List<UserPushToken> seed(int size) {
    return tokens.saveAllAndFlush(
        LongStream.rangeClosed(1, size)
            .mapToObj(
                id ->
                    UserPushToken.register(
                        id, AppPlatform.IOS, "ExponentPushToken[test-" + id + "]"))
            .toList());
  }

  private PreparePushDeliveryCommand command(UserPushToken token, String event) {
    return commandWithOwner(token, event, token.getUserProfileId());
  }

  private PreparePushDeliveryCommand commandWithOwner(
      UserPushToken token, String event, Long owner) {
    return new PreparePushDeliveryCommand(
        owner,
        token.getId(),
        NotificationType.CONTINUE_EXPRESSION,
        NotificationContentVariant.EXPRESSION_DYNAMIC,
        "push:" + event + ":" + owner + ":" + token.getId(),
        "표현 알림",
        "표현을 배워보세요.",
        "/expressions");
  }

  private SendPushNotificationCommand sendCommand(UserPushToken token, String event) {
    return new SendPushNotificationCommand(
        event + ":" + token.getUserProfileId(),
        token.getUserProfileId(),
        NotificationType.CONTINUE_EXPRESSION,
        "표현 알림",
        "표현을 배워보세요.",
        "/expressions");
  }

  private NotificationSender sender() {
    NotificationSender sender = mock(NotificationSender.class);
    when(sender.send(anyList()))
        .thenAnswer(
            call ->
                call.<List<PushMessage>>getArgument(0).stream()
                    .map(m -> PushTicketResult.accepted("ticket-" + m.expoPushToken()))
                    .toList());
    return sender;
  }

  private NotificationDispatchService dispatch(
      NotificationSender sender, PushQueuePublisher publisher) {
    return new NotificationDispatchService(
        tokenService, deliveries, sender, publisher, new SimpleMeterRegistry());
  }

  /** 외부 애플리케이션 설정 없이 notification 엔티티·Service만 구성한다. */
  @Configuration
  @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = PushDeliveryRepository.class)
  @Import({
    PushDeliveryService.class,
    UserPushTokenDeliveryService.class,
    PushDeliveryBatchRepository.class
  })
  static class Config {

    @Bean
    CountingDataSource dataSource() {
      String url = System.getenv("LAN468_TEST_JDBC_URL");
      if (url == null) {
        url =
            "jdbc:h2:mem:lan468;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
                + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
      } else if (!url.equals("jdbc:postgresql://127.0.0.1:55468/lan468_test")) {
        throw new IllegalArgumentException("전용 로컬 테스트 DB만 허용합니다.");
      }
      return new CountingDataSource(new DriverManagerDataSource(url, "lan468_test", ""));
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
      bean.setDataSource(source);
      bean.setPackagesToScan(PushDelivery.class.getPackageName());
      bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      bean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
      return bean;
    }

    @Bean
    PlatformTransactionManager transactionManager(
        jakarta.persistence.EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }

    @Bean
    NamedParameterJdbcTemplate jdbc(DataSource source) {
      return new NamedParameterJdbcTemplate(source);
    }
  }

  /** 실행된 PreparedStatement와 쓰기 커밋을 계측한다. */
  static class CountingDataSource extends DelegatingDataSource {
    final List<String> sql = new CopyOnWriteArrayList<>();
    final AtomicInteger writeCommits = new AtomicInteger();

    CountingDataSource(DataSource target) {
      super(target);
    }

    void reset() {
      sql.clear();
      writeCommits.set(0);
    }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection() throws SQLException {
      Connection delegate = super.getConnection();
      java.util.concurrent.atomic.AtomicBoolean readOnly =
          new java.util.concurrent.atomic.AtomicBoolean();
      return (Connection)
          Proxy.newProxyInstance(
              Connection.class.getClassLoader(),
              new Class<?>[] {Connection.class},
              (proxy, method, args) -> {
                if (method.getName().equals("setReadOnly")) {
                  readOnly.set((Boolean) args[0]);
                }
                if (method.getName().equals("commit") && !readOnly.get()) {
                  writeCommits.incrementAndGet();
                }
                Object result = invoke(delegate, method, args);
                if (method.getName().equals("prepareStatement")) {
                  return statement((PreparedStatement) result, (String) args[0]);
                }
                return result;
              });
    }

    private PreparedStatement statement(PreparedStatement delegate, String query) {
      return (PreparedStatement)
          Proxy.newProxyInstance(
              PreparedStatement.class.getClassLoader(),
              new Class<?>[] {PreparedStatement.class},
              (proxy, method, args) -> {
                if (List.of("execute", "executeQuery", "executeUpdate", "executeBatch")
                    .contains(method.getName())) {
                  sql.add(query.toLowerCase(java.util.Locale.ROOT).strip());
                }
                return invoke(delegate, method, args);
              });
    }

    private Object invoke(Object target, java.lang.reflect.Method method, Object[] args)
        throws Throwable {
      try {
        return method.invoke(target, args);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }
  }
}
