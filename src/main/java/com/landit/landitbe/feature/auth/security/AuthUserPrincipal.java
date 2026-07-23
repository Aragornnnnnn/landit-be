// 인증된 사용자 ID를 Spring Security principal로 표현한다.

package com.landit.landitbe.feature.auth.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** 인증된 사용자 ID를 Spring Security principal로 표현한다. */
public record AuthUserPrincipal(Long userId) implements UserDetails {

  /** 동작을 수행한다. */
  public AuthUserPrincipal {
    if (userId == null) {
      throw new IllegalArgumentException("userId must not be null");
    }
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return userId.toString();
  }
}
