// 관리자 쓰기 작업의 감사 로그 유형을 정의한다.

package com.landit.landitbe.feature.admin.domain;

/** 관리자 쓰기 작업의 감사 로그 유형을 정의한다. */
public enum AdminAction {
  APP_VERSION_CREATED,
  APP_VERSION_UPDATED,
  APP_VERSION_ACTIVATED
}
