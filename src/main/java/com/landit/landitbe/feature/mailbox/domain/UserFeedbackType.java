// 사용자가 편지함에 보내는 피드백 유형을 정의한다.

package com.landit.landitbe.feature.mailbox.domain;

/** 사용자가 편지함에 보내는 피드백 유형이다. */
public enum UserFeedbackType {
  BUG_REPORT("버그 제보"),
  FEATURE_REQUEST("기능 제안"),
  QUESTION("문의"),
  CHEER("응원");

  private final String displayTitle;

  UserFeedbackType(String displayTitle) {
    this.displayTitle = displayTitle;
  }

  /**
   * 클라이언트에 표시할 피드백 유형 제목을 반환한다.
   *
   * @return 피드백 유형 표시 제목
   */
  public String getDisplayTitle() {
    return displayTitle;
  }
}
