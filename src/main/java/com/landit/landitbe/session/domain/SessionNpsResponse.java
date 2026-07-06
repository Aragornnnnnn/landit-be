// 세션 종료 후 사용자 만족도 응답을 저장한다.
package com.landit.landitbe.session.domain;

import com.landit.landitbe.common.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "session_nps_response")
public class SessionNpsResponse extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "learning_session_id", nullable = false)
    private Long learningSessionId;

    @Column(nullable = false)
    private int score;

    @Column(name = "opinion_text", columnDefinition = "text")
    private String opinionText;

    protected SessionNpsResponse() {
    }
}
