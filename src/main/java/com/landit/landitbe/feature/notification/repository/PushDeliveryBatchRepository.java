// 푸시 발송 이력의 다중 행 INSERT·UPDATE와 페이지 접수 이력 조회를 수행한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.PushDelivery;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
   * 잠금·중복 검사가 끝난 새 이력을 단일 INSERT로 저장한다.
   *
   * @param deliveries 신규 발송 이력, 최대 100건
   * @return 생성 키를 반환 순서가 아닌 중복 방지 키에 연결한 결과
   */
  public Map<String, Long> insertRequested(List<PushDelivery> deliveries) {
    if (deliveries.isEmpty()) {
      return Map.of();
    }
    MapSqlParameterSource params = new MapSqlParameterSource();
    List<String> rows = new ArrayList<>();
    for (int i = 0; i < deliveries.size(); i++) {
      PushDelivery d = deliveries.get(i);
      params.addValue("user" + i, d.getUserProfileId());
      params.addValue("token" + i, d.getUserPushTokenId());
      params.addValue("expo" + i, d.getSentExpoPushToken());
      params.addValue("type" + i, d.getNotificationType().name());
      params.addValue(
          "variant" + i, d.getContentVariant() == null ? null : d.getContentVariant().name());
      params.addValue("key" + i, d.getDeduplicationKey());
      params.addValue("title" + i, d.getTitle());
      params.addValue("body" + i, d.getBody());
      params.addValue("link" + i, d.getDeepLink());
      params.addValue("now" + i, d.getRequestedAt());
      rows.add(
          """
          (:user%1$d, :token%1$d, :expo%1$d, :type%1$d, :variant%1$d, :key%1$d,
           :title%1$d, :body%1$d, :link%1$d, 'REQUESTED', :now%1$d, :now%1$d, :now%1$d)
          """
              .formatted(i));
    }
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        """
        insert into push_delivery (user_profile_id, user_push_token_id, sent_expo_push_token,
          notification_type, content_variant, deduplication_key, title, body, deep_link,
          status, requested_at, created_at, updated_at) values
        """
            + String.join(",", rows),
        params,
        keys,
        new String[] {"id", "deduplication_key"});
    Map<String, Long> ids = new LinkedHashMap<>();
    keys.getKeyList()
        .forEach(
            row ->
                ids.put(
                    (String) row.get("deduplication_key"), ((Number) row.get("id")).longValue()));
    if (ids.size() != deliveries.size()) {
      throw new IllegalStateException("발송 이력 생성 키 개수가 요청과 다릅니다.");
    }
    return ids;
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
