// 기존 학습 알림과 복습 알림이 공유하는 발송 빈도를 관리한다.

package com.landit.landitbe.feature.notification.scheduled.service;

import com.landit.landitbe.config.learning.ReviewProperties;
import com.landit.landitbe.feature.notification.delivery.dto.SendPushNotificationCommand;
import com.landit.landitbe.feature.notification.scheduled.repository.LearningNotificationSlotRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 고객지원·관리자 알림에는 이 정책을 적용하지 않는다. */
@Service
@RequiredArgsConstructor
public class LearningNotificationFrequencyService {
  private final LearningNotificationSlotRepository repository;
  private final ReviewProperties properties;
  private final Clock clock;

  /**
   * 사용자별 예약을 잠그고 빈도 조건을 만족한 명령만 반환한다.
   *
   * @param commands 기존 학습 또는 복습 알림
   * @return 발송할 명령. 같은 이벤트의 재시도는 기존 슬롯을 재사용한다
   */
  @Transactional
  public List<SendPushNotificationCommand> reserveAll(List<SendPushNotificationCommand> commands) {
    return repository.reserve(
        commands, LocalDateTime.now(clock), properties.notificationGapHours());
  }
}
