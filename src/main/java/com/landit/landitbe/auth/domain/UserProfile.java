// 서비스 사용자 프로필과 학습 기본 설정을 저장한다.
package com.landit.landitbe.auth.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "user_profile")
public class UserProfile extends BaseTimeEntity {

    // locale 표기는 콘텐츠 시딩 규칙과 동일하게 대문자('EN'/'KR')를 쓴다. (V13 마이그레이션으로 통일)
    private static final String DEFAULT_TARGET_LOCALE = "EN";
    private static final String DEFAULT_BASE_LOCALE = "KR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "target_locale", nullable = false, length = 35)
    private String targetLocale;

    @Column(name = "base_locale", nullable = false, length = 35)
    private String baseLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_level", length = 20)
    private LearningLevel learningLevel;

    @Column(name = "current_level", nullable = false)
    private int currentLevel;

    @Column(name = "ai_tutor_id")
    private Long aiTutorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_permission_status", nullable = false, length = 30)
    private PushPermissionStatus pushPermissionStatus;

    @Column(name = "push_permission_updated_at")
    private LocalDateTime pushPermissionUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserProfileStatus status;

    protected UserProfile() {
    }

    public UserProfile(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
        this.targetLocale = DEFAULT_TARGET_LOCALE;
        this.baseLocale = DEFAULT_BASE_LOCALE;
        this.currentLevel = 1;
        this.pushPermissionStatus = PushPermissionStatus.NOT_DETERMINED;
        this.status = UserProfileStatus.ACTIVE;
    }

    /** 소셜 제공자에서 받은 최신 프로필 정보로 갱신한다. */
    public void updateProfile(String email, String nickname) {
        if (email != null) {
            this.email = email;
        }
        if (nickname != null) {
            this.nickname = nickname;
        }
    }

    /** 사용자 프로필을 탈퇴 상태로 전환하고 프로필 이미지를 정리한다. */
    public void withdraw() {
        this.profileImageUrl = null;
        this.status = UserProfileStatus.WITHDRAWN;
    }

    /** 활성 사용자인지 확인한다. */
    public boolean isActive() {
        return status == UserProfileStatus.ACTIVE;
    }
}
