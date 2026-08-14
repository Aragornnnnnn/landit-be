// 전역 편지의 사용자별 읽음 상태를 저장하는 Entity다.

package com.landit.landitbe.feature.mailbox.domain;

import com.landit.landitbe.shared.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

/** 전역 편지의 사용자별 읽음 상태를 저장하는 Entity다. */
@Getter
@Entity
@Table(name = "mailbox_letter_read")
public class MailboxLetterRead extends BaseCreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "letter_id", nullable = false)
  private Long letterId;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "read_at", nullable = false)
  private LocalDateTime readAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected MailboxLetterRead() {}
}
