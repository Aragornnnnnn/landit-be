// 전용 로컬 PostgreSQL에서 요약 migration과 DB 시각 및 행 잠금을 검증한다.

package com.landit.landitbe.feature.learning.freetalk.context.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.landit.landitbe.feature.learning.freetalk.context.domain.FreeTalkContextSummary;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
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
  void deletingSessionCascadesSummary() {
    new JdbcTemplate(dataSource).update("delete from free_talk_session where id=30");
    assertTrue(repository.findById(30L).isEmpty());
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
