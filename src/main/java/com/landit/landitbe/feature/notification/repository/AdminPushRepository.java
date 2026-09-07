// 관리자 캠페인과 확정 대상의 원자적 저장 및 작업 선점을 담당한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.dto.AdminPushAudiencePreview;
import com.landit.landitbe.feature.notification.dto.AdminPushCampaignRequest;
import com.landit.landitbe.feature.notification.dto.AdminPushRunView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 관리자 캠페인 Service가 소유하는 SQL 저장소다. */
@Repository
@RequiredArgsConstructor
public class AdminPushRepository {
  private final JdbcTemplate jdbc;

  /** 짧은 멱등성·공용 제출 선점을 직렬화한다. */
  public void lockGate() {
    jdbc.queryForObject("select id from admin_push_gate where id=1 for update", Integer.class);
  }

  /**
   * 생성 키로 기존 캠페인을 조회한다.
   *
   * @param adminId 관리자 ID
   * @param key 요청 키
   * @return 기존 캠페인 목록
   */
  public List<Campaign> byKey(long adminId, String key) {
    return jdbc.query(
        """
        select * from admin_push_campaign where created_by=? and create_request_key=?
        """,
        this::mapCampaign,
        adminId,
        key);
  }

  /**
   * 캠페인을 조회한다.
   *
   * @param id 캠페인 ID
   * @return 해당 캠페인 목록
   */
  public List<Campaign> campaign(UUID id) {
    return jdbc.query("select * from admin_push_campaign where id=?", this::mapCampaign, id);
  }

  /**
   * 캠페인 목록을 최신 순으로 조회한다.
   *
   * @param page 페이지
   * @param size 페이지 크기
   * @return 캠페인 목록
   */
  public List<Campaign> list(int page, int size) {
    return jdbc.query(
        """
        select * from admin_push_campaign order by created_at desc , id desc limit ?
        offset ?
        """,
        this::mapCampaign,
        size,
        (long) page * size);
  }

  /**
   * 불변 캠페인을 저장한다.
   *
   * @param row 저장할 캠페인
   * @param key 생성 요청 키
   */
  public void insertCampaign(Campaign row, String key) {
    jdbc.update(
        """
        insert into admin_push_campaign ( id , title , body , deep_link , created_by ,
        create_request_key , request_hash , created_at ) values ( ? , ? , ? , ? , ? ,
        ? , ? , ? )
        """,
        row.id(),
        row.content().title(),
        row.content().body(),
        row.content().deepLink(),
        row.adminId(),
        key,
        row.hash(),
        row.createdAt());
  }

  /**
   * 예상 대상 수를 조회한다.
   *
   * @return 조회 시점의 사용자·토큰 수
   */
  public AdminPushAudiencePreview preview() {
    return jdbc.queryForObject(
        """
        select count ( distinct t.user_profile_id ) users , count ( * ) tokens from
        user_push_token t join user_profile p on p.id=t.user_profile_id where
        t.status= 'ACTIVE' and p.status= 'ACTIVE'
        """,
        (r, n) ->
            new AdminPushAudiencePreview(
                r.getLong("users"), r.getLong("tokens"), LocalDateTime.now()));
  }

  /**
   * 캠페인의 실행을 조회한다.
   *
   * @param campaignId 캠페인 ID
   * @return 생성 순 실행 목록
   */
  public List<Run> runs(UUID campaignId) {
    return jdbc.query(
        "select * from admin_push_run where campaign_id=? order by created_at,id",
        this::run,
        campaignId);
  }

  /**
   * 실행을 잠그고 조회한다.
   *
   * @param id 실행 ID
   * @return 실행 목록
   */
  public List<Run> lockRun(UUID id) {
    return jdbc.query("select * from admin_push_run where id=? for update", this::run, id);
  }

  /**
   * 최근 테스트의 존재를 검사한다.
   *
   * @param adminId 요청 관리자
   * @return 10초 내 테스트가 있으면 true
   */
  public boolean recentTest(long adminId) {
    return jdbc.queryForObject(
            """
            select count ( * ) from admin_push_run where requested_by=? and mode= 'TEST'
            and created_at>?
            """,
            Long.class,
            adminId,
            LocalDateTime.now().minusSeconds(10))
        > 0;
  }

  /**
   * DB에 실행 및 최초 발행 작업을 저장한다.
   *
   * @param id 실행 ID
   * @param campaignId 캠페인 ID
   * @param request 요청 정보
   */
  public void insertRun(UUID id, UUID campaignId, RunRequest request) {
    LocalDateTime now = LocalDateTime.now();
    jdbc.update(
        """
        insert into admin_push_run ( id , campaign_id , broadcast_campaign_id , mode ,
        requested_by , request_key , status , next_attempt_at , created_at ,
        updated_at ) values ( ? , ? , ? , ? , ? , ? , 'QUEUED' , ? , ? , ? )
        """,
        id,
        campaignId,
        request.test() ? null : campaignId,
        request.test() ? "TEST" : "BROADCAST",
        request.adminId(),
        request.key(),
        now,
        now,
        now);
  }

  /** 늦은 Ticket 저장으로 결과가 보정된 실행의 Receipt 확인을 복구한다. */
  public void reopenReceipts() {
    jdbc.update(
        """
        update admin_push_run set status= 'AWAITING_RECEIPTS' , completed_at=null ,
        next_attempt_at=? , published_at=null , work_version=work_version+1 ,
        updated_at=? where status= 'COMPLETED' and exists ( select 1 from
        admin_push_target t join push_delivery d on d.admin_push_target_id=t.id where
        t.run_id=admin_push_run.id and d.status= 'TICKET_ACCEPTED' )
        """,
        LocalDateTime.now(),
        LocalDateTime.now());
  }

  /**
   * 만기 실행을 발행 후보로 조회한다.
   *
   * @return 실행 ID와 버전 목록
   */
  public List<Run> due() {
    LocalDateTime now = LocalDateTime.now();
    return jdbc.query(
        """
        select * from admin_push_run where status not in ( 'COMPLETED' , 'BLOCKED' )
        and next_attempt_at<=? and ( lease_until is null or lease_until<=? ) and (
        published_at is null or published_at<=? ) order by next_attempt_at , id limit
        20
        """,
        this::run,
        now,
        now,
        now.minusMinutes(5));
  }

  /**
   * 발행 시도를 기록해 여러 발행기의 중복을 제한한다.
   *
   * @param row 실행
   * @return 선점 여부
   */
  public boolean claimPublication(Run row) {
    LocalDateTime now = LocalDateTime.now();
    return jdbc.update(
            """
            update admin_push_run set published_at=? , lease_owner=null , lease_until=null
            where id=? and work_version=? and status not in ( 'COMPLETED' , 'BLOCKED' )
            and ( lease_until is null or lease_until<=? ) and ( published_at is null or
            published_at<=? )
            """,
            now,
            row.id(),
            row.version(),
            now,
            now.minusMinutes(5))
        == 1;
  }

  /**
   * 소비자 lease를 저장한다.
   *
   * @param row 실행
   * @param owner 선점 식별자
   */
  public void claim(Run row, UUID owner) {
    jdbc.update(
        """
        update admin_push_run set lease_owner=? , lease_until=? , updated_at=? ,
        status=case when audience_captured_at is null then 'PREPARING' else status end
        where id=?
        """,
        owner,
        LocalDateTime.now().plusMinutes(5),
        LocalDateTime.now(),
        row.id());
  }

  /**
   * 전체 또는 관리자 대상 목록을 한 SQL 스냅샷으로 확정한다.
   *
   * @param row 실행
   */
  public void capture(Run row) {
    LocalDateTime now = LocalDateTime.now();
    jdbc.update(
        """
        insert into admin_push_target ( run_id , user_push_token_id , user_profile_id
        , expo_push_token_snapshot , created_at ) select ? , t.id , t.user_profile_id
        , t.expo_push_token , ? from user_push_token t join user_profile p on
        p.id=t.user_profile_id where t.status= 'ACTIVE' and p.status= 'ACTIVE' and (
        ?= 'BROADCAST' or t.user_profile_id=? )
        """,
        row.id(),
        now,
        row.mode(),
        row.adminId());
    Long users =
        jdbc.queryForObject(
            """
            select count ( distinct user_profile_id ) from admin_push_target where
            run_id=?
            """,
            Long.class,
            row.id());
    Long tokens =
        jdbc.queryForObject(
            "select count(*) from admin_push_target where run_id=?", Long.class, row.id());
    jdbc.update(
        """
        update admin_push_run set audience_captured_at=? , target_user_count=? ,
        target_token_count=? , status= 'SENDING' , last_error_code=? where id=?
        """,
        now,
        users,
        tokens,
        "TEST".equals(row.mode()) && tokens == 0 ? "NO_ACTIVE_PUSH_TOKEN" : null,
        row.id());
  }

  /**
   * 미선점 또는 명시적 거부 후 만기가 된 대상 페이지를 읽는다.
   *
   * @param runId 실행 ID
   * @return 최대 100개 대상
   */
  public List<Target> targets(UUID runId) {
    List<Target> result =
        new java.util.ArrayList<>(
            jdbc.query(
                """
                select t.* from admin_push_target t left join push_delivery d on
                d.admin_push_target_id=t.id where t.run_id=? and t.id> ( select last_target_id
                from admin_push_run where id=? ) and t.excluded_reason is null and d.id is
                null order by t.id limit 100
                """,
                this::target,
                runId,
                runId));
    if (result.size() < 100) {
      result.addAll(
          jdbc.query(
              """
              select t.* from admin_push_target t join push_delivery d on
              d.admin_push_target_id=t.id where t.run_id=? and t.excluded_reason is null and
              d.status= 'REQUESTED' and d.error_code= 'ADMIN_RATE_LIMITED' and
              d.admin_next_attempt_at<=? order by t.id limit ?
              """,
              this::target,
              runId,
              LocalDateTime.now(),
              100 - result.size()));
    }
    result.sort(java.util.Comparator.comparingLong(Target::tokenId));
    return result;
  }

  /**
   * 선점과 같은 트랜잭션으로 최초 제출 커서를 전진시킨다.
   *
   * @param runId 실행 ID
   * @param targetId 처리한 최대 대상 ID
   */
  public void advanceCursor(UUID runId, long targetId) {
    jdbc.update(
        """
        update admin_push_run set last_target_id=greatest ( last_target_id , ? ) where
        id=?
        """,
        targetId,
        runId);
  }

  private Target target(ResultSet r, int row) throws SQLException {
    return new Target(
        r.getLong("id"),
        r.getLong("user_push_token_id"),
        r.getLong("user_profile_id"),
        r.getString("expo_push_token_snapshot"));
  }

  /**
   * 사용자 상태를 확인하고 토큰을 잠가 확정 스냅샷과 비교한다.
   *
   * @param target 확정 대상
   * @return 제외 사유 또는 null
   */
  public String exclusion(Target target) {
    List<String> profile =
        jdbc.query(
            "select status from user_profile where id=?",
            (r, n) -> r.getString(1),
            target.userId());
    if (profile.isEmpty() || !"ACTIVE".equals(profile.getFirst())) {
      return "USER_INACTIVE";
    }
    return jdbc.queryForObject(
        """
        select status , user_profile_id , expo_push_token from user_push_token where
        id=? for update
        """,
        (r, n) -> {
          if (!"ACTIVE".equals(r.getString("status"))) {
            return "TOKEN_INACTIVE";
          }
          if (r.getLong("user_profile_id") != target.userId()) {
            return "TOKEN_OWNER_CHANGED";
          }
          return target.token().equals(r.getString("expo_push_token")) ? null : "TOKEN_CHANGED";
        },
        target.tokenId());
  }

  /**
   * 선점 전 제외를 저장한다.
   *
   * @param targetId 대상 ID
   * @param reason 사유
   */
  public void exclude(long targetId, String reason) {
    jdbc.update("update admin_push_target set excluded_reason=? where id=?", reason, targetId);
  }

  /**
   * 제출 게이트를 선점한다.
   *
   * @param owner 소비자 식별자
   * @return 제출 가능 여부
   */
  public boolean acquireSubmission(UUID owner) {
    LocalDateTime now = LocalDateTime.now();
    return jdbc.update(
            """
            update admin_push_gate set lease_owner=? , lease_until=? ,
            next_submission_at=? where id=1 and ( lease_until is null or lease_until<=? )
            and next_submission_at<=?
            """,
            owner,
            now.plusMinutes(5),
            now.plusSeconds(1),
            now,
            now)
        == 1;
  }

  /**
   * 공용 선점의 유효성을 확인한다.
   *
   * @param owner 소비자 식별자
   * @return 현재 유효한 소유권 여부
   */
  public boolean ownsSubmission(UUID owner) {
    return jdbc.queryForObject(
            """
            select count ( * ) from admin_push_gate where id=1 and lease_owner=? and
            lease_until>?
            """,
            Long.class,
            owner,
            LocalDateTime.now())
        == 1;
  }

  /**
   * 자신이 보유한 제출 게이트를 해제한다.
   *
   * @param owner 소비자 식별자
   */
  public void releaseSubmission(UUID owner) {
    jdbc.update(
        """
        update admin_push_gate set lease_owner=null , lease_until=null ,
        next_submission_at=? where id=1 and lease_owner=?
        """,
        LocalDateTime.now().plusSeconds(1),
        owner);
  }

  /**
   * 만기가 된 Receipt 하나를 조회한다.
   *
   * @param runId 실행 ID
   * @return 발송 이력 ID 목록
   */
  public List<Long> receipt(UUID runId) {
    return jdbc.query(
        """
        select d.id from push_delivery d join admin_push_target t on
        t.id=d.admin_push_target_id where t.run_id=? and d.status= 'TICKET_ACCEPTED'
        and d.admin_receipt_next_at<=? and ( d.admin_receipt_lease_until is null or
        d.admin_receipt_lease_until<=? ) order by d.admin_receipt_next_at , d.id limit
        100
        """,
        (r, n) -> r.getLong(1),
        runId,
        LocalDateTime.now(),
        LocalDateTime.now());
  }

  /**
   * 오래된 결과 불명 선점 이력을 찾는다.
   *
   * @param runId 실행 ID
   * @return 정리할 이력 ID
   */
  public List<Long> stale(UUID runId) {
    return jdbc.query(
        """
        select d.id from push_delivery d join admin_push_target t on
        t.id=d.admin_push_target_id where t.run_id=? and d.status= 'REQUESTED' and
        d.admin_next_attempt_at is null and d.requested_at<=?
        """,
        (r, n) -> r.getLong(1),
        runId,
        LocalDateTime.now().minusMinutes(5));
  }

  /**
   * 대상별 이력을 하나의 집계 스냅샷으로 읽는다.
   *
   * @param row 실행 메타데이터
   * @return 실행 상태와 집계
   */
  public AdminPushRunView view(Run row) {
    java.util.Map<String, Long> reasons = new java.util.TreeMap<>();
    jdbc.query(
        """
        select coalesce(t.excluded_reason,d.error_code) reason,count(*) total
        from admin_push_target t left join push_delivery d on d.admin_push_target_id=t.id
        where t.run_id=? and coalesce(t.excluded_reason,d.error_code) is not null
        group by coalesce(t.excluded_reason,d.error_code)
        """,
        (org.springframework.jdbc.core.RowCallbackHandler)
            r -> reasons.put(r.getString("reason"), r.getLong("total")),
        row.id());
    return jdbc.queryForObject(
        """
        select count ( * ) total , coalesce ( sum ( case when t.excluded_reason is not
        null then 1 else 0 end ) , 0 ) excluded , coalesce ( sum ( case when
        t.excluded_reason is null and d.status= 'DELIVERED' then 1 else 0 end ) , 0 )
        succeeded , coalesce ( sum ( case when t.excluded_reason is null and d.status=
        'FAILED' then 1 else 0 end ) , 0 ) failed , coalesce ( sum ( case when
        t.excluded_reason is null and d.status= 'UNKNOWN' then 1 else 0 end ) , 0 )
        unknown_count , coalesce ( sum ( case when d.expo_ticket_id is not null then 1
        else 0 end ) , 0 ) accepted from admin_push_target t left join push_delivery d
        on d.admin_push_target_id=t.id where t.run_id=?
        """,
        (r, n) -> {
          long excluded = r.getLong("excluded");
          long success = r.getLong("succeeded");
          long failed = r.getLong("failed");
          long unknown = r.getLong("unknown_count");
          return new AdminPushRunView(
              row.id(),
              row.campaignId(),
              row.mode(),
              row.status(),
              row.capturedAt(),
              row.userCount(),
              row.tokenCount(),
              r.getLong("total") - excluded - success - failed - unknown,
              success,
              failed,
              excluded,
              unknown,
              r.getLong("accepted"),
              java.util.Map.copyOf(reasons),
              row.error(),
              row.createdAt(),
              row.updatedAt());
        },
        row.id());
  }

  /**
   * 다음 작업까지의 대기 시각을 계산한다.
   *
   * @param runId 실행 ID
   * @return 가장 이른 대기 시각
   */
  public LocalDateTime nextDue(UUID runId) {
    return jdbc.queryForObject(
        """
        select sum(case when d.id is null then 1 else 0 end) fresh,
               min(case when d.status='REQUESTED' and d.admin_next_attempt_at is null
                        then d.requested_at end) requested,
               min(d.admin_next_attempt_at) retry_at,
               min(case when d.admin_receipt_lease_until>d.admin_receipt_next_at
                        then d.admin_receipt_lease_until else d.admin_receipt_next_at end) receipt_at
        from admin_push_target t left join push_delivery d on d.admin_push_target_id=t.id
        where t.run_id=? and t.excluded_reason is null
          and (d.id is null or d.status in ('REQUESTED','TICKET_ACCEPTED'))
        """,
        (r, n) -> {
          if (r.getLong("fresh") > 0) {
            return LocalDateTime.now();
          }
          LocalDateTime requested = time(r, "requested");
          return java.util.stream.Stream.of(
                  requested == null ? null : requested.plusMinutes(5),
                  time(r, "retry_at"),
                  time(r, "receipt_at"))
              .filter(java.util.Objects::nonNull)
              .min(LocalDateTime::compareTo)
              .orElse(null);
        },
        runId);
  }

  /**
   * 남아 있는 제출 작업 수를 조회한다.
   *
   * @param runId 실행 ID
   * @return 미제출 또는 제출 결과 대기 건수
   */
  public long unsent(UUID runId) {
    return jdbc.queryForObject(
        """
        select count ( * ) from admin_push_target t left join push_delivery d on
        d.admin_push_target_id=t.id where t.run_id=? and t.excluded_reason is null and
        ( d.id is null or d.status= 'REQUESTED' )
        """,
        Long.class,
        runId);
  }

  /**
   * 소유한 작업의 진행을 커밋하고 다음 메시지 발행을 준비한다.
   *
   * @param row 실행
   * @param owner 소유자
   * @param state 다음 상태
   * @param next 다음 실행 시각
   */
  public void finish(Run row, UUID owner, String state, LocalDateTime next) {
    jdbc.update(
        """
        update admin_push_run set status=? , next_attempt_at=? , lease_owner=null ,
        lease_until=null , published_at=null , work_version=work_version+1 ,
        failure_count=0 , completed_at=? , updated_at=? where id=? and lease_owner=?
        and work_version=?
        """,
        state,
        next,
        "COMPLETED".equals(state) ? LocalDateTime.now() : null,
        LocalDateTime.now(),
        row.id(),
        owner,
        row.version());
  }

  /**
   * 반복 작업 장애를 기록하고 일정 횟수 이후 중단한다.
   *
   * @param row 실행
   * @param owner 소유자. 발행 실패는 null
   */
  public void failure(Run row, UUID owner) {
    int count = row.failures() + 1;
    String condition = owner == null ? " and lease_owner is null" : " and lease_owner=?";
    Object[] values =
        owner == null
            ? new Object[] {
              count,
              count >= 8 ? "BLOCKED" : row.status(),
              LocalDateTime.now().plusSeconds(Math.min(300, 5L << Math.min(count - 1, 6))),
              LocalDateTime.now(),
              row.id(),
              row.version()
            }
            : new Object[] {
              count,
              count >= 8 ? "BLOCKED" : row.status(),
              LocalDateTime.now().plusSeconds(Math.min(300, 5L << Math.min(count - 1, 6))),
              LocalDateTime.now(),
              row.id(),
              row.version(),
              owner
            };
    jdbc.update(
        """
        update admin_push_run set failure_count=? , status=? , next_attempt_at=? ,
        last_error_code= 'WORK_INTERRUPTED' , published_at=null , lease_owner=null ,
        lease_until=null , updated_at=? where id=? and work_version=?
        """
            + condition,
        values);
  }

  /**
   * 중단된 실행의 미처리 작업을 재개한다.
   *
   * @param row 실행
   */
  public void resume(Run row) {
    jdbc.update(
        """
        update admin_push_run set status=? , failure_count=0 , next_attempt_at=? ,
        published_at=null , lease_owner=null , lease_until=null ,
        work_version=work_version+1 , updated_at=? where id=?
        """,
        row.capturedAt() == null ? "QUEUED" : "SENDING",
        LocalDateTime.now(),
        LocalDateTime.now(),
        row.id());
  }

  private Campaign mapCampaign(ResultSet r, int number) throws SQLException {
    return new Campaign(
        r.getObject("id", UUID.class),
        new AdminPushCampaignRequest(
            r.getString("title"), r.getString("body"), r.getString("deep_link")),
        r.getLong("created_by"),
        r.getString("request_hash"),
        time(r, "created_at"));
  }

  private Run run(ResultSet r, int number) throws SQLException {
    return new Run(
        r.getObject("id", UUID.class),
        r.getObject("campaign_id", UUID.class),
        r.getString("mode"),
        r.getLong("requested_by"),
        r.getString("request_key"),
        r.getString("status"),
        r.getLong("work_version"),
        r.getObject("lease_owner", UUID.class),
        time(r, "lease_until"),
        time(r, "audience_captured_at"),
        r.getObject("target_user_count", Long.class),
        r.getObject("target_token_count", Long.class),
        r.getInt("failure_count"),
        r.getString("last_error_code"),
        time(r, "created_at"),
        time(r, "updated_at"),
        time(r, "next_attempt_at"));
  }

  private LocalDateTime time(ResultSet r, String name) throws SQLException {
    return r.getTimestamp(name) == null ? null : r.getTimestamp(name).toLocalDateTime();
  }

  private LocalDateTime time(ResultSet r, int index) throws SQLException {
    return r.getTimestamp(index) == null ? null : r.getTimestamp(index).toLocalDateTime();
  }

  /**
   * 불변 캠페인 저장값이다.
   *
   * @param id 캠페인 ID
   * @param content 표시 내용
   * @param adminId 생성자
   * @param hash 입력 해시
   * @param createdAt 생성 시각
   */
  public record Campaign(
      UUID id,
      AdminPushCampaignRequest content,
      long adminId,
      String hash,
      LocalDateTime createdAt) {}

  /**
   * 실행 요청값이다.
   *
   * @param adminId 요청자
   * @param key 요청 키
   * @param test 테스트 여부
   */
  public record RunRequest(long adminId, String key, boolean test) {}

  /**
   * 확정된 토큰 스냅샷이다.
   *
   * @param id 대상 ID
   * @param tokenId 토큰 행 ID
   * @param userId 소유자 ID
   * @param token Expo 토큰
   */
  public record Target(long id, long tokenId, long userId, String token) {}

  /**
   * 작업 선점과 조회에 사용하는 실행 메타데이터다.
   *
   * @param id 실행 ID
   * @param campaignId 캠페인 ID
   * @param mode 실행 유형
   * @param adminId 요청자
   * @param key 요청 키
   * @param status 진행 상태
   * @param version 작업 버전
   * @param owner 선점자
   * @param leaseUntil 선점 만료
   * @param capturedAt 대상 확정 시각
   * @param userCount 확정 사용자 수
   * @param tokenCount 확정 토큰 수
   * @param failures 연속 실패 수
   * @param error 마지막 오류
   * @param createdAt 생성 시각
   * @param updatedAt 변경 시각
   * @param nextAt 다음 작업 시각
   */
  public record Run(
      UUID id,
      UUID campaignId,
      String mode,
      long adminId,
      String key,
      String status,
      long version,
      UUID owner,
      LocalDateTime leaseUntil,
      LocalDateTime capturedAt,
      Long userCount,
      Long tokenCount,
      int failures,
      String error,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime nextAt) {}
}
