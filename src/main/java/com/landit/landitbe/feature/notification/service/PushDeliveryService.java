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
  private static final String BAD_DEVICE_TOKEN = "BadDeviceToken";

  private final PushDeliveryRepository pushDeliveryRepository;
  private final UserPushTokenDeliveryService userPushTokenDeliveryService;

  /**
   * User Push Token을 잠근 뒤 중복되지 않은 발송 이력을 Expo 호출 전에 선점한다.
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

    Optional<UserPushTokenDeliveryTarget> deliveryTarget =
        userPushTokenDeliveryService.findLockedSendableDeliveryTarget(
            command.userPushTokenId(), command.userProfileId());
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
                command.userPushTokenId(),
                deliveryTarget.get().expoPushToken(),
                command.notificationType(),
                command.contentVariant(),
                command.deduplicationKey(),
                command.title(),
                command.body(),
                command.deepLink(),
                LocalDateTime.now()));
    return Optional.of(prepared(delivery));
  }

  /**
   * 확정된 관리자 대상에 기존 발송 이력을 연결해 선점한다.
   *
   * @param command 발송 정보
   * @param targetId 확정 대상 ID
   * @return 신규 또는 429 재시도 선점
   */
  @Transactional
  public Optional<PreparedPushDelivery> prepareAdmin(
      PreparePushDeliveryCommand command, long targetId) {
    Optional<PreparedPushDelivery> prepared = prepare(command);
    prepared.ifPresent(
        value -> requireForUpdate(value.pushDeliveryId()).attachAdminTarget(targetId));
    return prepared;
  }

  /**
   * 관리자 발송의 결과 불명을 기록한다.
   *
   * @param id 발송 이력 ID
   * @param code 실패 사유
   */
  @Transactional
  public void adminUnknown(long id, String code) {
    requireForUpdate(id).adminUnknown(code);
  }

  /**
   * 명시적 HTTP 429 거부만 재시도 가능하게 기록한다.
   *
   * @param id 발송 이력 ID
   */
  @Transactional
  public void adminRateLimited(long id) {
    requireForUpdate(id).adminRateLimited(LocalDateTime.now());
  }

  /**
   * 관리자 Receipt 회차를 DB 잠금으로 선점한다.
   *
   * @param id 발송 이력 ID
   * @return 이번 회차 조회 대상
   */
  @Transactional
  public Optional<AdminReceiptTarget> claimAdminReceipt(long id) {
    PushDelivery delivery = requireForUpdate(id);
    return delivery.claimAdminReceipt(LocalDateTime.now())
        ? Optional.of(
            new AdminReceiptTarget(
                id, delivery.getExpoTicketId(), delivery.getAdminReceiptAttempt()))
        : Optional.empty();
  }

  /**
   * 관리자 Receipt 다음 회차를 DB에 남긴다.
   *
   * @param id 발송 이력 ID
   * @param attempt 선점한 확인 회차
   */
  @Transactional
  public void deferAdminReceipt(long id, int attempt) {
    requireForUpdate(id).deferAdminReceipt(attempt, LocalDateTime.now());
  }

  /** 잠긴 기존 이력의 Token과 현재 발송 대상을 확인하고 재시도 표식을 소비한다. */
  private Optional<PreparedPushDelivery> claimRetry(
      PushDelivery delivery, PreparePushDeliveryCommand command) {
    if (!delivery.isRetryable()) {
      return Optional.empty();
    }
    return userPushTokenDeliveryService
        .findLockedSendableDeliveryTarget(command.userPushTokenId(), command.userProfileId())
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
      revokeWhenDeviceNotRegistered(delivery, result.errorCode());
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
    if (delivery.getStatus() != PushDeliveryStatus.TICKET_ACCEPTED
        && !(delivery.getAdminPushTargetId() != null
            && delivery.getStatus() == PushDeliveryStatus.UNKNOWN
            && delivery.getExpoTicketId() != null)) {
      return;
    }
    if (result.status() == PushReceiptStatus.DELIVERED) {
      delivery.delivered(LocalDateTime.now());
      return;
    }
    if (delivery.failReceipt(result.errorCode(), LocalDateTime.now())) {
      revokeWhenInvalidReceiptToken(delivery, result.errorCode());
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

  /** Expo가 등록 해제된 기기라고 응답하면 현재 연결된 Token을 무효화한다. */
  private void revokeWhenDeviceNotRegistered(PushDelivery delivery, String errorCode) {
    if (!DEVICE_NOT_REGISTERED.equals(errorCode)) {
      return;
    }
    userPushTokenDeliveryService.revokeCurrentTokenOwner(delivery.getSentExpoPushToken());
  }

  /** Receipt에서 Expo 또는 APNs가 기기 Token 오류를 반환하면 현재 연결된 Token을 무효화한다. */
  private void revokeWhenInvalidReceiptToken(PushDelivery delivery, String errorCode) {
    if (!DEVICE_NOT_REGISTERED.equals(errorCode) && !BAD_DEVICE_TOKEN.equals(errorCode)) {
      return;
    }
    userPushTokenDeliveryService.revokeCurrentTokenOwner(delivery.getSentExpoPushToken());
  }

  /**
   * 결과 저장의 선점 회차를 포함한 Receipt 대상이다.
   *
   * @param pushDeliveryId 발송 이력 ID
   * @param ticketId Expo Ticket
   * @param attempt 선점 회차
   */
  public record AdminReceiptTarget(long pushDeliveryId, String ticketId, int attempt) {}
}
