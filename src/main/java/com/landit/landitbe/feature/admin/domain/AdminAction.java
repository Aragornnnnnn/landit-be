// 관리자 쓰기 작업의 감사 로그 유형을 정의한다.

package com.landit.landitbe.feature.admin.domain;

/** 관리자 쓰기 작업의 감사 로그 유형을 정의한다. */
public enum AdminAction {
  /** 앱 버전 정책 등록 작업이다. */
  APP_VERSION_CREATED,

  /** 앱 버전 정책 수정 작업이다. */
  APP_VERSION_UPDATED,

  /** 플랫폼의 활성 앱 버전 정책 전환 작업이다. */
  APP_VERSION_ACTIVATED
}
