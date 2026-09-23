// 다음 대화에서 이어 물을 후속 질문을 만들 때 세션이 전달하는 문맥을 담는다.

package com.landit.landitbe.feature.memory.dto;

import java.util.List;

/**
 * 후속 질문 생성에만 쓰는 문맥이다. 기억 후보 추출에는 쓰지 않는다.
 *
 * @param askedMemoryIds 이미 후속 질문의 근거로 쓴 장기기억 ID. 같은 기억으로 질문을 되풀이하지 않게 뺀다
 * @param sessionEndedBy 세션이 어떻게 끝났는지(예: USER_CONFIRMED, TIME_LIMIT_REACHED). 모르면 null
 */
public record ConversationMemoryFollowUpContext(List<Long> askedMemoryIds, String sessionEndedBy) {

  /** ID 목록을 null 없이 불변으로 보관한다. */
  public ConversationMemoryFollowUpContext {
    askedMemoryIds = askedMemoryIds == null ? List.of() : List.copyOf(askedMemoryIds);
  }

  /**
   * 후속 질문 문맥이 없는 요청에 쓴다.
   *
   * @return 뺄 기억도 종료 방식도 없는 문맥
   */
  public static ConversationMemoryFollowUpContext none() {
    return new ConversationMemoryFollowUpContext(List.of(), null);
  }
}
