// 프리톡 대화 상대 캐릭터와 음성 매핑을 정의한다.

package com.landit.landitbe.feature.session.domain;

import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Arrays;

/** 프리톡에서 선택할 수 있는 캐릭터다. */
public enum FreeTalkCharacter {
  CHLOE("chloe", "Chloe"),
  MARCO("marco", "Marco"),
  TEDDY("teddy", "Teddy");

  private final String id;
  private final String displayName;

  FreeTalkCharacter(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  /**
   * 공개 API 캐릭터 식별자를 반환한다.
   *
   * @return 소문자 캐릭터 식별자
   */
  public String id() {
    return id;
  }

  /**
   * 제목 fallback에 사용할 캐릭터 표시명을 반환한다.
   *
   * @return 영문 캐릭터 표시명
   */
  public String displayName() {
    return displayName;
  }

  /**
   * 공개 식별자를 지원하는 캐릭터로 변환한다.
   *
   * @param id 공개 API 캐릭터 식별자
   * @return 일치하는 캐릭터
   * @throws ApiException 식별자가 없거나 지원하지 않을 때
   */
  public static FreeTalkCharacter fromId(String id) {
    return Arrays.stream(values())
        .filter(character -> character.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST));
  }
}
