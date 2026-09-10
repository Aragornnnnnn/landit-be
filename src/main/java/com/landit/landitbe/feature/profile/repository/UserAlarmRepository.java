// 사용자별 일일 알람 설정을 사용자 프로필 ID로 조회하고 저장한다.

package com.landit.landitbe.feature.profile.repository;

import com.landit.landitbe.feature.profile.domain.UserAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

/** UserAlarmService가 소유하는 사용자별 일일 알람 저장소다. */
public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {}
