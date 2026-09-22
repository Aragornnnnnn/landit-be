// 일괄 작업에 사용할 활성 사용자 ID를 불변 값으로 전달한다.

package com.landit.landitbe.feature.profile.dto;

import java.util.List;

/**
 * 잠금으로 확인한 활성 사용자 ID 목록이다.
 *
 * @param ids 활성 사용자 ID 목록
 */
public record ActiveUserProfileIds(List<Long> ids) {

  /**
   * 호출자가 원본 목록을 변경해도 조회 결과를 보존한다.
   *
   * @param ids 활성 사용자 ID 목록
   */
  public ActiveUserProfileIds {
    ids = List.copyOf(ids);
  }
}
