// 플랫폼별 활성 앱 버전 정책을 조회한다.

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

/** 플랫폼별 활성 앱 버전 정책을 조회한다. */
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

  /**
   * 관리자 목록 화면에 표시할 앱 버전 정책을 플랫폼·빌드 순으로 조회한다.
   *
   * @return 플랫폼 오름차순과 빌드 번호 내림차순으로 정렬된 정책 목록
   */
  List<AppVersion> findAllByOrderByPlatformAscBuildNumberDesc();

  /**
   * 같은 플랫폼·빌드 번호 정책의 존재 여부를 조회한다.
   *
   * @param platform 앱 플랫폼
   * @param buildNumber 앱 빌드 번호
   * @return 같은 플랫폼과 빌드 번호의 정책이 있으면 {@code true}
   */
  boolean existsByPlatformAndBuildNumber(AppPlatform platform, long buildNumber);

  /**
   * 플랫폼에서 활성화된 앱 버전을 조회한다.
   *
   * @param platform 앱 플랫폼
   * @return 활성 앱 버전
   */
  Optional<AppVersion> findByPlatformAndActiveTrue(AppPlatform platform);

  /**
   * 앱 버전 정책 ID로 플랫폼만 조회한다.
   *
   * @param appVersionId 앱 버전 정책 ID
   * @return 정책이 존재하면 해당 앱 플랫폼
   */
  @Query("select version.platform from AppVersion version where version.id = :appVersionId")
  Optional<AppPlatform> findPlatformById(@Param("appVersionId") Long appVersionId);

  /**
   * 같은 플랫폼의 모든 정책을 쓰기 잠금으로 조회한다.
   *
   * @param platform 앱 플랫폼
   * @return 식별자 순서로 잠긴 앱 버전 정책 목록
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select version from AppVersion version "
          + "where version.platform = :platform order by version.id")
  List<AppVersion> findAllByPlatformForUpdate(@Param("platform") AppPlatform platform);
}
