// 사용자별 최근 알림 계산 상태를 조회하고 저장한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.UserNotificationState;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자별 최근 알림 계산 상태를 조회하고 저장한다. */
public interface UserNotificationStateRepository
    extends JpaRepository<UserNotificationState, Long> {

  /**
   * 지정한 사용자들의 기존 알림 상태를 한 번에 조회한다.
   *
   * @param userProfileIds 조회할 사용자 ID 목록
   * @return 사용자별 기존 알림 상태
   */
  List<UserNotificationState> findAllByUserProfileIdIn(Collection<Long> userProfileIds);
}
