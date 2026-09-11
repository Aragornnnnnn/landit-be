// 서버 재시작 없이 적용할 공개 범위와 시각을 저장한다.

package com.landit.landitbe.feature.subscription.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

/** 페이월 표시와 서버의 새 학습 제한이 공유하는 정책이다. */
@Entity
@Getter
@Table(name = "subscription_launch_policy")
public class SubscriptionLaunchPolicy {
  @Id private Long id = 1L;
  @Version private Long version;

  @Enumerated(EnumType.STRING)
  private Mode mode;

  private LocalDateTime effectiveAt;
  private boolean newStartsPaused;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "subscription_review_user", joinColumns = @JoinColumn(name = "policy_id"))
  @Column(name = "user_id")
  private Set<Long> reviewUserIds = new HashSet<>();

  /** JPA용 생성자다. */
  protected SubscriptionLaunchPolicy() {}

  /** 검증된 공개 정책으로 단일 정책 행을 생성한다. */
  public SubscriptionLaunchPolicy(
      Mode mode, LocalDateTime effectiveAt, boolean newStartsPaused, Set<Long> reviewUserIds) {
    update(mode, effectiveAt, newStartsPaused, reviewUserIds);
  }

  /** 버전을 보존하면서 정책의 공개 범위와 시각을 갱신한다. */
  public void update(
      Mode mode, LocalDateTime effectiveAt, boolean newStartsPaused, Set<Long> reviewUserIds) {
    this.mode = mode;
    this.effectiveAt = effectiveAt;
    this.newStartsPaused = newStartsPaused;
    this.reviewUserIds.clear();
    this.reviewUserIds.addAll(reviewUserIds);
  }

  /** 정책의 공개 범위다. */
  public enum Mode {
    OFF,
    REVIEW,
    ALL
  }
}
