// 관리자 쓰기 작업의 감사 로그 유형을 정의한다.

package com.landit.landitbe.feature.admin.domain;

/** 관리자 쓰기 작업의 감사 로그 유형을 정의한다. */
public enum AdminAction {
  /** 앱 버전 정책 수정 작업이다. */
  APP_VERSION_UPDATED,

  /** 편지함 공지·업데이트 생성 작업이다. */
  MAILBOX_LETTER_CREATED,

  /** 편지함 공지·업데이트 수정 작업이다. */
  MAILBOX_LETTER_UPDATED,

  /** 편지함 답장 발송 작업이다. */
  MAILBOX_REPLY_SENT,

  /** 발음 평가 자산 일괄 임포트 작업이다. */
  EXPRESSION_PRONUNCIATION_ASSET_IMPORTED
}
