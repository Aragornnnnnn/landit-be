// 사용자별 일일 알람 설정 조회와 저장을 담당하며 기기 예약은 수행하지 않는다.

package com.landit.landitbe.feature.profile.service;

import com.landit.landitbe.feature.profile.domain.UserAlarm;
import com.landit.landitbe.feature.profile.dto.UserAlarmResponse;
import com.landit.landitbe.feature.profile.dto.UserAlarmUpdateRequest;
import com.landit.landitbe.feature.profile.exception.UserProfileException;
import com.landit.landitbe.feature.profile.repository.UserAlarmRepository;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자별 일일 알람 설정 조회와 저장을 담당한다. */
@Service
@RequiredArgsConstructor
public class UserAlarmService {

  private final UserProfileService userProfileService;
  private final UserAlarmRepository userAlarmRepository;

  /**
   * 활성 사용자의 알람 설정을 조회한다.
   *
   * @param userId 인증된 사용자 ID
   * @return 저장된 설정. 미설정이면 시각은 {@code null}, 활성 상태는 {@code false}
   * @throws UserProfileException 활성 사용자가 없을 때
   */
  @Transactional(readOnly = true)
  public UserAlarmResponse getAlarm(Long userId) {
    userProfileService.requireActive(userId);
    return userAlarmRepository
        .findById(userId)
        .map(UserAlarmResponse::from)
        .orElseGet(() -> new UserAlarmResponse(null, false));
  }

  /**
   * 활성 사용자의 일일 알람 설정 전체를 저장하거나 변경한다.
   *
   * @param userId 인증된 사용자 ID
   * @param request Bean Validation을 통과한 알람 설정 변경 요청
   * @return 저장한 알람 설정. 실제 기기 예약 성공 여부는 포함하지 않는다
   * @throws UserProfileException 활성 사용자가 없을 때
   */
  @Transactional
  public UserAlarmResponse updateAlarm(Long userId, UserAlarmUpdateRequest request) {
    // 알람 행이 없는 최초 설정도 직렬화하고 탈퇴와의 경합을 방지한다.
    userProfileService.requireActiveForUpdate(userId);
    LocalTime alarmTime = LocalTime.parse(request.time());
    UserAlarm alarm =
        userAlarmRepository
            .findById(userId)
            .orElseGet(() -> new UserAlarm(userId, alarmTime, request.enabled()));
    alarm.update(alarmTime, request.enabled());
    return UserAlarmResponse.from(userAlarmRepository.save(alarm));
  }
}
