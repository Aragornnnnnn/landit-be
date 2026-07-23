// 사용자 디바이스의 푸시 토큰을 저장한다.

package com.landit.landitbe.feature.notification.domain;

import com.landit.landitbe.shared.domain.AppPlatform;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 사용자 디바이스의 푸시 토큰을 저장한다. */
@Entity
@Table(name = "user_push_token")
public class UserPushToken extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppPlatform platform;

  @Column(nullable = false, length = 500)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserPushTokenStatus status;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserPushToken() {}
}
