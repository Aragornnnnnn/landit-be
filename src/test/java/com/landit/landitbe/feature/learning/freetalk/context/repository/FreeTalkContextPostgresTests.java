// 전용 로컬 PostgreSQL에서 요약 migration과 DB 시각 및 행 잠금을 검증한다.

package com.landit.landitbe.feature.learning.freetalk.context.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.landit.landitbe.feature.learning.freetalk.context.domain.FreeTalkContextSummary;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(FreeTalkContextPostgresTests.Config.class)
@EnabledIfEnvironmentVariable(named = "LAN531_TEST_POSTGRES", matches = "true")
class FreeTalkContextPostgresTests {
  @Autowired private FreeTalkContextSummaryRepository repository;
  @Autowired private PlatformTransactionManager manager;
  @Autowired private DataSource dataSource;

  @BeforeEach
  void reset() {
    repository.deleteAllInBatch();
    new JdbcTemplate(dataSource)
        .update("insert into free_talk_session(id) values(30) on conflict do nothing");
    repository.saveAndFlush(FreeTalkContextSummary.start(30L, "v1", 6000));
  }

  @Test
  void databaseClockAdvancesWithinTransactionAndLeaseRoundTrips() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              var before = repository.currentTime();
              new JdbcTemplate(dataSource).execute("select pg_sleep(0.15)");
              var after = repository.currentTime();
              assertTrue(Duration.between(before, after).toMillis() >= 100);
              var state = repository.findByIdForUpdate(30L).orElseThrow();
              state.claim("worker", after.plusSeconds(30));
              repository.saveAndFlush(state);
            });
    var state = repository.findById(30L).orElseThrow();
    assertTrue(state.ownsLease("worker", repository.currentTime()));
    assertFalse(state.ownsLease("worker", state.getLeaseUntil()));
  }

  @Test
  void concurrentClaimWaitsAndOldTokenCannotOwnNewLease() throws Exception {
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch secondStarted = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first =
          workers.submit(
              () ->
                  new TransactionTemplate(manager)
                      .executeWithoutResult(
                          status -> {
                            var state = repository.findByIdForUpdate(30L).orElseThrow();
                            state.claim("first", repository.currentTime().plusSeconds(30));
                            locked.countDown();
                            await(release);
                          }));
      try {
        assertTrue(locked.await(5, TimeUnit.SECONDS));
        var second =
            workers.submit(
                () ->
                    new TransactionTemplate(manager)
                        .execute(
                            status -> {
                              secondStarted.countDown();
                              var state = repository.findByIdForUpdate(30L).orElseThrow();
                              return state.ownsLease("first", repository.currentTime());
                            }));
        assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> second.get(150, TimeUnit.MILLISECONDS));
        release.countDown();
        first.get(5, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, second.get(5, TimeUnit.SECONDS));
      } finally {
        release.countDown();
      }
    }
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              var state = repository.findByIdForUpdate(30L).orElseThrow();
              state.claim("second", repository.currentTime().plusSeconds(30));
              assertFalse(state.ownsLease("first", repository.currentTime()));
            });
  }

  @Test
  void deletingSessionCascadesSummary() {
    new JdbcTemplate(dataSource).update("delete from free_talk_session where id=30");
    assertTrue(repository.findById(30L).isEmpty());
  }

  private static void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(5, TimeUnit.SECONDS));
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(error);
    }
  }

  @Configuration
  @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = FreeTalkContextSummaryRepository.class)
  static class Config {
    @Bean
    DataSource dataSource() {
      // 임시 클러스터의 전용 스키마만 사용하며 운영 연결 설정을 읽지 않는다.
      var source =
          new DriverManagerDataSource(
              "jdbc:postgresql://127.0.0.1:55431/postgres?currentSchema=lan531", "landit_test", "");
      var jdbc = new JdbcTemplate(source);
      jdbc.execute("create schema if not exists lan531");
      jdbc.execute("drop table if exists free_talk_context_summary");
      jdbc.execute("create table if not exists free_talk_session(id bigint primary key)");
      new ResourceDatabasePopulator(
              new ClassPathResource("db/migration/V112__add_free_talk_context_summary.sql"))
          .execute(source);
      return source;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      var factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(source);
      factory.setPackagesToScan(FreeTalkContextSummary.class.getPackageName());
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
      return factory;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }
  }
}
