// 제공자 접수와 안전한 재시도 및 불확실한 발송 결과를 구분한다.

package com.landit.landitbe.feature.notification.client;

/**
 * 이메일 제공자 접수 결과다.
 *
 * @param status ACCEPTED, RETRYABLE, FAILED, UNKNOWN 중 하나
 * @param providerMessageId 접수 성공 시 제공자 메시지 ID
 */
public record EmailSendResult(Status status, String providerMessageId) {
  /** 외부 발송 결과다. UNKNOWN은 중복 방지를 위해 자동 재시도하지 않는다. */
  public enum Status {
    ACCEPTED,
    RETRYABLE,
    FAILED,
    UNKNOWN
  }
}
