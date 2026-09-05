// 예약 학습 알림 배치가 500명 단위의 일괄 조회로 처리되는지 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.repository.UserNotificationStateRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** 예약 학습 알림 배치가 500명 단위의 일괄 조회로 처리되는지 검증한다. */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ScheduledNotificationServiceTest {

  @Mock private NotificationTargetPageQueryService notificationTargetPageQueryService;

  @Mock private NotificationTargetSelectionService notificationTargetSelectionService;

  @Mock private UserNotificationStateRepository userNotificationStateRepository;

  @Mock private NotificationDispatchService notificationDispatchService;

  @Mock private PlatformTransactionManager transactionManager;

  private ScheduledNotificationService scheduledNotificationService;

  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    lenient()
        .when(transactionManager.getTransaction(any()))
        .thenAnswer(invocation -> new SimpleTransactionStatus());
    lenient()
        .when(notificationDispatchService.sendAll(any()))
        .thenAnswer(
            invocation -> {
              List<SendPushNotificationCommand> commands = invocation.getArgument(0);
              return new NotificationDispatchResult(commands.size(), 1, commands.size(), 0);
            });
    scheduledNotificationService =
        new ScheduledNotificationService(
            notificationTargetPageQueryService,
            notificationTargetSelectionService,
            userNotificationStateRepository,
            notificationDispatchService,
            transactionManager,
            meterRegistry);
  }

  /** 500명 경계에서 다음 Keyset 페이지를 조회하고 사용자별 SQS 재발행 없이 상태를 저장한다. */
  @Test
  void processesUsersInFiveHundredSizeKeysetPagesWithoutPublishingPushSendMessages(
      CapturedOutput output) {
    NotificationTargetPage firstPage = page(1L, 500);
    NotificationTargetPage secondPage = page(501L, 1);
    LocalDate scheduledDate = LocalDate.of(2026, 7, 26);
    when(notificationTargetPageQueryService.loadPage(0L, 500, scheduledDate)).thenReturn(firstPage);
    when(notificationTargetPageQueryService.loadPage(500L, 500, scheduledDate))
        .thenReturn(secondPage);
    when(notificationTargetPageQueryService.loadPage(501L, 500, scheduledDate))
        .thenReturn(new NotificationTargetPage(List.of(), Map.of(), List.of()));
    when(userNotificationStateRepository.findAllByUserProfileIdIn(any())).thenReturn(List.of());
    when(notificationTargetSelectionService.select(any(), any(LocalDate.class)))
        .thenReturn(
            Optional.of(
                new SelectedNotificationTarget(
                    NotificationType.DAILY_SCENARIO_REMINDER, 11L, null)));
    AtomicInteger visibilityExtensionCount = new AtomicInteger();

    scheduledNotificationService.process(
        "scheduled-message-1",
        Instant.parse("2026-07-26T11:00:00Z"),
        visibilityExtensionCount::incrementAndGet);

    assertThat(visibilityExtensionCount).hasValue(5);
    verify(notificationTargetPageQueryService).loadPage(0L, 500, scheduledDate);
    verify(notificationTargetPageQueryService).loadPage(500L, 500, scheduledDate);
    verify(notificationTargetPageQueryService).loadPage(501L, 500, scheduledDate);
    verify(userNotificationStateRepository, times(2)).findAllByUserProfileIdIn(any());
    verify(notificationDispatchService, times(2)).sendAll(any());
    verify(userNotificationStateRepository, times(2)).saveAll(any());
    verify(notificationTargetSelectionService, times(501)).select(any(), eq(scheduledDate));
    verify(notificationTargetPageQueryService, times(3))
        .loadPage(any(Long.class), eq(500), eq(scheduledDate));
    assertThat(meterCount("landit.notification.scheduled.users", "stage", "scanned"))
        .isEqualTo(501.0);
    assertThat(meterCount("landit.notification.scheduled.users", "stage", "sendable"))
        .isEqualTo(501.0);
    assertThat(meterCount("landit.notification.scheduled.users", "stage", "selected"))
        .isEqualTo(501.0);
    assertThat(meterRegistry.find("landit.notification.scheduled.page.duration").timer().count())
        .isEqualTo(2L);
    assertThat(
            meterRegistry
                .find("landit.notification.scheduled.batch.duration")
                .tag("outcome", "success")
                .timer()
                .count())
        .isEqualTo(1L);
    assertThat(output)
        .contains("scheduled_notification_batch_started")
        .contains("messageId=scheduled-message-1")
        .contains("scheduled_notification_page_completed")
        .contains("scheduled_notification_batch_completed")
        .contains("pageCount=2")
        .contains("scannedUsers=501");
  }

  /** 같은 날짜의 재처리에서 선정 유형이 바뀌어도 사용자 일일 이벤트 ID는 유지한다. */
  @Test
  @SuppressWarnings("unchecked")
  void keepsDailyEventIdWhenSelectedNotificationTypeChangesOnRetry() {
    LocalDate scheduledDate = LocalDate.of(2026, 7, 26);
    NotificationTargetPage userPage = page(1L, 1);
    NotificationTargetPage emptyPage = new NotificationTargetPage(List.of(), Map.of(), List.of());
    when(notificationTargetPageQueryService.loadPage(0L, 500, scheduledDate)).thenReturn(userPage);
    when(notificationTargetPageQueryService.loadPage(1L, 500, scheduledDate)).thenReturn(emptyPage);
    when(userNotificationStateRepository.findAllByUserProfileIdIn(any())).thenReturn(List.of());
    when(notificationTargetSelectionService.select(any(), any(LocalDate.class)))
        .thenReturn(
            Optional.of(
                new SelectedNotificationTarget(
                    NotificationType.DAILY_SCENARIO_REMINDER, 11L, null)),
            Optional.of(
                new SelectedNotificationTarget(NotificationType.CONTINUE_EXPRESSION, 101L, 11L)));

    scheduledNotificationService.process(
        "scheduled-message-1", Instant.parse("2026-07-26T11:00:00Z"), () -> {});
    scheduledNotificationService.process(
        "scheduled-message-1", Instant.parse("2026-07-26T11:00:00Z"), () -> {});

    ArgumentCaptor<List<SendPushNotificationCommand>> commandsCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(notificationDispatchService, times(2)).sendAll(commandsCaptor.capture());
    List<String> eventIds = new ArrayList<>();
    commandsCaptor
        .getAllValues()
        .forEach(
            commands ->
                commands.stream().map(SendPushNotificationCommand::eventId).forEach(eventIds::add));
    assertThat(eventIds).containsExactly("scheduled:2026-07-26:1", "scheduled:2026-07-26:1");
  }

  /** 페이지 조회 실패 시 배치 실패 단계와 실행 시도를 남기고 예외를 유지한다. */
  @Test
  void recordsFailedBatchWithoutSwallowingFailure(CapturedOutput output) {
    LocalDate scheduledDate = LocalDate.of(2026, 7, 26);
    IllegalStateException failure = new IllegalStateException("조회 실패");
    when(notificationTargetPageQueryService.loadPage(0L, 500, scheduledDate)).thenThrow(failure);

    assertThatThrownBy(
            () ->
                scheduledNotificationService.process(
                    "scheduled-message-1", Instant.parse("2026-07-26T11:00:00Z"), () -> {}))
        .isSameAs(failure);

    assertThat(
            meterRegistry
                .find("landit.notification.scheduled.batch.duration")
                .tag("outcome", "failure")
                .timer()
                .count())
        .isEqualTo(1L);
    assertThat(output)
        .contains("scheduled_notification_batch_failed")
        .contains("messageId=scheduled-message-1")
        .contains("failureStage=page_load")
        .contains("exception=IllegalStateException");
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
        userProfileId, 11L, false, 0L, null, null, List.of());
  }

  /** 지정한 태그를 가진 Counter 값을 반환한다. */
  private double meterCount(String name, String tagKey, String tagValue) {
    return meterRegistry.find(name).tag(tagKey, tagValue).counter().count();
  }
}
