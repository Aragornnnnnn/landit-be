// 관리자 푸시 캠페인과 활성 Token 페이지를 저장하고 조회한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.AdminPushAudienceType;
import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignView;
import java.sql.ResultSet;
import java.sql.SQLException;
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

  private static final String AUDIENCE_FROM =
      """
      from user_push_token t join user_profile p on p.id=t.user_profile_id
      join admin_push_campaign c on c.id=?
      where t.status='ACTIVE' and p.status='ACTIVE'
        and (c.audience_type='ALL' or exists (
          select 1 from admin_push_campaign_user selected
          where selected.campaign_id=c.id and selected.user_profile_id=p.id))
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
          id,title,body,deep_link,created_by,create_request_key,request_hash,created_at,updated_at,audience_type
        ) values (?,?,?,?,?,?,?,?,?,?)
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
        campaign.content().audienceType().name());
    if (!campaign.content().userProfileIds().isEmpty()) {
      jdbc.batchUpdate(
          "insert into admin_push_campaign_user(campaign_id,user_profile_id) values (?,?)",
          campaign.content().userProfileIds().stream()
              .map(userId -> new Object[] {campaign.id(), userId})
              .toList());
    }
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
    String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
    return jdbc.queryForObject(
            "select count(*) from user_profile where id in (" + placeholders + ")",
            Long.class,
            userIds.toArray())
        == userIds.size();
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
   * 최초 전체 발송 요청 시 대상 범위와 수를 고정한다.
   *
   * @param id 캠페인 ID
   * @return 최초 상태 전환 여부
   */
  @Transactional
  public boolean queueAndCaptureTargets(UUID id) {
    int updated =
        jdbc.update(
            """
            update admin_push_campaign set
              status='QUEUED',
              updated_at=?
            where id=? and status='DRAFT'
            """,
            LocalDateTime.now(),
            id);
    if (updated == 0) {
      return false;
    }
    jdbc.update(
        """
        insert into admin_push_target(campaign_id,user_push_token_id,user_profile_id)
        select c.id,t.id,t.user_profile_id
        """
            + AUDIENCE_FROM,
        id);
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
        campaign.content().userProfileIds());
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
                    + "where campaign_id=? order by user_profile_id",
                Long.class,
                id);
    return new Campaign(
        id,
        new AdminPushCampaignRequest(
            result.getString("title"),
            result.getString("body"),
            result.getString("deep_link"),
            audience,
            selectedUsers),
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
            : result.getTimestamp("completed_at").toLocalDateTime());
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
      LocalDateTime completedAt) {}

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
