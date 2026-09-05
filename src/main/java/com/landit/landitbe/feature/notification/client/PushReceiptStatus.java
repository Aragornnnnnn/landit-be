// 외부 Push 제공자의 배달 Receipt 상태를 정의한다.

package com.landit.landitbe.feature.notification.client;

/** 외부 Push 제공자의 배달 Receipt 상태를 정의한다. */
public enum PushReceiptStatus {
  NOT_READY,
  DELIVERED,
  FAILED
}
