// 엔티티의 생성 시간만 공통으로 관리한다.
package com.landit.landitbe.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseCreatedAtEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 엔티티가 처음 저장될 때 생성 시간을 채운다. */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** 엔티티 생성 시간을 반환한다. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
