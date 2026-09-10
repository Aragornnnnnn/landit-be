// RevenueCat 웹훅으로 받은 구독 이벤트를 사용자별 결제 이력으로 저장한다.

package com.landit.landitbe.feature.subscription.domain;

import com.landit.landitbe.feature.profile.domain.SubscriptionPeriodType;
import com.landit.landitbe.feature.profile.domain.SubscriptionStore;
import com.landit.landitbe.shared.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * RevenueCat 웹훅으로 받은 구독 이벤트를 사용자별 결제 이력으로 저장한다.
 *
 * <p>RevenueCat은 재시도할 때 같은 이벤트 ID를 다시 보내므로 이벤트 ID를 유일 키로 두어 중복 저장을 막는다.
 */
@Getter
@Entity
@Table(
    name = "subscription_event",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_subscription_event_event_id", columnNames = "event_id"))
public class SubscriptionEvent extends BaseCreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, length = 100)
  private String eventId;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 40)
  private SubscriptionEventType type;

  @Column(name = "product_id", length = 255)
  private String productId;

  @Enumerated(EnumType.STRING)
  @Column(name = "period_type", length = 30)
  private SubscriptionPeriodType periodType;

  @Column(name = "price", precision = 19, scale = 4)
  private BigDecimal price;

  @Column(name = "currency", length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "store", length = 30)
  private SubscriptionStore store;

  @Column(name = "environment", length = 20)
  private String environment;

  @Column(name = "cancel_reason", length = 50)
  private String cancelReason;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected SubscriptionEvent() {}

  private SubscriptionEvent(
      String eventId,
      Long userProfileId,
      SubscriptionEventType type,
      String productId,
      SubscriptionPeriodType periodType,
      BigDecimal price,
      String currency,
      SubscriptionStore store,
      String environment,
      String cancelReason,
      LocalDateTime occurredAt,
      LocalDateTime expiresAt) {
    this.eventId = eventId;
    this.userProfileId = userProfileId;
    this.type = type;
    this.productId = productId;
    this.periodType = periodType;
    this.price = price;
    this.currency = currency;
    this.store = store;
    this.environment = environment;
    this.cancelReason = cancelReason;
    this.occurredAt = occurredAt;
    this.expiresAt = expiresAt;
  }

  /**
   * 웹훅 이벤트 한 건을 결제 이력으로 생성한다.
   *
   * @param eventId RevenueCat 이벤트 ID
   * @param userProfileId 이벤트가 속한 사용자 프로필 ID
   * @param type 이벤트 타입
   * @param productId 구독 상품 ID. 없으면 null
   * @param periodType 결제 기간 종류. 없거나 알 수 없으면 null
   * @param price 결제 통화 기준 금액. 체험·해지처럼 결제가 없으면 null
   * @param currency ISO 4217 통화 코드. 없으면 null
   * @param store 결제한 스토어. 없거나 알 수 없으면 null
   * @param environment SANDBOX 또는 PRODUCTION
   * @param cancelReason CANCELLATION 이벤트의 해지 사유. 없으면 null
   * @param occurredAt 결제 또는 이벤트 발생 시각
   * @param expiresAt 구독 만료 시각. 없으면 null
   * @return 생성한 결제 이력
   */
  public static SubscriptionEvent record(
      String eventId,
      Long userProfileId,
      SubscriptionEventType type,
      String productId,
      SubscriptionPeriodType periodType,
      BigDecimal price,
      String currency,
      SubscriptionStore store,
      String environment,
      String cancelReason,
      LocalDateTime occurredAt,
      LocalDateTime expiresAt) {
    return new SubscriptionEvent(
        eventId,
        userProfileId,
        type,
        productId,
        periodType,
        price,
        currency,
        store,
        environment,
        cancelReason,
        occurredAt,
        expiresAt);
  }
}
