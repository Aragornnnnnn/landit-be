// 관리자 감사 로그를 저장한다.

package com.landit.landitbe.feature.admin.repository;

import com.landit.landitbe.feature.admin.domain.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** 관리자 감사 로그를 저장한다. */
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {}
