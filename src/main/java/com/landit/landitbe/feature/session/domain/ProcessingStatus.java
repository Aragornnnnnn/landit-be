// 비동기 처리 결과 상태를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 비동기 처리 결과 상태를 정의한다. */
public enum ProcessingStatus {
  PREPARING,
  COMPLETED,
  FAILED
}
