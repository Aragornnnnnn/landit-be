// 푸시 발송 이력의 다중 행 INSERT·UPDATE와 페이지 접수 이력 조회를 수행한다.

package com.landit.landitbe.feature.notification.delivery.repository;

import com.landit.landitbe.feature.notification.delivery.domain.PushDelivery;
import jakarta.persistence.EntityManager;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** PushDeliveryService의 트랜잭션 안에서 제한된 묶음의 실제 SQL 요청 수를 줄인다. */
@Repository
@RequiredArgsConstructor
public class PushDeliveryBatchRepository {

  private final NamedParameterJdbcTemplate jdbc;
  private final EntityManager entityManager;

  /**
   * 페이지의 이벤트 접두어와 일치하는 접수 이력을 한 SQL로 조회한다.
   *
   * @param prefixes 정확히 일치시킬 이벤트 접두어 목록
   * @return 현재 Token 상태와 무관한 접수 이력 ID 목록
   */
  public List<Long> findAcceptedIds(List<String> prefixes) {
    if (prefixes.isEmpty()) {
      return List.of();
    }
    MapSqlParameterSource params = new MapSqlParameterSource();
    List<String> predicates = new ArrayList<>();
    for (int i = 0; i < prefixes.size(); i++) {
      String literalPrefix =
          prefixes.get(i).replace("!", "!!").replace("%", "!%").replace("_", "!_");
      params.addValue("prefix" + i, literalPrefix + "%");
      predicates.add("deduplication_key like :prefix" + i + " escape '!'");
    }
    return jdbc.queryForList(
        "select id from push_delivery where status = 'TICKET_ACCEPTED' and ("
            + String.join(" or ", predicates)
            + ") order by id",
        params,
        Long.class);
  }

  /**
   * 잠금과 중복 검사가 끝난 새 이력을 고정 INSERT의 JDBC 배치로 저장한다.
   *
   * @param deliveries 신규 발송 이력, 최대 100건
   * @return 생성 키를 반환 순서가 아닌 중복 방지 키에 연결한 결과
   * @throws IllegalStateException 실행 건수나 생성 키가 입력과 일치하지 않을 때
   */
  public Map<String, Long> insertRequested(List<PushDelivery> deliveries) {
    if (deliveries.isEmpty()) {
      return Map.of();
    }
    SqlParameterSource[] parameters =
        deliveries.stream().map(this::insertParameters).toArray(SqlParameterSource[]::new);
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    int[] counts =
        jdbc.batchUpdate(
            """
        insert into push_delivery (user_profile_id, user_push_token_id, sent_expo_push_token,
          notification_type, content_variant, deduplication_key, title, body, deep_link,
          status, requested_at, created_at, updated_at) values
          (:user, :token, :expo, :type, :variant, :key, :title, :body, :link,
           'REQUESTED', :now, :now, :now)
        """,
            parameters,
            keys,
            new String[] {"id", "deduplication_key"});
    requireSingleRowResults(counts, deliveries.size());
    return generatedIds(keys, deliveries);
  }

  private SqlParameterSource insertParameters(PushDelivery delivery) {
    return new MapSqlParameterSource("user", delivery.getUserProfileId())
        .addValue("token", delivery.getUserPushTokenId())
        .addValue("expo", delivery.getSentExpoPushToken())
        .addValue("type", delivery.getNotificationType().name())
        .addValue(
            "variant",
            delivery.getContentVariant() == null ? null : delivery.getContentVariant().name(),
            Types.VARCHAR)
        .addValue("key", delivery.getDeduplicationKey())
        .addValue("title", delivery.getTitle())
        .addValue("body", delivery.getBody())
        .addValue("link", delivery.getDeepLink())
        .addValue("now", delivery.getRequestedAt(), Types.TIMESTAMP);
  }

  private Map<String, Long> generatedIds(GeneratedKeyHolder keys, List<PushDelivery> deliveries) {
    Map<String, Long> ids = new LinkedHashMap<>();
    for (Map<String, Object> row : keys.getKeyList()) {
      if (!(row.get("deduplication_key") instanceof String key)
          || !(row.get("id") instanceof Number id)
          || ids.putIfAbsent(key, id.longValue()) != null) {
        throw new IllegalStateException("발송 이력 생성 키가 없거나 중복됩니다.");
      }
    }
    if (ids.size() != deliveries.size()
        || !ids.keySet()
            .equals(
                deliveries.stream()
                    .map(PushDelivery::getDeduplicationKey)
                    .collect(Collectors.toSet()))
        || new HashSet<>(ids.values()).size() != ids.size()) {
      throw new IllegalStateException("발송 이력 생성 키가 요청과 다릅니다.");
    }
    return ids;
  }

  private void requireSingleRowResults(int[] counts, int expected) {
    // SUCCESS_NO_INFO도 정확히 한 행을 저장했는지 알 수 없으므로 성공으로 추정하지 않는다.
    if (counts.length != expected || Arrays.stream(counts).anyMatch(count -> count != 1)) {
      throw new IllegalStateException("발송 이력 배치 실행 건수가 요청과 다릅니다.");
    }
  }

  /**
   * 잠근 엔티티의 도메인 전이 결과를 한 UPDATE로 저장한다.
   *
   * @param deliveries 상태가 변경된 잠긴 이력
   */
  public void updateStates(List<PushDelivery> deliveries) {
    if (deliveries.isEmpty()) {
      return;
    }
    MapSqlParameterSource params = new MapSqlParameterSource("now", LocalDateTime.now());
    for (int i = 0; i < deliveries.size(); i++) {
      PushDelivery d = deliveries.get(i);
      params.addValue("id" + i, d.getId());
      params.addValue("status" + i, d.getStatus().name());
      params.addValue("ticket" + i, d.getExpoTicketId());
      params.addValue("error" + i, d.getErrorCode());
      params.addValue("checked" + i, d.getReceiptCheckedAt());
      // JDBC가 상태를 저장하므로 JPA의 건별 dirty-check UPDATE를 실행하지 않는다.
      entityManager.detach(d);
    }
    params.addValue("ids", deliveries.stream().map(PushDelivery::getId).toList());
    String assignments =
        Map.of(
                "status",
                "status",
                "expo_ticket_id",
                "ticket",
                "error_code",
                "error",
                "receipt_checked_at",
                "checked")
            .entrySet()
            .stream()
            .map(entry -> assignment(entry.getKey(), entry.getValue(), deliveries.size()))
            .collect(Collectors.joining(", "));
    int updated =
        jdbc.update(
            "update push_delivery set " + assignments + ", updated_at = :now where id in (:ids)",
            params);
    if (updated != deliveries.size()) {
      throw new IllegalStateException("발송 이력 갱신 개수가 요청과 다릅니다.");
    }
  }

  private String assignment(String column, String parameter, int size) {
    StringBuilder expression = new StringBuilder(column + " = case id");
    for (int i = 0; i < size; i++) {
      expression.append(" when :id").append(i).append(" then :").append(parameter).append(i);
    }
    return expression.append(" else ").append(column).append(" end").toString();
  }
}
