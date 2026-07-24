// Push Device 동기화 중 동시 저장 충돌을 한 번 재시도하는지 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.notification.domain.PushDevice;
import com.landit.landitbe.feature.notification.dto.PushDeviceSyncRequest;
import com.landit.landitbe.feature.notification.repository.PushDeviceRepository;
import com.landit.landitbe.shared.domain.AppPlatform;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** Push Device 동기화 중 동시 저장 충돌을 한 번 재시도하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class PushDeviceServiceTest {

  private static final Long USER_PROFILE_ID = 1L;
  private static final UUID INSTALLATION_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  private static final String EXPO_PUSH_TOKEN = "ExponentPushToken[device-token]";

  @Mock private PushDeviceRepository pushDeviceRepository;

  @Mock private PlatformTransactionManager transactionManager;

  private PushDeviceService pushDeviceService;

  @BeforeEach
  void setUp() {
    lenient()
        .when(transactionManager.getTransaction(any()))
        .thenAnswer(invocation -> new SimpleTransactionStatus());
    pushDeviceService = new PushDeviceService(pushDeviceRepository, transactionManager);
  }

  /** 새 설치 저장이 동시성 충돌하면 다시 조회한 기존 설치를 한 번 갱신한다. */
  @Test
  void retriesSynchronizationWhenNewInstallationSaveConflicts() {
    PushDevice existingPushDevice =
        PushDevice.create(
            2L, INSTALLATION_ID, AppPlatform.ANDROID, false, "ExponentPushToken[old-token]");
    PushDeviceSyncRequest request =
        new PushDeviceSyncRequest(AppPlatform.IOS, true, EXPO_PUSH_TOKEN);
    AtomicInteger installationLookupCount = new AtomicInteger();
    when(pushDeviceRepository.findByInstallationIdForUpdate(INSTALLATION_ID))
        .thenAnswer(
            invocation ->
                installationLookupCount.getAndIncrement() == 0
                    ? Optional.empty()
                    : Optional.of(existingPushDevice));
    when(pushDeviceRepository.findByExpoPushTokenForUpdate(EXPO_PUSH_TOKEN))
        .thenReturn(Optional.empty());
    when(pushDeviceRepository.saveAndFlush(any(PushDevice.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate installation"))
        .thenAnswer(this::returnSavedPushDevice);

    assertThatCode(() -> pushDeviceService.synchronize(USER_PROFILE_ID, INSTALLATION_ID, request))
        .doesNotThrowAnyException();

    assertThat(existingPushDevice.getUserProfileId()).isEqualTo(USER_PROFILE_ID);
    assertThat(existingPushDevice.getPlatform()).isEqualTo(AppPlatform.IOS);
    assertThat(existingPushDevice.isPushEnabled()).isTrue();
    assertThat(existingPushDevice.getExpoPushToken()).isEqualTo(EXPO_PUSH_TOKEN);
    verify(transactionManager, times(2)).getTransaction(any());
    verify(pushDeviceRepository, times(2)).findByInstallationIdForUpdate(INSTALLATION_ID);
    verify(pushDeviceRepository, times(2)).saveAndFlush(any(PushDevice.class));
  }

  /** 일시적인 잠금 획득 실패도 한 번 재시도한다. */
  @Test
  void retriesSynchronizationWhenLockAcquisitionTemporarilyFails() {
    PushDeviceSyncRequest request =
        new PushDeviceSyncRequest(AppPlatform.IOS, true, EXPO_PUSH_TOKEN);
    when(pushDeviceRepository.findByInstallationIdForUpdate(INSTALLATION_ID))
        .thenReturn(Optional.empty());
    when(pushDeviceRepository.findByExpoPushTokenForUpdate(EXPO_PUSH_TOKEN))
        .thenReturn(Optional.empty());
    when(pushDeviceRepository.saveAndFlush(any(PushDevice.class)))
        .thenThrow(new CannotAcquireLockException("temporary lock failure"))
        .thenAnswer(this::returnSavedPushDevice);

    assertThatCode(() -> pushDeviceService.synchronize(USER_PROFILE_ID, INSTALLATION_ID, request))
        .doesNotThrowAnyException();

    verify(transactionManager, times(2)).getTransaction(any());
    verify(pushDeviceRepository, times(2)).saveAndFlush(any(PushDevice.class));
  }

  /** 두 번째 동기화 시도도 실패하면 해당 예외를 그대로 전파한다. */
  @Test
  void propagatesSecondFailureAfterExactlyOneRetry() {
    PushDeviceSyncRequest request =
        new PushDeviceSyncRequest(AppPlatform.IOS, true, EXPO_PUSH_TOKEN);
    CannotAcquireLockException secondFailure =
        new CannotAcquireLockException("second attempt failed");
    when(pushDeviceRepository.findByInstallationIdForUpdate(INSTALLATION_ID))
        .thenReturn(Optional.empty());
    when(pushDeviceRepository.findByExpoPushTokenForUpdate(EXPO_PUSH_TOKEN))
        .thenReturn(Optional.empty());
    when(pushDeviceRepository.saveAndFlush(any(PushDevice.class)))
        .thenThrow(new DataIntegrityViolationException("first attempt failed"))
        .thenThrow(secondFailure);

    assertThatThrownBy(
            () -> pushDeviceService.synchronize(USER_PROFILE_ID, INSTALLATION_ID, request))
        .isSameAs(secondFailure);

    verify(transactionManager, times(2)).getTransaction(any());
    verify(pushDeviceRepository, times(2)).saveAndFlush(any(PushDevice.class));
  }

  /** 외부 트랜잭션이 있어도 첫 시도와 재시도를 각각 새 트랜잭션으로 실행한다. */
  @Test
  void usesRequiresNewForBothAttemptsInsideCallerTransaction() {
    when(pushDeviceRepository.findByInstallationIdForUpdate(INSTALLATION_ID))
        .thenReturn(Optional.empty());
    when(pushDeviceRepository.findByExpoPushTokenForUpdate(EXPO_PUSH_TOKEN))
        .thenReturn(Optional.empty());
    when(pushDeviceRepository.saveAndFlush(any(PushDevice.class)))
        .thenThrow(new DataIntegrityViolationException("first attempt failed"))
        .thenAnswer(this::returnSavedPushDevice);

    PushDeviceSyncRequest request =
        new PushDeviceSyncRequest(AppPlatform.IOS, true, EXPO_PUSH_TOKEN);
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> pushDeviceService.synchronize(USER_PROFILE_ID, INSTALLATION_ID, request));

    ArgumentCaptor<TransactionDefinition> transactionDefinitions =
        ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(transactionManager, times(3)).getTransaction(transactionDefinitions.capture());
    assertThat(transactionDefinitions.getAllValues())
        .extracting(TransactionDefinition::getPropagationBehavior)
        .containsExactly(
            TransactionDefinition.PROPAGATION_REQUIRED,
            TransactionDefinition.PROPAGATION_REQUIRES_NEW,
            TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** 저장된 Push Device를 Repository 반환값으로 돌려준다. */
  private PushDevice returnSavedPushDevice(InvocationOnMock invocation) {
    return invocation.getArgument(0);
  }
}
