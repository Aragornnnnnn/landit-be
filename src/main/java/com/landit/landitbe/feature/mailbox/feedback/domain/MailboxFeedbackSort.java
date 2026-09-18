// 관리자 문의 목록의 정렬 순서를 정의한다.

package com.landit.landitbe.feature.mailbox.feedback.domain;

/** 어드민 피드백 목록 정렬 방향이다. */
public enum MailboxFeedbackSort {
  /** 최신 피드백부터 정렬한다. */
  NEWEST,

  /** 오래된 피드백부터 정렬한다. */
  OLDEST
}
