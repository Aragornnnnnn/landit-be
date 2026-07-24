// 인증 사용자의 앱 설치별 푸시 수신 상태와 Token 소유권을 동기화한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.PushDevice;
import com.landit.landitbe.feature.notification.dto.PushDeviceSyncRequest;
import com.landit.landitbe.feature.notification.dto.PushDeviceSyncResponse;
import com.landit.landitbe.feature.notification.repository.PushDeviceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/** 인증 사용자의 앱 설치별 푸시 수신 상태와 Token 소유권을 동기화한다. */
@Service
public class PushDeviceService {

  private final PushDeviceRepository pushDeviceRepository;
  private final TransactionOperations synchronizationTransactions;

  /**
   * 각 동기화 시도를 독립된 새 트랜잭션에서 실행하도록 서비스를 구성한다.
   *
   * @param pushDeviceRepository Push Device 저장소
   * @param transactionManager 애플리케이션 트랜잭션 관리자
   */
  public PushDeviceService(
      PushDeviceRepository pushDeviceRepository, PlatformTransactionManager transactionManager) {
    this.pushDeviceRepository = pushDeviceRepository;
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.synchronizationTransactions = transactionTemplate;
  }

  /**
   * 설치 ID를 기준으로 현재 사용자, 수신 설정, Expo Push Token을 멱등 동기화한다.
   *
   * @param userProfileId 현재 인증 사용자 ID
   * @param installationId 앱 설치 ID
   * @param request 현재 설치 상태
   * @return 동기화된 설치 상태
   */
  public PushDeviceSyncResponse synchronize(
      Long userProfileId, UUID installationId, PushDeviceSyncRequest request) {
    try {
      return executeSynchronization(userProfileId, installationId, request);
    } catch (DataIntegrityViolationException | TransientDataAccessException exception) {
      return executeSynchronization(userProfileId, installationId, request);
    }
  }

  /**
   * Push Device를 잠그고 현재 사용자 소유이면서 발송 가능한 설치 정보를 반환한다.
   *
   * <p>호출자의 발송 선점 트랜잭션에 참여하므로 반환 뒤 발송 이력을 저장할 때까지 잠금이 유지된다.
   *
   * @param pushDeviceId Push Device ID
   * @param userProfileId 발송 대상 사용자 ID
   * @return 잠금 확인된 발송 대상
   */
  @Transactional
  public Optional<PushDeviceDeliveryTarget> findLockedSendableDeliveryTarget(
      Long pushDeviceId, Long userProfileId) {
    return pushDeviceRepository
        .findByIdForUpdate(pushDeviceId)
        .filter(PushDevice::isSendable)
        .filter(pushDevice -> pushDevice.getUserProfileId().equals(userProfileId))
        .map(
            pushDevice ->
                new PushDeviceDeliveryTarget(pushDevice.getId(), pushDevice.getExpoPushToken()));
  }

  /**
   * 발송 당시 Token을 현재 소유한 Push Device를 잠그고 Token을 무효화한다.
   *
   * @param sentExpoPushToken 발송 당시 Expo Push Token
   */
  @Transactional
  public void invalidateCurrentTokenOwner(String sentExpoPushToken) {
    pushDeviceRepository
        .findByExpoPushTokenForUpdate(sentExpoPushToken)
        .ifPresent(PushDevice::invalidateToken);
  }

  /** 저장 충돌 뒤에도 새 트랜잭션으로 재시도할 수 있도록 동기화 작업을 실행한다. */
  private PushDeviceSyncResponse executeSynchronization(
      Long userProfileId, UUID installationId, PushDeviceSyncRequest request) {
    return synchronizationTransactions.execute(
        status -> synchronizeInTransaction(userProfileId, installationId, request));
  }

  /** 하나의 트랜잭션에서 앱 설치 상태와 Expo Push Token 소유권을 동기화한다. */
  private PushDeviceSyncResponse synchronizeInTransaction(
      Long userProfileId, UUID installationId, PushDeviceSyncRequest request) {
    String expoPushToken = request.normalizedExpoPushToken();
    PushDevice pushDevice =
        pushDeviceRepository
            .findByInstallationIdForUpdate(installationId)
            .orElseGet(
                () ->
                    PushDevice.create(
                        userProfileId,
                        installationId,
                        request.platform(),
                        request.pushEnabled(),
                        expoPushToken));

    detachTokenFromAnotherInstallation(pushDevice, expoPushToken);
    pushDevice.synchronize(userProfileId, request.platform(), request.pushEnabled(), expoPushToken);

    return PushDeviceSyncResponse.from(pushDeviceRepository.saveAndFlush(pushDevice));
  }

  /** 같은 Expo Push Token을 사용 중인 다른 설치의 Token 연결을 해제한다. */
  private void detachTokenFromAnotherInstallation(
      PushDevice currentPushDevice, String expoPushToken) {
    if (expoPushToken == null) {
      return;
    }

    pushDeviceRepository
        .findByExpoPushTokenForUpdate(expoPushToken)
        .filter(tokenOwner -> tokenOwner != currentPushDevice)
        .ifPresent(
            tokenOwner -> {
              tokenOwner.detachToken();
              pushDeviceRepository.saveAndFlush(tokenOwner);
            });
  }
}
