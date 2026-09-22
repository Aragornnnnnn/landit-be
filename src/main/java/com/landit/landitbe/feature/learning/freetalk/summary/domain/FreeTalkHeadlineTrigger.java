// 스몰톡 요약 헤드라인 말풍선이 어떤 사실을 골라 말했는지 정의한다.

package com.landit.landitbe.feature.learning.freetalk.summary.domain;

/**
 * 헤드라인 말풍선의 계기다. 기획 1-1의 우선순위대로 위에서부터 처음 해당되는 하나를 고른다. 반응 횟수 계기와 배운 표현 계기는 쓰지 않는다(비교 카드에 없는 지표는 말하지
 * 않고, 표현 작업은 기다리지 않는다).
 */
public enum FreeTalkHeadlineTrigger {
  /** 예외: 사용자의 첫 스몰톡. */
  FIRST_SESSION,
  /** 예외: 직전 스몰톡에서 10일 이상 지나 돌아옴. */
  RETURN_AFTER_BREAK,
  /** 1. 지난번에 틀렸던 패턴을 오늘 맞게 씀. */
  GROWTH,
  /** 2. 말한 시간이 늘었다. */
  SPEAKING_TIME_UP,
  /** 4. 한 번에 더 길게 말했다. */
  LONGEST_TURN_UP,
  /** 5. 더 많이 주고받았다. */
  TURN_COUNT_UP,
  /** 7. 지난번과 비슷하다. */
  SIMILAR,
  /** 8. 지난번보다 줄었다. */
  DECREASED
}
