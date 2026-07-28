// 사용자의 프리톡 공통 표현 학습 완료 시각을 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

/** 사용자의 프리톡 공통 표현 학습 완료 시각을 저장한다. */
@Getter
@Entity
@Table(name = "user_free_talk_expression_completion")
public class UserFreeTalkExpressionCompletion extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "free_talk_expression_id", nullable = false)
  private Long freeTalkExpressionId;

  @Column(name = "completed_at", nullable = false)
  private LocalDateTime completedAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserFreeTalkExpressionCompletion() {}

  /** 사용자의 프리톡 표현 학습 완료 기록을 생성한다. */
  public static UserFreeTalkExpressionCompletion complete(
      Long userProfileId, Long freeTalkExpressionId, LocalDateTime completedAt) {
    UserFreeTalkExpressionCompletion completion = new UserFreeTalkExpressionCompletion();
    completion.userProfileId = userProfileId;
    completion.freeTalkExpressionId = freeTalkExpressionId;
    completion.completedAt = completedAt;
    return completion;
  }
}
