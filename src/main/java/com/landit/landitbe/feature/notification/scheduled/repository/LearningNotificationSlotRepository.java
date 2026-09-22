// 학습·복습 알림의 예약을 사용자 잠금으로 직렬화한다.

package com.landit.landitbe.feature.notification.scheduled.repository;

import com.landit.landitbe.feature.notification.delivery.dto.SendPushNotificationCommand;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 여러 기기에 보내는 하나의 사용자 알림을 한 번으로 센다. */
@Repository
@RequiredArgsConstructor
public class LearningNotificationSlotRepository {
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * 하루 종류별 한 건, 두 종류 합계 두 건과 최소 시간 간격을 원자적으로 예약한다.
   *
   * @param commands 사용자별 발송 명령
   * @param now 실제 처리 시각
   * @param gapHours 종류 간 최소 간격
   * @return 새 예약 또는 같은 이벤트 재시도로 허용한 명령
   */
  public List<SendPushNotificationCommand> reserve(
      List<SendPushNotificationCommand> commands, LocalDateTime now, int gapHours) {
    if (commands.isEmpty()) {
      return List.of();
    }
    var ids =
        commands.stream()
            .map(SendPushNotificationCommand::userProfileId)
            .distinct()
            .sorted()
            .toList();
    Map<String, Object> parameters =
        Map.of(
            "ids",
            ids,
            "since",
            now.minusDays(1),
            "events",
            commands.stream().map(SendPushNotificationCommand::eventId).toList());
    // 다른 기능의 Entity를 가져오지 않고, 공통 프로필 잠금 순서를 사용자 ID 오름차순으로 맞춘다.
    List<Long> active =
        jdbc.query(
            "select id from user_profile where id in (:ids) and status = 'ACTIVE'"
                + " order by id for update",
            parameters,
            (rs, row) -> rs.getLong(1));
    List<Slot> slots =
        new ArrayList<>(
            jdbc.query(
                """
                select event_id, user_profile_id, notification_group, reserved_at
                from learning_notification_slot
                where user_profile_id in (:ids) and (reserved_at >= :since or event_id in (:events))
                """,
                parameters,
                (rs, row) ->
                    new Slot(
                        rs.getString(1),
                        rs.getLong(2),
                        rs.getString(3),
                        rs.getObject(4, LocalDateTime.class))));
    Map<Long, LocalDateTime> legacy = new java.util.HashMap<>();
    jdbc.query(
        "select user_profile_id, last_sent_at from user_notification_state"
            + " where user_profile_id in (:ids) and last_sent_at >= :since",
        parameters,
        rs -> {
          legacy.put(rs.getLong(1), rs.getObject(2, LocalDateTime.class));
        });
    List<SendPushNotificationCommand> accepted = new ArrayList<>();
    for (var command : commands) {
      if (active.contains(command.userProfileId())
          && allowed(command, slots, legacy, now, gapHours)) {
        boolean existing =
            slots.stream().anyMatch(slot -> slot.eventId().equals(command.eventId()));
        if (!existing) {
          reserveOne(command, now);
          slots.add(new Slot(command.eventId(), command.userProfileId(), group(command), now));
        }
        accepted.add(command);
      }
    }
    return List.copyOf(accepted);
  }

  private boolean allowed(
      SendPushNotificationCommand command,
      List<Slot> slots,
      Map<Long, LocalDateTime> legacy,
      LocalDateTime now,
      int gapHours) {
    var own = slots.stream().filter(slot -> slot.eventId().equals(command.eventId())).findFirst();
    if (own.isPresent() && !own.get().at().toLocalDate().equals(now.toLocalDate())) {
      return false;
    }
    var userSlots =
        slots.stream().filter(slot -> slot.userId() == command.userProfileId()).toList();
    LocalDateTime lastDaily = legacy.get(command.userProfileId());
    if (group(command).equals("REVIEW")
        && lastDaily != null
        && lastDaily.isAfter(now.minusHours(gapHours))
        // 같은 날 DAILY 슬롯이 있으면 재시도로 갱신되는 상태 대신 최초 예약 시각을 따른다.
        && userSlots.stream()
            .noneMatch(
                slot ->
                    slot.group().equals("DAILY")
                        && slot.at().toLocalDate().equals(lastDaily.toLocalDate()))) {
      return false;
    }
    if (userSlots.stream()
        .anyMatch(
            slot ->
                !slot.eventId().equals(command.eventId())
                    && slot.at().isAfter(now.minusHours(gapHours)))) {
      return false;
    }
    if (own.isPresent()) {
      return true;
    }
    var today =
        userSlots.stream()
            .filter(slot -> slot.at().toLocalDate().equals(now.toLocalDate()))
            .toList();
    return today.size() < 2
        && today.stream().noneMatch(slot -> slot.group().equals(group(command)));
  }

  private void reserveOne(SendPushNotificationCommand command, LocalDateTime now) {
    jdbc.update(
        """
        insert into learning_notification_slot(event_id, user_profile_id, notification_group, reserved_at, reserved_date)
        values (:event, :user, :kind, :now, :date)
        """,
        Map.of(
            "event",
            command.eventId(),
            "user",
            command.userProfileId(),
            "kind",
            group(command),
            "now",
            now,
            "date",
            now.toLocalDate()));
  }

  private String group(SendPushNotificationCommand command) {
    return command.notificationType() == NotificationType.EXPRESSION_REVIEW ? "REVIEW" : "DAILY";
  }

  private record Slot(String eventId, long userId, String group, LocalDateTime at) {}
}
