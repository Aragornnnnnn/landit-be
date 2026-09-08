// 관리자 푸시 캠페인과 활성 Token 페이지를 저장하고 조회한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.AdminPushAudienceType;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 푸시 캠페인 Service가 소유하는 SQL 저장소다. */
@Repository
@RequiredArgsConstructor
public class AdminPushRepository {

  private static final int BATCH_SIZE = 100;
  private static final int USER_BATCH_SIZE = 1000;
  private static final String SCHEDULE_FROM =
      " from admin_push_campaign where scheduled_at is not null and (?='' or status=?)";

  private static final String AUDIENCE_FROM =
      """
      from user_push_token t join user_profile p on p.id=t.user_profile_id
      join admin_push_campaign c on c.id=?
      where t.status='ACTIVE' and p.status='ACTIVE'
        and (c.audience_type='ALL' or exists (
          select 1 from admin_push_campaign_user selected
          where selected.campaign_id=c.id and selected.user_profile_id=p.id and not selected.excluded))
        and not exists (select 1 from admin_push_campaign_user excluded
          where excluded.campaign_id=c.id and excluded.user_profile_id=p.id and excluded.excluded)
      """;

  private final JdbcTemplate jdbc;

  /**
   * 생성 요청 키로 캠페인을 조회한다.
   *
   * @param adminId 관리자 ID
   * @param key 요청 키
   * @return 일치하는 캠페인
   */
  public List<Campaign> byKey(long adminId, String key) {
    return jdbc.query(
        "select * from admin_push_campaign where created_by=? and create_request_key=?",
        this::map,
        adminId,
        key);
  }

  /**
   * 캠페인을 조회한다.
   *
   * @param id 캠페인 ID
   * @return 일치하는 캠페인
   */
  public List<Campaign> find(UUID id) {
    return jdbc.query("select * from admin_push_campaign where id=?", this::map, id);
  }

  /**
   * 캠페인을 최신순으로 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 캠페인 목록
   */
  public List<Campaign> list(int page, int size) {
    return jdbc.query(
        "select * from admin_push_campaign order by created_at desc,id desc limit ? offset ?",
        this::map,
        size,
        (long) page * size);
  }

  /**
   * 예약 캠페인을 예약 시각과 ID 내림차순으로 조회한다.
   *
   * @param status 상태 필터. 빈 문자열이면 모든 예약 상태
   * @param page 0부터 시작하는 페이지 번호
   * @param size 페이지 크기
   * @return 예약 캠페인 목록
   */
  public List<Campaign> schedules(String status, int page, int size) {
    return jdbc.query(
        "select *" + SCHEDULE_FROM + " order by scheduled_at desc,id desc limit ? offset ?",
        this::map,
        status,
        status,
        size,
        (long) page * size);
  }

  /**
   * 같은 상태 조건의 전체 예약 수를 조회한다.
   *
   * @param status 상태 필터. 빈 문자열이면 모든 예약 상태
   * @return 전체 예약 수
   */
  public long countSchedules(String status) {
    return jdbc.queryForObject("select count(*)" + SCHEDULE_FROM, Long.class, status, status);
  }

  /**
   * 새 캠페인을 저장한다.
   *
   * @param campaign 저장할 캠페인
   * @param key 생성 요청 키
   */
  @Transactional
  public void insert(Campaign campaign, String key) {
    jdbc.update(
        """
        insert into admin_push_campaign (
          id,title,body,deep_link,created_by,create_request_key,request_hash,created_at,updated_at,audience_type,audience_sql
        ) values (?,?,?,?,?,?,?,?,?,?,?)
        """,
        campaign.id(),
        campaign.content().title(),
        campaign.content().body(),
        campaign.content().deepLink(),
        campaign.adminId(),
        key,
        campaign.hash(),
        campaign.createdAt(),
        campaign.createdAt(),
        campaign.content().audienceType().name(),
        campaign.content().audienceSql());
    if (!campaign.content().userProfileIds().isEmpty()) {
      jdbc.batchUpdate(
          "insert into admin_push_campaign_user(campaign_id,user_profile_id) values (?,?)",
          campaign.content().userProfileIds(),
          USER_BATCH_SIZE,
          (statement, userId) -> {
            statement.setObject(1, campaign.id());
            statement.setLong(2, userId);
          });
    }
    jdbc.batchUpdate(
        "insert into admin_push_campaign_user(campaign_id,user_profile_id,excluded) "
            + "values (?,?,true)",
        campaign.content().excludedUserProfileIds(),
        USER_BATCH_SIZE,
        (statement, userId) -> {
          statement.setObject(1, campaign.id());
          statement.setLong(2, userId);
        });
  }

  /**
   * 선택 목록의 모든 사용자 ID가 존재하는지 확인한다.
   *
   * @param userIds 중복을 제거한 사용자 ID 목록
   * @return 모두 존재하거나 목록이 비어 있으면 true
   */
  public boolean usersExist(List<Long> userIds) {
    if (userIds.isEmpty()) {
      return true;
    }
    for (int offset = 0; offset < userIds.size(); offset += USER_BATCH_SIZE) {
      List<Long> batch =
          userIds.subList(offset, Math.min(offset + USER_BATCH_SIZE, userIds.size()));
      String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
      if (jdbc.queryForObject(
              "select count(*) from user_profile where id in (" + placeholders + ")",
              Long.class,
              batch.toArray())
          != batch.size()) {
        return false;
      }
    }
    return true;
  }

  /**
   * 현재 활성 사용자와 Token 수를 반환한다.
   *
   * @param id 대상 조건을 적용할 캠페인 ID
   * @return 예상 대상 수
   */
  public AdminPushAudiencePreview preview(UUID id) {
    return jdbc.queryForObject(
        "select count(distinct t.user_profile_id) users,count(*) tokens " + AUDIENCE_FROM,
        (result, row) ->
            new AdminPushAudiencePreview(
                result.getLong("users"), result.getLong("tokens"), LocalDateTime.now()),
        id);
  }

  /**
   * SQL과 수동 목록을 합친 사용자들의 현재 활성 Token 수를 조회한다.
   *
   * @param userIds 중복 없는 대상 ID
   * @return 예상 발송 수
   */
  public AdminPushAudiencePreview previewUsers(List<Long> userIds) {
    long users = 0;
    long tokens = 0;
    for (int offset = 0; offset < userIds.size(); offset += USER_BATCH_SIZE) {
      List<Long> batch =
          userIds.subList(offset, Math.min(offset + USER_BATCH_SIZE, userIds.size()));
      String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
      AdminPushAudiencePreview count =
          jdbc.queryForObject(
              "select count(distinct p.id) users,count(*) tokens from user_profile p "
                  + "join user_push_token t on t.user_profile_id=p.id "
                  + "where p.status='ACTIVE' and t.status='ACTIVE' and p.id in ("
                  + placeholders
                  + ")",
              (result, row) ->
                  new AdminPushAudiencePreview(
                      result.getLong("users"), result.getLong("tokens"), LocalDateTime.now()),
              batch.toArray());
      users += count.estimatedUserCount();
      tokens += count.estimatedTokenCount();
    }
    return new AdminPushAudiencePreview(users, tokens, LocalDateTime.now());
  }

  /**
   * 최초 전체 발송 요청 시 대상 범위와 수를 고정한다.
   *
   * @param id 캠페인 ID
   * @return 최초 상태 전환 여부
   */
  @Transactional
  public boolean queueAndCaptureTargets(UUID id) {
    return queueAndCaptureTargets(id, null);
  }

  /**
   * 조회된 SQL 사용자 목록 또는 저장된 대상 조건으로 발송 대상을 한 번 고정한다.
   *
   * @param id 캠페인 ID
   * @param resolvedUsers SQL과 수동 선택의 최종 ID. null이면 저장된 조건 사용
   * @return 최초 고정 여부
   */
  @Transactional
  public boolean queueAndCaptureTargets(UUID id, List<Long> resolvedUsers) {
    int updated =
        jdbc.update(
            """
            update admin_push_campaign set
              status='QUEUED',
              updated_at=?
            where id=? and (status in ('DRAFT','PENDING') or
              (status in ('SCHEDULE_PENDING','SCHEDULED') and scheduled_at<=CURRENT_TIMESTAMP))
            """,
            LocalDateTime.now(),
            id);
    if (updated == 0) {
      return false;
    }
    if (resolvedUsers == null) {
      jdbc.update(
          """
          insert into admin_push_target(campaign_id,user_push_token_id,user_profile_id)
          select c.id,t.id,t.user_profile_id
          """
              + AUDIENCE_FROM,
          id);
    } else {
      captureUsers(id, resolvedUsers);
    }
    jdbc.update(
        """
        update admin_push_campaign set
          target_user_count=(select count(distinct user_profile_id) from admin_push_target where campaign_id=?),
          target_token_count=(select count(*) from admin_push_target where campaign_id=?),
          max_target_id=coalesce((select max(id) from admin_push_target where campaign_id=?),0),
          updated_at=? where id=?
        """,
        id,
        id,
        id,
        LocalDateTime.now(),
        id);
    return true;
  }

  private void captureUsers(UUID id, List<Long> userIds) {
    for (int offset = 0; offset < userIds.size(); offset += USER_BATCH_SIZE) {
      List<Long> batch =
          userIds.subList(offset, Math.min(offset + USER_BATCH_SIZE, userIds.size()));
      String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
      var args = new java.util.ArrayList<Object>();
      args.add(id);
      args.addAll(batch);
      jdbc.update(
          "insert into admin_push_target(campaign_id,user_push_token_id,user_profile_id) "
              + "select ?,t.id,p.id from user_push_token t "
              + "join user_profile p on p.id=t.user_profile_id "
              + "where t.status='ACTIVE' and p.status='ACTIVE' and p.id in ("
              + placeholders
              + ")",
          args.toArray());
    }
  }

  /**
   * SQL 대상 준비를 Worker에 넘기기 위해 발송 의도를 고정한다.
   *
   * @param id 캠페인 ID
   * @return 최초 요청 여부
   */
  public boolean requestSend(UUID id) {
    return jdbc.update(
            "update admin_push_campaign set status='PENDING',updated_at=? "
                + "where id=? and status='DRAFT'",
            LocalDateTime.now(),
            id)
        == 1;
  }

  /**
   * 예약 생성 전 재시도 가능한 예약 의도를 저장한다.
   *
   * @param id 캠페인 ID
   * @param time UTC 예약 시각
   * @return 최초 요청 여부
   */
  public boolean requestSchedule(UUID id, Instant time) {
    return jdbc.update(
            "update admin_push_campaign set status='SCHEDULE_PENDING',scheduled_at=?,updated_at=? "
                + "where id=? and status='DRAFT'",
            Timestamp.from(time),
            LocalDateTime.now(),
            id)
        == 1;
  }

  /**
   * AWS 예약 등록 성공을 기록한다. 이미 취소되거나 발송 중이면 변경하지 않는다.
   *
   * @param id 캠페인 ID
   */
  public void scheduled(UUID id) {
    jdbc.update(
        "update admin_push_campaign set status='SCHEDULED',updated_at=? "
            + "where id=? and status='SCHEDULE_PENDING'",
        LocalDateTime.now(),
        id);
  }

  /**
   * 아직 대상을 고정하지 않은 예약을 취소한다.
   *
   * @param id 캠페인 ID
   * @return 최초 취소 여부
   */
  public boolean cancelSchedule(UUID id) {
    return jdbc.update(
            "update admin_push_campaign set status='CANCELLED',updated_at=? "
                + "where id=? and status in ('SCHEDULE_PENDING','SCHEDULED')",
            LocalDateTime.now(),
            id)
        == 1;
  }

  /**
   * 캠페인의 다음 활성 Token 페이지를 조회한다.
   *
   * @param campaign 캠페인 진행 상태
   * @return 최대 100개 Token 대상
   */
  public List<Target> targets(Campaign campaign) {
    return jdbc.query(
        """
        select id,user_push_token_id,user_profile_id
        from admin_push_target
        where campaign_id=? and id>? and id<=?
        order by id limit ?
        """,
        (result, row) ->
            new Target(
                result.getLong("id"),
                result.getLong("user_push_token_id"),
                result.getLong("user_profile_id")),
        campaign.id(),
        campaign.lastTargetId(),
        campaign.maxTargetId(),
        BATCH_SIZE);
  }

  /**
   * 처리한 Token 페이지의 커서를 전진시키고 마지막 페이지면 완료한다.
   *
   * @param id 캠페인 ID
   * @param lastTokenId 처리한 마지막 Token ID
   * @param completed 마지막 페이지 여부
   */
  public void advance(UUID id, long lastTokenId, boolean completed) {
    jdbc.update(
        """
        update admin_push_campaign set status=?,last_target_id=?,completed_at=?,updated_at=?
        where id=? and status in ('QUEUED','SENDING') and last_target_id<=?
        """,
        completed ? "COMPLETED" : "SENDING",
        lastTokenId,
        completed ? LocalDateTime.now() : null,
        LocalDateTime.now(),
        id,
        lastTokenId);
  }

  /**
   * 캠페인과 기존 발송 이력에서 현재 집계를 만든다.
   *
   * @param campaign 캠페인
   * @return 관리자 조회 응답
   */
  public AdminPushCampaignView view(Campaign campaign) {
    Counts counts =
        jdbc.queryForObject(
            """
            select count(*) total,
              (select count(*) from admin_push_target where campaign_id=? and id<=?) processed,
              (select count(*) from push_delivery processed_delivery
                join admin_push_target processed_target
                  on processed_target.user_push_token_id=processed_delivery.user_push_token_id
                where processed_target.campaign_id=? and processed_target.id<=?
                  and processed_delivery.deduplication_key like ?) processed_deliveries,
              coalesce(sum(case when status in ('REQUESTED','TICKET_ACCEPTED') then 1 else 0 end),0) pending,
              coalesce(sum(case when status='DELIVERED' then 1 else 0 end),0) succeeded,
              coalesce(sum(case when status='FAILED' then 1 else 0 end),0) failed
            from push_delivery where deduplication_key like ?
            """,
            (result, row) ->
                new Counts(
                    result.getLong("total"),
                    result.getLong("processed"),
                    result.getLong("processed_deliveries"),
                    result.getLong("pending"),
                    result.getLong("succeeded"),
                    result.getLong("failed")),
            campaign.id(),
            campaign.lastTargetId(),
            campaign.id(),
            campaign.lastTargetId(),
            "push:admin-broadcast:" + campaign.id() + ":%",
            "push:admin-broadcast:" + campaign.id() + ":%");
    long excluded = Math.max(0, counts.processed() - counts.processedDeliveries());
    return new AdminPushCampaignView(
        campaign.id(),
        campaign.content().title(),
        campaign.content().body(),
        campaign.content().deepLink(),
        campaign.adminId(),
        campaign.status(),
        campaign.targetUserCount(),
        campaign.targetTokenCount(),
        counts.pending(),
        counts.succeeded(),
        counts.failed(),
        excluded,
        campaign.createdAt(),
        campaign.completedAt(),
        campaign.content().audienceType(),
        campaign.content().userProfileIds(),
        campaign.content().audienceSql(),
        campaign.content().excludedUserProfileIds(),
        campaign.scheduledAt());
  }

  private Campaign map(ResultSet result, int row) throws SQLException {
    UUID id = result.getObject("id", UUID.class);
    AdminPushAudienceType audience =
        AdminPushAudienceType.valueOf(result.getString("audience_type"));
    List<Long> selectedUsers =
        audience == AdminPushAudienceType.ALL
            ? List.of()
            : jdbc.queryForList(
                "select user_profile_id from admin_push_campaign_user "
                    + "where campaign_id=? and not excluded order by user_profile_id",
                Long.class,
                id);
    return new Campaign(
        id,
        new AdminPushCampaignRequest(
            result.getString("title"),
            result.getString("body"),
            result.getString("deep_link"),
            audience,
            selectedUsers,
            result.getString("audience_sql"),
            jdbc.queryForList(
                "select user_profile_id from admin_push_campaign_user "
                    + "where campaign_id=? and excluded order by user_profile_id",
                Long.class,
                id)),
        result.getLong("created_by"),
        result.getString("request_hash"),
        result.getString("status"),
        result.getLong("target_user_count"),
        result.getLong("target_token_count"),
        result.getLong("max_target_id"),
        result.getLong("last_target_id"),
        result.getTimestamp("created_at").toLocalDateTime(),
        result.getTimestamp("completed_at") == null
            ? null
            : result.getTimestamp("completed_at").toLocalDateTime(),
        result.getTimestamp("scheduled_at") == null
            ? null
            : result.getTimestamp("scheduled_at").toInstant());
  }

  /**
   * 저장된 캠페인과 발송 커서다.
   *
   * @param id 캠페인 ID
   * @param content 알림 내용
   * @param adminId 생성 관리자 ID
   * @param hash 생성 요청 해시
   * @param status 캠페인 상태
   * @param targetUserCount 대상 사용자 수
   * @param targetTokenCount 대상 Token 수
   * @param maxTargetId 대상 스냅샷의 마지막 ID
   * @param lastTargetId 처리한 마지막 대상 ID
   * @param createdAt 생성 시각
   * @param completedAt 제출 완료 시각
   * @param scheduledAt UTC 예약 시각
   */
  public record Campaign(
      UUID id,
      AdminPushCampaignRequest content,
      long adminId,
      String hash,
      String status,
      long targetUserCount,
      long targetTokenCount,
      long maxTargetId,
      long lastTargetId,
      LocalDateTime createdAt,
      LocalDateTime completedAt,
      Instant scheduledAt) {

    /**
     * 예약하지 않은 캠페인을 생성한다.
     *
     * @param id 캠페인 ID
     * @param content 내용
     * @param adminId 관리자 ID
     * @param hash 원문 해시
     * @param status 상태
     * @param targetUserCount 사용자 수
     * @param targetTokenCount Token 수
     * @param maxTargetId 마지막 대상 ID
     * @param lastTargetId 처리한 ID
     * @param createdAt 생성 시각
     * @param completedAt 완료 시각
     */
    public Campaign(
        UUID id,
        AdminPushCampaignRequest content,
        long adminId,
        String hash,
        String status,
        long targetUserCount,
        long targetTokenCount,
        long maxTargetId,
        long lastTargetId,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
      this(
          id,
          content,
          adminId,
          hash,
          status,
          targetUserCount,
          targetTokenCount,
          maxTargetId,
          lastTargetId,
          createdAt,
          completedAt,
          null);
    }
  }

  /**
   * 한 Token 발송 대상이다.
   *
   * @param id 대상 스냅샷 ID
   * @param tokenId Token ID
   * @param userId 사용자 ID
   */
  public record Target(long id, long tokenId, long userId) {}

  private record Counts(
      long total,
      long processed,
      long processedDeliveries,
      long pending,
      long succeeded,
      long failed) {}
}
