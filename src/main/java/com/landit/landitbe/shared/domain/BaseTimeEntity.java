// 엔티티의 생성 시간과 수정 시간을 공통으로 관리한다.

package com.landit.landitbe.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

/** 엔티티의 생성 시간과 수정 시간을 공통으로 관리한다. */
@MappedSuperclass
public abstract class BaseTimeEntity {

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /** 엔티티가 처음 저장될 때 생성 시간과 수정 시간을 채운다. */
  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  /** 엔티티가 수정될 때 수정 시간을 갱신한다. */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  /** 엔티티 생성 시간을 반환한다. */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /** 엔티티 수정 시간을 반환한다. */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
