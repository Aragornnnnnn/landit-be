// 플랫폼별 활성 앱 버전 정책을 조회한다.

package com.landit.landitbe.app.infrastructure;

import com.landit.landitbe.app.domain.AppVersion;
import com.landit.landitbe.common.domain.AppPlatform;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 플랫폼별 활성 앱 버전 정책을 조회한다. */
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

  /** 동작을 수행한다. */
  Optional<AppVersion> findByPlatformAndActiveTrue(AppPlatform platform);
}
