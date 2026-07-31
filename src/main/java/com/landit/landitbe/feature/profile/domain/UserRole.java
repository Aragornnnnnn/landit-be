// 사용자 프로필의 서비스 권한 역할을 정의한다.

package com.landit.landitbe.feature.profile.domain;

/** 사용자 프로필의 서비스 권한 역할을 정의한다. */
public enum UserRole {
  /** 일반 사용자 역할이다. */
  USER,

  /** 관리자 API에 접근할 수 있는 역할이다. */
  ADMIN
}
