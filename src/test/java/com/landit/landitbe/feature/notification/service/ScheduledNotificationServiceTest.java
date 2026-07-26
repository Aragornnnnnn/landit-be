// 예약 학습 알림 배치가 500명 단위의 일괄 조회로 처리되는지 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.messaging.PushQueuePublisher;
import com.landit.landitbe.feature.notification.repository.UserNotificationStateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** 예약 학습 알림 배치가 500명 단위의 일괄 조회로 처리되는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class ScheduledNotificationServiceTest {

  @Mock private NotificationTargetPageQueryService notificationTargetPageQueryService;

  @Mock private NotificationTargetSelectionService notificationTargetSelectionService;

  @Mock private UserNotificationStateRepository userNotificationStateRepository;

  @Mock private PushQueuePublisher pushQueuePublisher;

  @Mock private PlatformTransactionManager transactionManager;

  private ScheduledNotificationService scheduledNotificationService;

  @BeforeEach
  void setUp() {
    lenient()
        .when(transactionManager.getTransaction(any()))
        .thenAnswer(invocation -> new SimpleTransactionStatus());
    scheduledNotificationService =
        new ScheduledNotificationService(
            notificationTargetPageQueryService,
            notificationTargetSelectionService,
            userNotificationStateRepository,
            pushQueuePublisher,
            transactionManager);
  }

  /** 500명 경계에서 다음 Keyset 페이지를 조회하고 사용자별 추가 조회 없이 발송 메시지를 만든다. */
  @Test
  void processesUsersInFiveHundredSizeKeysetPagesWithoutPerUserRepositoryLookups() {
    NotificationTargetPage firstPage = page(1L, 500);
    NotificationTargetPage secondPage = page(501L, 1);
    when(notificationTargetPageQueryService.loadPage(0L, 500)).thenReturn(firstPage);
    when(notificationTargetPageQueryService.loadPage(500L, 500)).thenReturn(secondPage);
    when(notificationTargetPageQueryService.loadPage(501L, 500))
        .thenReturn(new NotificationTargetPage(List.of(), Map.of(), List.of()));
    when(userNotificationStateRepository.findAllByUserProfileIdIn(any())).thenReturn(List.of());
    when(notificationTargetSelectionService.select(any()))
        .thenReturn(
            Optional.of(new SelectedNotificationTarget(NotificationType.CONTINUE_SCENARIO, 11L)));
    AtomicInteger visibilityExtensionCount = new AtomicInteger();

    scheduledNotificationService.process(
        Instant.parse("2026-07-26T11:00:00Z"), visibilityExtensionCount::incrementAndGet);

    assertThat(visibilityExtensionCount).hasValue(5);
    verify(notificationTargetPageQueryService).loadPage(0L, 500);
    verify(notificationTargetPageQueryService).loadPage(500L, 500);
    verify(notificationTargetPageQueryService).loadPage(501L, 500);
    verify(userNotificationStateRepository, times(2)).findAllByUserProfileIdIn(any());
    verify(pushQueuePublisher, times(501)).publishNotification(any());
    verify(userNotificationStateRepository, times(2)).saveAll(any());
    verify(notificationTargetSelectionService, times(501)).select(any());
    verify(notificationTargetPageQueryService, times(3)).loadPage(any(Long.class), eq(500));
  }

  /** 지정한 ID 범위의 사용자를 같은 선정 입력과 발송 가능 상태로 구성한다. */
  private NotificationTargetPage page(long firstUserProfileId, int count) {
    List<Long> userProfileIds =
        LongStream.range(firstUserProfileId, firstUserProfileId + count).boxed().toList();
    Map<Long, NotificationTargetSelectionInput> inputs =
        userProfileIds.stream()
            .collect(Collectors.toMap(userProfileId -> userProfileId, this::input));
    return new NotificationTargetPage(userProfileIds, inputs, userProfileIds);
  }

  /** 사용자별 선정 결과를 만들기 위한 최소 학습 입력을 구성한다. */
  private NotificationTargetSelectionInput input(Long userProfileId) {
    return new NotificationTargetSelectionInput(
        userProfileId, null, null, null, null, List.of(), List.of());
  }
}
