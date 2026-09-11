// 메시지 평가의 재시도 입력과 완성 결과를 영속 보관한다.

package com.landit.landitbe.feature.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

/** AI 호출과 별개 트랜잭션으로 보관하는 메시지 평가 작업이다. */
@Getter
@Entity
@Table(name = "message_feedback_work")
public class MessageFeedbackWork {
  @Id private Long messageId;

  @Column(nullable = false)
  private Long sessionId;

  @Column(nullable = false, columnDefinition = "text")
  private String requestPayload;

  @Column(columnDefinition = "text")
  private String resultPayload;

  private boolean legacyCompleted;
  private boolean terminalFailed;
  private String attemptToken;
  private int attempts;

  @Column(nullable = false)
  private LocalDateTime availableAt;

  private LocalDateTime leaseUntil;

  /** JPA용 생성자다. */
  protected MessageFeedbackWork() {}

  /** 저장된 메시지와 같은 트랜잭션에서 복구 가능한 작업을 예약한다. */
  public MessageFeedbackWork(
      Long messageId, Long sessionId, String requestPayload, LocalDateTime availableAt) {
    this.messageId = messageId;
    this.sessionId = sessionId;
    this.requestPayload = requestPayload;
    this.availableAt = availableAt;
  }
}
