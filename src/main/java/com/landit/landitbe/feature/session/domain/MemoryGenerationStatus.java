// 프리톡 장기기억 생성 작업의 상태를 정의한다.

package com.landit.landitbe.feature.session.domain;

/** 프리톡 장기기억 생성 작업의 상태를 정의한다. */
public enum MemoryGenerationStatus {
  /** 장기기억 생성 작업이 준비되어 아직 실행되지 않았다. */
  PREPARING,

  /** 장기기억 생성 작업이 완료됐다. */
  READY,

  /** 장기기억 생성 작업에 실패했다. */
  FAILED
}
