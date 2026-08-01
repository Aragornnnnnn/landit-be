// 관리자 쓰기 작업의 감사 로그를 민감정보 없이 저장한다.

package com.landit.landitbe.feature.admin.service;

import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.domain.AdminAuditLog;
import com.landit.landitbe.feature.admin.repository.AdminAuditLogRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 쓰기 작업의 감사 로그를 민감정보 없이 저장한다. */
@Service
public class AdminAuditService {

  private final AdminAuditLogRepository adminAuditLogRepository;

  /**
   * 관리자 감사 로그 저장 Repository를 주입받는다.
   *
   * @param adminAuditLogRepository 관리자 감사 로그 Repository
   */
  public AdminAuditService(AdminAuditLogRepository adminAuditLogRepository) {
    this.adminAuditLogRepository = adminAuditLogRepository;
  }

  /**
   * 관리자 쓰기 작업의 대상과 변경 전후 값을 감사 로그에 저장한다.
   *
   * @param adminUserProfileId 작업을 수행한 관리자 사용자 프로필 ID
   * @param action 수행한 관리자 작업 유형
   * @param targetType 변경 대상 유형
   * @param targetId 변경 대상 식별자
   * @param beforeValue 변경 전 값
   * @param afterValue 변경 후 값
   * @throws IllegalArgumentException 필수 값이 없거나 민감정보가 포함됐을 때
   */
  @Transactional
  public void record(
      Long adminUserProfileId,
      AdminAction action,
      String targetType,
      String targetId,
      String beforeValue,
      String afterValue) {
    requirePresent(adminUserProfileId, "adminUserProfileId");
    if (action == null) {
      throw new IllegalArgumentException("action must not be null");
    }
    requireText(targetType, "targetType");
    requireText(targetId, "targetId");
    rejectSensitiveValue(targetType);
    rejectSensitiveValue(targetId);
    rejectSensitiveValue(beforeValue);
    rejectSensitiveValue(afterValue);
    adminAuditLogRepository.save(
        AdminAuditLog.recorded(
            adminUserProfileId, action, targetType, targetId, beforeValue, afterValue));
  }

  // 필수 ID 값이 존재하는지 검증한다.
  private void requirePresent(Long value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " must not be null");
    }
  }

  // 필수 문자열 값이 비어 있지 않은지 검증한다.
  private void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }

  // 감사 로그 값에 인증 관련 민감정보가 포함됐는지 검사한다.
  private void rejectSensitiveValue(String value) {
    if (value == null) {
      return;
    }
    String normalizedValue = value.toLowerCase(Locale.ROOT);
    if (normalizedValue.contains("authorization")
        || normalizedValue.contains("bearer ")
        || normalizedValue.contains("accesstoken")
        || normalizedValue.contains("access_token")
        || normalizedValue.contains("refreshtoken")
        || normalizedValue.contains("refresh_token")) {
      throw new IllegalArgumentException("감사 로그에 민감정보를 저장할 수 없습니다.");
    }
  }
}
