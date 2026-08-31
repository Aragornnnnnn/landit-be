// 사용자별 마지막 알림 계산 결과 스냅샷 갱신을 검증한다.

package com.landit.landitbe.feature.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 사용자별 마지막 알림 계산 결과 스냅샷 갱신을 검증한다. */
class UserNotificationStateTest {

  /** 새 계산 결과를 저장하고 실제 발송 시각을 별도로 기록한다. */
  @Test
  void recordsCalculatedTargetAndSentTimeSeparately() {
    LocalDateTime activityAt = LocalDateTime.of(2026, 7, 26, 19, 30);
    LocalDateTime sentAt = LocalDateTime.of(2026, 7, 26, 20, 0);

    UserNotificationState state =
        UserNotificationState.ready(1L, NotificationType.DAILY_SCENARIO_REMINDER, 10L, activityAt);
    state.markSent(sentAt);

    assertThat(state.getUserProfileId()).isEqualTo(1L);
    assertThat(state.getNotificationType()).isEqualTo(NotificationType.DAILY_SCENARIO_REMINDER);
    assertThat(state.getTargetId()).isEqualTo(10L);
    assertThat(state.getStatus()).isEqualTo(NotificationStateStatus.SENT);
    assertThat(state.getLastActivityAt()).isEqualTo(activityAt);
    assertThat(state.getLastSentAt()).isEqualTo(sentAt);
  }
}
