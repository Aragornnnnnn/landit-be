// 서비스 사용자 프로필의 생명주기 상태를 정의한다.

package com.landit.landitbe.auth.domain;

/** 서비스 사용자 프로필의 생명주기 상태를 정의한다. */
public enum UserProfileStatus {
  ACTIVE,
  WITHDRAWN,
  BANNED
}
