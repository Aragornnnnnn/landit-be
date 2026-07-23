// 플랫폼별 활성 앱 버전 정책을 조회한다.

package com.landit.landitbe.feature.app.repository;

import com.landit.landitbe.feature.app.domain.AppVersion;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 플랫폼별 활성 앱 버전 정책을 조회한다. */
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

  /**
   * 플랫폼에서 활성화된 앱 버전을 조회한다.
   *
   * @param platform 앱 플랫폼
   * @return 활성 앱 버전
   */
  Optional<AppVersion> findByPlatformAndActiveTrue(AppPlatform platform);
}
