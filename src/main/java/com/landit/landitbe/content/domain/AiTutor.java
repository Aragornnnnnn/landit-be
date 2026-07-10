// AI 튜터의 공통 발화와 음성 설정을 저장한다.
package com.landit.landitbe.content.domain;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.landit.landitbe.common.domain.Locale;

@Entity
@Table(name = "ai_tutor")
public class AiTutor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accent_locale", nullable = false, length = 35)
    private String accentLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_locale", nullable = false, length = 35)
    private Locale targetLocale;

    @Column(name = "voice_provider", length = 30)
    private String voiceProvider;

    @Column(name = "voice_id", length = 100)
    private String voiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    protected AiTutor() {
    }
}
