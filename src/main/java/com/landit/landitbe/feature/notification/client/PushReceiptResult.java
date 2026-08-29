// 외부 Push 제공자의 배달 Receipt 조회 결과를 정의한다.

package com.landit.landitbe.feature.notification.client;

/**
 * 외부 Push 제공자의 배달 Receipt 조회 결과를 정의한다.
 *
 * @param status 배달 Receipt 상태
 * @param errorCode 배달 실패 오류 코드
 */
public record PushReceiptResult(PushReceiptStatus status, String errorCode) {

  /** 아직 Receipt가 생성되지 않은 결과를 반환한다. */
  public static PushReceiptResult notReady() {
    return new PushReceiptResult(PushReceiptStatus.NOT_READY, null);
  }

  /** 외부 Push 제공자가 배달을 접수한 결과를 반환한다. */
  public static PushReceiptResult delivered() {
    return new PushReceiptResult(PushReceiptStatus.DELIVERED, null);
  }

  /**
   * 외부 Push 제공자가 배달에 실패한 결과를 반환한다.
   *
   * @param errorCode 배달 실패 오류 코드
   * @return 배달 실패 결과
   */
  public static PushReceiptResult failed(String errorCode) {
    return new PushReceiptResult(PushReceiptStatus.FAILED, errorCode);
  }
}
