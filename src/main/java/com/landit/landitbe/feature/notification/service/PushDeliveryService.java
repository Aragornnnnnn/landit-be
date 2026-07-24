// 푸시 발송 이력을 선점하고 Ticket·Receipt 상태를 트랜잭션으로 기록한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.client.PushReceiptStatus;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.domain.PushDelivery;
import com.landit.landitbe.feature.notification.domain.PushDeliveryStatus;
import com.landit.landitbe.feature.notification.repository.PushDeliveryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 푸시 발송 이력을 선점하고 Ticket·Receipt 상태를 트랜잭션으로 기록한다. */
@Service
@RequiredArgsConstructor
public class PushDeliveryService {

  private static final String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

  private final PushDeliveryRepository pushDeliveryRepository;
  private final PushDeviceService pushDeviceService;

  /**
   * Push Device를 잠근 뒤 중복되지 않은 발송 이력을 Expo 호출 전에 선점한다.
   *
   * @param command 발송 대상과 메시지 정보
   * @return Expo에 전달할 선점된 발송 정보
   */
  @Transactional
  public Optional<PreparedPushDelivery> prepare(PreparePushDeliveryCommand command) {
    Optional<PushDelivery> existingDelivery =
        pushDeliveryRepository.findByDeduplicationKeyForUpdate(command.deduplicationKey());
    if (existingDelivery.isPresent()) {
      return claimRetry(existingDelivery.get(), command);
    }

    Optional<PushDeviceDeliveryTarget> deliveryTarget =
        pushDeviceService.findLockedSendableDeliveryTarget(
            command.pushDeviceId(), command.userProfileId());
    if (deliveryTarget.isEmpty()) {
      return Optional.empty();
    }
    if (pushDeliveryRepository.existsByDeduplicationKey(command.deduplicationKey())) {
      return Optional.empty();
    }

    PushDelivery delivery =
        pushDeliveryRepository.saveAndFlush(
            PushDelivery.requested(
                command.userProfileId(),
                command.pushDeviceId(),
                deliveryTarget.get().expoPushToken(),
                command.notificationType(),
                command.deduplicationKey(),
                command.title(),
                command.body(),
                command.deepLink(),
                LocalDateTime.now()));
    return Optional.of(prepared(delivery));
  }

  /** 잠긴 기존 이력의 Token과 현재 발송 대상을 확인하고 재시도 표식을 소비한다. */
  private Optional<PreparedPushDelivery> claimRetry(
      PushDelivery delivery, PreparePushDeliveryCommand command) {
    if (!delivery.isRetryable()) {
      return Optional.empty();
    }
    return pushDeviceService
        .findLockedSendableDeliveryTarget(command.pushDeviceId(), command.userProfileId())
        .filter(target -> delivery.getSentExpoPushToken().equals(target.expoPushToken()))
        .filter(target -> delivery.claimRetry())
        .map(target -> prepared(delivery));
  }

  /**
   * Ticket 접수 상태인 발송 이력 ID를 중복 방지 키 접두어로 조회한다.
   *
   * @param deduplicationKeyPrefix 발송 이력 중복 방지 키 접두어
   * @return Ticket 접수 상태인 발송 이력 ID 목록
   */
  @Transactional(readOnly = true)
  public List<Long> findAcceptedDeliveryIds(String deduplicationKeyPrefix) {
    return pushDeliveryRepository.findIdsByStatusAndDeduplicationKeyPrefix(
        PushDeliveryStatus.TICKET_ACCEPTED, deduplicationKeyPrefix);
  }

  /**
   * Expo Push Ticket 결과를 발송 이력에 기록한다.
   *
   * @param pushDeliveryId Push Delivery ID
   * @param result Expo Push Ticket 결과
   */
  @Transactional
  public void recordTicketResult(Long pushDeliveryId, PushTicketResult result) {
    PushDelivery delivery = requireForUpdate(pushDeliveryId);
    if (result.accepted()) {
      delivery.acceptTicket(result.ticketId());
      return;
    }
    if (delivery.failTicket(result.errorCode(), LocalDateTime.now())) {
      invalidateDeviceWhenUnregistered(delivery, result.errorCode());
    }
  }

  /**
   * 외부 Push 제공자의 일시 오류를 같은 발송 이력으로 재시도할 수 있게 기록한다.
   *
   * @param pushDeliveryId Push Delivery ID
   */
  @Transactional
  public void markRetryable(Long pushDeliveryId) {
    requireForUpdate(pushDeliveryId).markRetryable();
  }

  /**
   * Receipt 확인을 기다리는 발송 이력을 조회한다.
   *
   * @param pushDeliveryId Push Delivery ID
   * @return Ticket 접수 상태의 Receipt 조회 대상
   */
  @Transactional(readOnly = true)
  public Optional<PushReceiptTarget> findReceiptTarget(Long pushDeliveryId) {
    return pushDeliveryRepository
        .findById(pushDeliveryId)
        .filter(delivery -> delivery.getStatus() == PushDeliveryStatus.TICKET_ACCEPTED)
        .filter(delivery -> delivery.getExpoTicketId() != null)
        .map(delivery -> new PushReceiptTarget(delivery.getId(), delivery.getExpoTicketId()));
  }

  /**
   * Expo Push Receipt 결과를 발송 이력에 기록한다.
   *
   * @param pushDeliveryId Push Delivery ID
   * @param result Expo Push Receipt 결과
   */
  @Transactional
  public void recordReceiptResult(Long pushDeliveryId, PushReceiptResult result) {
    if (result.status() == PushReceiptStatus.NOT_READY) {
      throw new IllegalArgumentException("준비되지 않은 Receipt는 최종 결과로 기록할 수 없습니다.");
    }
    PushDelivery delivery = requireForUpdate(pushDeliveryId);
    if (delivery.getStatus() != PushDeliveryStatus.TICKET_ACCEPTED) {
      return;
    }
    if (result.status() == PushReceiptStatus.DELIVERED) {
      delivery.delivered(LocalDateTime.now());
      return;
    }
    if (delivery.failReceipt(result.errorCode(), LocalDateTime.now())) {
      invalidateDeviceWhenUnregistered(delivery, result.errorCode());
    }
  }

  /** 선점된 발송 이력의 Token 스냅샷으로 외부 발송 정보를 생성한다. */
  private PreparedPushDelivery prepared(PushDelivery delivery) {
    return new PreparedPushDelivery(
        delivery.getId(),
        delivery.getSentExpoPushToken(),
        delivery.getTitle(),
        delivery.getBody(),
        delivery.getDeepLink());
  }

  /** 상태를 변경할 Push Delivery를 쓰기 잠금으로 조회한다. */
  private PushDelivery requireForUpdate(Long pushDeliveryId) {
    return pushDeliveryRepository
        .findByIdForUpdate(pushDeliveryId)
        .orElseThrow(() -> new IllegalArgumentException("푸시 발송 이력이 존재하지 않습니다."));
  }

  /** Expo가 등록 해제된 기기라고 응답하면 연결된 Token을 무효화한다. */
  private void invalidateDeviceWhenUnregistered(PushDelivery delivery, String errorCode) {
    if (!DEVICE_NOT_REGISTERED.equals(errorCode)) {
      return;
    }
    pushDeviceService.invalidateCurrentTokenOwner(delivery.getSentExpoPushToken());
  }
}
