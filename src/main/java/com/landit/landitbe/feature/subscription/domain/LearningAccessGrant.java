// 시작한 학습의 종류와 24시간 완료 권한을 보존한다.

package com.landit.landitbe.feature.subscription.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

/** 구독이 변경돼도 같은 학습만 완료할 수 있는 권한이다. */
@Entity
@Getter
@Table(name = "learning_access_grant")
public class LearningAccessGrant {
  @Id private String id;
  private Long userId;
  private String kind;
  private Long targetId;
  private long policyVersion;
  private String basis;
  private LocalDateTime startedAt;
  private LocalDateTime expiresAt;
  private LocalDateTime completedAt;

  /** JPA용 생성자다. */
  protected LearningAccessGrant() {}

  /** 서버가 허용한 시작에만 새 권한을 발급한다. */
  public LearningAccessGrant(
      long userId,
      String kind,
      long targetId,
      long policyVersion,
      String basis,
      LocalDateTime startedAt) {
    this.id = UUID.randomUUID().toString();
    this.userId = userId;
    this.kind = kind;
    this.targetId = targetId;
    this.policyVersion = policyVersion;
    this.basis = basis;
    this.startedAt = startedAt;
    this.expiresAt = startedAt.plusHours(24);
  }

  /** 완료된 표현은 같은 권한으로 새 연습을 시작할 수 없게 한다. */
  public void complete(LocalDateTime now) {
    if (completedAt == null) {
      completedAt = now;
    }
  }
}
