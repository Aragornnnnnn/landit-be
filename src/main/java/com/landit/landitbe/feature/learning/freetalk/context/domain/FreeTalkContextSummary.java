// 프리톡 세션의 파생 컨텍스트 요약 상태를 저장한다.

package com.landit.landitbe.feature.learning.freetalk.context.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 원문 이력과 별도로 세션 요약의 revision과 선점 상태를 관리한다. */
@Getter
@Entity
@Table(name = "free_talk_context_summary")
public class FreeTalkContextSummary extends BaseTimeEntity {

  @Id
  @Column(name = "free_talk_session_id")
  private Long freeTalkSessionId;

  @Column(name = "policy_version", nullable = false, length = 20)
  private String policyVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "summary_content", columnDefinition = "jsonb")
  private JsonNode summaryContent;

  @Column(name = "covered_through_sequence", nullable = false)
  private int coveredThroughSequence;

  @Column(nullable = false)
  private int revision;

  @Column(name = "lease_token", length = 36)
  private String leaseToken;

  @Column(name = "lease_until")
  private Instant leaseUntil;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "source_byte_limit", nullable = false)
  private int sourceByteLimit;

  @Column(name = "suspended_reason", length = 40)
  private String suspendedReason;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkContextSummary() {}

  /** 요약 상태의 초기 컬럼을 설정한다. */
  private FreeTalkContextSummary(
      Long freeTalkSessionId, String policyVersion, int sourceByteLimit) {
    this.freeTalkSessionId = freeTalkSessionId;
    this.policyVersion = policyVersion;
    this.sourceByteLimit = sourceByteLimit;
  }

  /** 새 세션의 빈 요약 상태를 만든다. */
  public static FreeTalkContextSummary start(
      Long freeTalkSessionId, String policyVersion, int sourceByteLimit) {
    return new FreeTalkContextSummary(freeTalkSessionId, policyVersion, sourceByteLimit);
  }

  /** 외부 AI 호출을 선점한다. */
  public void claim(String token, Instant until) {
    leaseToken = token;
    leaseUntil = until;
  }

  /** 요약 결과를 자신의 선점 범위에서만 확정한다. */
  public void complete(JsonNode content, int coveredThroughSequence, int defaultByteLimit) {
    sourceByteLimit = defaultByteLimit;
    summaryContent = content;
    this.coveredThroughSequence = coveredThroughSequence;
    revision++;
    leaseToken = null;
    leaseUntil = null;
    nextAttemptAt = null;
    suspendedReason = null;
  }

  /** 실패한 작업의 다음 실행 시각을 기록하고 선점을 해제한다. */
  public void defer(Instant nextAttemptAt) {
    this.nextAttemptAt = nextAttemptAt;
    leaseToken = null;
    leaseUntil = null;
  }

  /**
   * AI 입력 초과 후 다음 시도의 원문 구간 크기를 줄인다.
   *
   * @param byteLimit 다음 시도의 UTF-8 구간 한도
   * @param nextAttempt 다음 시도 가능 시각
   */
  public void reduceSourceLimit(int byteLimit, Instant nextAttempt) {
    sourceByteLimit = byteLimit;
    defer(nextAttempt);
  }

  /** 최소 요약 단위도 처리할 수 없음을 기록한다. */
  public void suspend(String reason) {
    suspendedReason = reason;
    leaseToken = null;
    leaseUntil = null;
  }

  /**
   * 현재 작업의 선점이 만료되지 않았는지 확인한다.
   *
   * @param token 작업의 선점 식별자
   * @param now DB 기준 현재 시각
   * @return 같은 토큰이며 만료 전이면 true
   */
  public boolean ownsLease(String token, Instant now) {
    return leaseToken != null
        && leaseToken.equals(token)
        && leaseUntil != null
        && leaseUntil.isAfter(now);
  }
}
