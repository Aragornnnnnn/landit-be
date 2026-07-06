// 프리톡 세션에만 필요한 정보를 저장한다.
package com.landit.landitbe.session.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "free_talk_session")
public class FreeTalkSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "learning_session_id", nullable = false)
    private Long learningSessionId;

    @Column(length = 255)
    private String title;

    protected FreeTalkSession() {
    }
}
