// 앱 버전별 업데이트 정책을 저장한다.
package com.landit.landitbe.app.domain;

import com.landit.landitbe.common.domain.AppPlatform;
import com.landit.landitbe.common.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_version")
public class AppVersion extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppPlatform platform;

    @Column(name = "version_name", nullable = false, length = 30)
    private String versionName;

    @Column(name = "build_number", nullable = false)
    private long buildNumber;

    @Column(name = "minimum_supported_build_number", nullable = false)
    private long minimumSupportedBuildNumber;

    @Column(name = "force_update_reason", length = 500)
    private String forceUpdateReason;

    @Column(name = "soft_update_reason", length = 500)
    private String softUpdateReason;

    @Column(name = "release_note", columnDefinition = "text")
    private String releaseNote;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    protected AppVersion() {
    }
}
