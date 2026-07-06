// 사용자가 완료한 Writing 표현 기록을 저장한다.
package com.landit.landitbe.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_writing_expression_completion")
public class UserWritingExpressionCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    @Column(name = "writing_expression_id", nullable = false)
    private Long writingExpressionId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    protected UserWritingExpressionCompletion() {
    }
}
