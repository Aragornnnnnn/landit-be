// 푸시 발송 이력을 선점하고 Ticket·Receipt 상태를 트랜잭션으로 기록한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.client.PushReceiptResult;
import com.landit.landitbe.feature.notification.client.PushReceiptStatus;
import com.landit.landitbe.feature.notification.client.PushTicketResult;
import com.landit.landitbe.feature.notification.domain.PushDelivery;
import com.landit.landitbe.feature.notification.domain.PushDeliveryStatus;
import com.landit.landitbe.feature.notification.repository.PushDeliveryBatchRepository;
import com.landit.landitbe.feature.notification.repository.PushDeliveryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final PushDeliveryBatchRepository pushDeliveryBatchRepository;

  /**
   * 최대 100개 후보를 일괄 잠금·검사하고 발송 가능한 이력만 선점한다.
   *
   * @param commands 발송 후보 목록
   * @return 입력 순서의 신규 또는 재시도 선점 결과
   */
  @Transactional
  public List<PreparedPushDelivery> prepareAll(List<PreparePushDeliveryCommand> commands) {
    requireBatchSize(commands.size());
    if (commands.isEmpty()) {
      return List.of();
    }
    List<PreparePushDeliveryCommand> unique =
        commands.stream()
            .collect(
                Collectors.toMap(
                    PreparePushDeliveryCommand::deduplicationKey,
                    Function.identity(),
                    (first, ignored) -> first,
                    LinkedHashMap::new))
            .values()
            .stream()
            .toList();
    List<String> keys = unique.stream().map(PreparePushDeliveryCommand::deduplicationKey).toList();
    Map<String, PushDelivery> existing =
        pushDeliveryRepository.findAllByKeysForUpdate(keys).stream()
            .collect(Collectors.toMap(PushDelivery::getDeduplicationKey, Function.identity()));
    Map<Long, Long> owners = new LinkedHashMap<>();
    unique.forEach(
        c -> {
          Long previous = owners.putIfAbsent(c.userPushTokenId(), c.userProfileId());
          if (previous != null && !previous.equals(c.userProfileId())) {
            throw new IllegalArgumentException("같은 Token에 서로 다른 사용자를 지정할 수 없습니다.");
          }
        });
    Map<Long, UserPushTokenDeliveryTarget> targets =
        userPushTokenDeliveryService.findLockedSendableDeliveryTargets(owners);
    Set<String> presentKeys = new HashSet<>(pushDeliveryRepository.findExistingKeys(keys));
    Map<String, PreparedPushDelivery> prepared = new LinkedHashMap<>();
    List<PushDelivery> created = new ArrayList<>();
    List<PushDelivery> retries = new ArrayList<>();
    for (PreparePushDeliveryCommand command : unique) {
      UserPushTokenDeliveryTarget target = targets.get(command.userPushTokenId());
      if (target == null) {
        continue;
      }
      PushDelivery delivery = existing.get(command.deduplicationKey());
      if (delivery != null) {
        if (delivery.getSentExpoPushToken().equals(target.expoPushToken())
            && delivery.claimRetry()) {
          retries.add(delivery);
          prepared.put(command.deduplicationKey(), prepared(delivery));
        }
      } else if (!presentKeys.contains(command.deduplicationKey())) {
        created.add(
            PushDelivery.requested(
                command.userProfileId(),
                command.userPushTokenId(),
                target.expoPushToken(),
                command.notificationType(),
                command.contentVariant(),
                command.deduplicationKey(),
                command.title(),
                command.body(),
                command.deepLink(),
                LocalDateTime.now()));
      }
    }
    // 재시도 표식을 먼저 저장해 INSERT 이후 JPA 자동 flush가 건별 갱신하지 않게 한다.
    pushDeliveryBatchRepository.updateStates(retries);
    Map<String, Long> inserted = pushDeliveryBatchRepository.insertRequested(created);
    created.forEach(
        d ->
            prepared.put(
                d.getDeduplicationKey(),
                new PreparedPushDelivery(
                    inserted.get(d.getDeduplicationKey()),
                    d.getSentExpoPushToken(),
                    d.getTitle(),
                    d.getBody(),
                    d.getDeepLink())));
    return unique.stream()
        .map(c -> prepared.get(c.deduplicationKey()))
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  /**
   * 여러 이벤트의 접수 이력을 한 번에 조회한다. 현재 활성 Token이 없어도 복구한다.
   *
   * @param prefixes 이벤트별 중복 방지 키 접두어
   * @return Receipt 확인을 다시 예약할 발송 ID
   */
  @Transactional(readOnly = true)
  public List<Long> findAcceptedDeliveryIdsForEvents(List<String> prefixes) {
    return pushDeliveryBatchRepository.findAcceptedIds(prefixes);
  }

  /**
   * Expo 묶음 응답을 입력 순서의 ID에 연결해 하나의 트랜잭션으로 기록한다.
   *
   * @param ids 발송 ID, 최대 100개
   * @param results ID 순서에 대응하는 Ticket 결과
   */
  @Transactional
  public void recordTicketResults(List<Long> ids, List<PushTicketResult> results) {
    requireBatchSize(ids.size());
    if (ids.size() != results.size() || ids.stream().distinct().count() != ids.size()) {
      throw new IllegalArgumentException("Ticket 결과와 발송 ID 개수가 다르거나 ID가 중복됩니다.");
    }
    if (ids.isEmpty()) {
      return;
    }
    List<PushDelivery> deliveries = pushDeliveryRepository.findAllByIdsForUpdate(ids);
    if (deliveries.size() != ids.size()) {
      throw new IllegalArgumentException("푸시 발송 이력이 존재하지 않습니다.");
    }
    Map<Long, PushTicketResult> byId = new LinkedHashMap<>();
    for (int i = 0; i < ids.size(); i++) {
      byId.put(ids.get(i), results.get(i));
    }
    List<PushDelivery> changed = new ArrayList<>();
    List<String> revokedTokens = new ArrayList<>();
    for (PushDelivery delivery : deliveries) {
      PushTicketResult result = byId.get(delivery.getId());
      boolean transitioned =
          result.accepted()
              ? delivery.acceptTicket(result.ticketId())
              : delivery.failTicket(result.errorCode(), LocalDateTime.now());
      if (transitioned) {
        changed.add(delivery);
        if (!result.accepted() && DEVICE_NOT_REGISTERED.equals(result.errorCode())) {
          revokedTokens.add(delivery.getSentExpoPushToken());
        }
      }
    }
    pushDeliveryBatchRepository.updateStates(changed);
    userPushTokenDeliveryService.revokeCurrentTokenOwners(revokedTokens);
  }

  private void requireBatchSize(int size) {
    if (size > 100) {
      throw new IllegalArgumentException("발송 DB 묶음은 최대 100건입니다.");
    }
  }

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
   * 지정한 Token에서 Ticket 접수 상태인 발송 이력 ID를 조회한다.
   *
   * @param deduplicationKeyPrefix 발송 이력 중복 방지 키 접두어
   * @param userPushTokenIds 조회할 Token ID
   * @return Ticket 접수 상태인 발송 이력 ID 목록
   */
  @Transactional(readOnly = true)
  public List<Long> findAcceptedDeliveryIds(
      String deduplicationKeyPrefix, List<Long> userPushTokenIds) {
    return pushDeliveryRepository.findIdsByStatusAndDeduplicationKeyPrefixAndTokenIds(
        PushDeliveryStatus.TICKET_ACCEPTED, deduplicationKeyPrefix, userPushTokenIds);
  }

  /**
   * 지정한 Token 중 아직 Expo 요청 결과 기록을 기다리는 이력이 있는지 확인한다.
   *
   * @param deduplicationKeyPrefix 발송 이력 중복 방지 키 접두어
   * @param userPushTokenIds 조회할 Token ID
   * @return 요청 처리 중인 이력이 있으면 {@code true}
   */
  @Transactional(readOnly = true)
  public boolean hasRequestedDeliveries(
      String deduplicationKeyPrefix, List<Long> userPushTokenIds) {
    if (userPushTokenIds.isEmpty()) {
      return false;
    }
    return !pushDeliveryRepository
        .findIdsByStatusAndDeduplicationKeyPrefixAndTokenIds(
            PushDeliveryStatus.REQUESTED, deduplicationKeyPrefix, userPushTokenIds)
        .isEmpty();
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
    if (delivery.getStatus() != PushDeliveryStatus.TICKET_ACCEPTED) {
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
}
