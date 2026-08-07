// 플랫폼별 단일 앱 버전 정책을 조회한다.

package com.landit.landitbe.feature.app.repository;

import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.shared.domain.AppPlatform;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 플랫폼별 단일 앱 버전 정책을 조회한다. */
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

  /**
   * 관리자 목록 화면에 표시할 플랫폼별 앱 버전 정책을 조회한다.
   *
   * @return 플랫폼 오름차순으로 정렬된 정책 목록
   */
  List<AppVersion> findAllByOrderByPlatformAsc();

  /**
   * 플랫폼의 단일 앱 버전 정책을 조회한다.
   *
   * @param platform 앱 플랫폼
   * @return 플랫폼별 앱 버전 정책
   */
  Optional<AppVersion> findByPlatform(AppPlatform platform);

  /**
   * 플랫폼의 단일 앱 버전 정책을 수정용으로 잠금 조회한다.
   *
   * @param platform 앱 플랫폼
   * @return 잠금을 획득한 플랫폼별 앱 버전 정책
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select appVersion from AppVersion appVersion where appVersion.platform = :platform")
  Optional<AppVersion> findByPlatformForUpdate(@Param("platform") AppPlatform platform);
}
