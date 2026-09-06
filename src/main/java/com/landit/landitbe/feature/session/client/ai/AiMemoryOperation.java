// 장기기억 후보에 적용할 상태 판정 연산을 정의한다.

package com.landit.landitbe.feature.session.client.ai;

/** 장기기억 후보에 적용할 상태 판정 연산이다. */
public enum AiMemoryOperation {
  ADD,
  SUPERSEDE,
  IGNORE
}
