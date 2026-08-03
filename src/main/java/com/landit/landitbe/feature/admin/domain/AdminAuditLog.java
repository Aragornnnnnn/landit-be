// 관리자 쓰기 작업의 변경 이력을 저장한다.

package com.landit.landitbe.feature.admin.domain;

import com.landit.landitbe.shared.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 관리자 쓰기 작업의 변경 이력을 저장한다. */
@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog extends BaseCreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "admin_user_profile_id", nullable = false)
  private Long adminUserProfileId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private AdminAction action;

  @Column(name = "target_type", nullable = false, length = 100)
  private String targetType;

  @Column(name = "target_id", nullable = false, length = 255)
  private String targetId;

  @Column(name = "before_value", columnDefinition = "text")
  private String beforeValue;

  @Column(name = "after_value", columnDefinition = "text")
  private String afterValue;

  /** JPA에서 사용하는 기본 생성자다. */
  protected AdminAuditLog() {}

  // 검증된 관리자 작업 정보를 감사 로그 엔티티로 초기화한다.
  private AdminAuditLog(
      Long adminUserProfileId,
      AdminAction action,
      String targetType,
      String targetId,
      String beforeValue,
      String afterValue) {
    this.adminUserProfileId = adminUserProfileId;
    this.action = action;
    this.targetType = targetType;
    this.targetId = targetId;
    this.beforeValue = beforeValue;
    this.afterValue = afterValue;
  }

  /**
   * 관리자 쓰기 작업의 감사 로그를 생성한다.
   *
   * @param adminUserProfileId 작업을 수행한 관리자 사용자 프로필 ID
   * @param action 수행한 관리자 작업 유형
   * @param targetType 변경 대상 유형
   * @param targetId 변경 대상 식별자
   * @param beforeValue 변경 전 값
   * @param afterValue 변경 후 값
   * @return 저장 전 감사 로그 엔티티
   */
  public static AdminAuditLog recorded(
      Long adminUserProfileId,
      AdminAction action,
      String targetType,
      String targetId,
      String beforeValue,
      String afterValue) {
    return new AdminAuditLog(
        adminUserProfileId, action, targetType, targetId, beforeValue, afterValue);
  }
}
