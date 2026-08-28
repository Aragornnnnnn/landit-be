// 프리톡 장기기억 조회가 발생한 세션 시작 단계를 정의한다.

package com.landit.landitbe.feature.memory.service;

/** 세션당 한 번 허용되는 장기기억 조회 단계를 정의한다. */
public enum MemoryRetrievalStage {
  OPENING,
  FIRST_USER_TURN
}
