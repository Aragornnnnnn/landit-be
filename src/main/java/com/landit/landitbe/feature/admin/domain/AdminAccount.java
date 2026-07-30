// 관리자 접근 허용 사용자 프로필을 저장한다.

package com.landit.landitbe.feature.admin.domain;

import com.landit.landitbe.shared.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 관리자 접근 허용 사용자 프로필을 저장한다. */
@Entity
@Table(name = "admin_account")
public class AdminAccount extends BaseCreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false, unique = true)
  private Long userProfileId;

  /** JPA에서 사용하는 기본 생성자다. */
  protected AdminAccount() {}
}
