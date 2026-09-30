// JDBC 배치의 생성 키 매핑, 실행 결과와 NULL 파라미터 계약을 검증한다.

package com.landit.landitbe.feature.notification.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.delivery.domain.PushDelivery;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import jakarta.persistence.EntityManager;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

class PushDeliveryBatchRepositoryTest {
  private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
  private final EntityManager entityManager = mock(EntityManager.class);
  private final PushDeliveryBatchRepository repository =
      new PushDeliveryBatchRepository(jdbc, entityManager);

  @Test
  void mapsReversedGeneratedKeysAndBindsNullableReviewVariant() {
    when(jdbc.batchUpdate(
            anyString(),
            any(SqlParameterSource[].class),
            any(KeyHolder.class),
            eq(new String[] {"id", "deduplication_key"})))
        .thenAnswer(
            call -> {
              SqlParameterSource[] parameters = call.getArgument(1);
              assertThat(parameters).hasSize(2);
              assertThat(parameters[0].getValue("key")).isEqualTo("first");
              assertThat(parameters[0].getValue("type")).isEqualTo("EXPRESSION_REVIEW");
              assertThat(parameters[0].getValue("variant")).isNull();
              assertThat(parameters[0].getSqlType("variant")).isEqualTo(Types.VARCHAR);
              KeyHolder holder = call.getArgument(2);
              holder.getKeyList().addAll(List.of(key("second", 22L), key("first", 11L)));
              return new int[] {1, 1};
            });
    assertThat(repository.insertRequested(List.of(delivery("first"), delivery("second"))))
        .containsExactlyInAnyOrderEntriesOf(Map.of("first", 11L, "second", 22L));
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing", "duplicate-key", "unexpected-key", "duplicate-id"})
  void rejectsInvalidGeneratedKeys(String scenario) {
    List<Map<String, Object>> keys =
        switch (scenario) {
          case "missing" -> List.of(key("first", 11L));
          case "duplicate-key" -> List.of(key("first", 11L), key("first", 22L));
          case "unexpected-key" -> List.of(key("first", 11L), key("other", 22L));
          default -> List.of(key("first", 11L), key("second", 11L));
        };
    stubInsert(new int[] {1, 1}, keys);
    assertThatThrownBy(
            () -> repository.insertRequested(List.of(delivery("first"), delivery("second"))))
        .isInstanceOf(IllegalStateException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2, Statement.EXECUTE_FAILED, Statement.SUCCESS_NO_INFO})
  void rejectsUnconfirmedInsertCounts(int count) {
    stubInsert(new int[] {count}, List.of(key("first", 11L)));
    assertThatThrownBy(() -> repository.insertRequested(List.of(delivery("first"))))
        .isInstanceOf(IllegalStateException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2, Statement.EXECUTE_FAILED, Statement.SUCCESS_NO_INFO})
  void rejectsUnconfirmedUpdateCounts(int count) {
    when(jdbc.batchUpdate(anyString(), any(SqlParameterSource[].class)))
        .thenReturn(new int[] {count});
    assertThatThrownBy(() -> repository.updateStates(List.of(delivery("first"))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsTruncatedBatchCounts() {
    stubInsert(new int[0], List.of(key("first", 11L)));
    assertThatThrownBy(() -> repository.insertRequested(List.of(delivery("first"))))
        .isInstanceOf(IllegalStateException.class);
    when(jdbc.batchUpdate(anyString(), any(SqlParameterSource[].class))).thenReturn(new int[0]);
    assertThatThrownBy(() -> repository.updateStates(List.of(delivery("first"))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void detachesBeforeBatchExecutionAndBindsNullableResultTypes() {
    PushDelivery delivery = delivery("first");
    when(jdbc.batchUpdate(anyString(), any(SqlParameterSource[].class)))
        .thenAnswer(
            call -> {
              Mockito.verify(entityManager).detach(delivery);
              SqlParameterSource parameters = call.<SqlParameterSource[]>getArgument(1)[0];
              for (String name : List.of("ticket", "error")) {
                assertThat(parameters.getValue(name)).isNull();
                assertThat(parameters.getSqlType(name)).isEqualTo(Types.VARCHAR);
              }
              assertThat(parameters.getValue("checked")).isNull();
              assertThat(parameters.getSqlType("checked")).isEqualTo(Types.TIMESTAMP);
              return new int[] {1};
            });
    repository.updateStates(List.of(delivery));
  }

  @Test
  void skipsEmptyBatches() {
    assertThat(repository.insertRequested(List.of())).isEmpty();
    repository.updateStates(List.of());
    verifyNoInteractions(jdbc, entityManager);
  }

  private void stubInsert(int[] counts, List<Map<String, Object>> keys) {
    when(jdbc.batchUpdate(
            anyString(),
            any(SqlParameterSource[].class),
            any(KeyHolder.class),
            any(String[].class)))
        .thenAnswer(
            call -> {
              call.<KeyHolder>getArgument(2).getKeyList().addAll(keys);
              return counts;
            });
  }

  private Map<String, Object> key(String key, long id) {
    return Map.of("deduplication_key", key, "id", id);
  }

  private PushDelivery delivery(String key) {
    return PushDelivery.requested(
        1L,
        2L,
        "ExponentPushToken[test]",
        NotificationType.EXPRESSION_REVIEW,
        key,
        "복습",
        "표현 복습",
        "/reviews/test",
        LocalDateTime.of(2026, 9, 23, 17, 0));
  }
}
