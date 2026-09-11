// 사용자의 서버 기준 구독 상태와 페이월 판단 근거를 API 응답으로 제공한다.

package com.landit.landitbe.feature.subscription.dto;

import com.landit.landitbe.feature.profile.domain.SubscriptionPeriodType;
import com.landit.landitbe.feature.profile.domain.SubscriptionStatus;
import com.landit.landitbe.feature.profile.domain.SubscriptionStore;
import com.landit.landitbe.feature.profile.dto.UserSubscriptionSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 사용자의 서버 기준 구독 상태와 페이월 판단 근거를 API 응답으로 제공한다.
 *
 * @param subscriptionStatus 구독 상태
 * @param premium 프리미엄 혜택 적용 여부
 * @param periodType 현재 결제 기간 종류. 무료 체험 중이면 TRIAL. 프리미엄이 꺼져 있거나 알 수 없으면 {@code null}
 * @param expiresAt 구독 만료 시각. 프리미엄이 꺼져 있거나 알 수 없으면 {@code null}
 * @param conversationCompletedSinceLaunch 유료 구독 도입 이후 시나리오 대화를 끝까지 완료한 적이 있는지
 * @param productId 구독 상품 ID. 프리미엄이 꺼져 있거나 알 수 없으면 {@code null}
 * @param store 결제한 스토어. 프리미엄이 꺼져 있거나 알 수 없으면 {@code null}
 */
@Schema(description = "사용자 구독 상태")
public record UserSubscriptionResponse(
    @Schema(
            description =
                "구독 상태. NONE(구독 이력 없음), ACTIVE(구독 중), CANCELED(해지 예약, 만료 전까지 프리미엄 유지),"
                    + " EXPIRED(만료 또는 환불). 프리미엄 적용 여부는 premium으로 판단한다.",
            example = "ACTIVE")
        SubscriptionStatus subscriptionStatus,
    @Schema(description = "프리미엄 혜택 적용 여부", example = "true") boolean premium,
    @Schema(
            description =
                "현재 결제 기간 종류. TRIAL(무료 체험), INTRO(할인 도입가), NORMAL(정가), PROMOTIONAL(프로모션 무료),"
                    + " PREPAID(선결제). 프리미엄이 꺼져 있으면 null",
            example = "TRIAL")
        SubscriptionPeriodType periodType,
    @Schema(description = "구독 만료 시각. 프리미엄이 꺼져 있으면 null", example = "2026-10-04T12:00:00")
        LocalDateTime expiresAt,
    @Schema(
            description =
                "유료 구독 도입 이후 시나리오 대화를 끝까지 완료한 적이 있으면 true. 신규 가입자는 시나리오 1 완료,"
                    + " 도입 전 가입자는 도입 후 오늘의 시나리오 완료가 기준이다. 도입 시점 미설정 또는 도입 전에는 항상 false.",
            example = "false")
        boolean conversationCompletedSinceLaunch,
    @Schema(
            description = "구독 상품 ID. 웹은 이 값으로 월간·연간 이름을 붙인다. 프리미엄이 꺼져 있으면 null",
            example = "com.saynow.app.premium.yearly")
        String productId,
    @Schema(
            description =
                "결제한 스토어. APP_STORE, PLAY_STORE 등 RevenueCat store 값. 웹은 이 값으로 구독 관리 링크를 고른다."
                    + " 프리미엄이 꺼져 있으면 null",
            example = "APP_STORE")
        SubscriptionStore store,
    @Schema(description = "현재 계정에 페이월 표시와 서버 유료 제한을 적용하는지") boolean paymentEnabled,
    @Schema(description = "공개 정책 버전") long paymentPolicyVersion,
    @Schema(description = "배포 전환으로 새 학습 시작만 일시 중지됐는지") boolean newStartsPaused,
    @Schema(description = "새 시나리오 대화를 시작할 수 있는지") boolean canStartScenario,
    @Schema(description = "소모한 첫 무료 기회에 연결된 세션. 재개 가능 여부는 세션 조회로 확인한다")
        Long freeScenarioSessionId) {

  /**
   * 프로필의 구독 스냅샷과 대화 완료 여부를 응답으로 합친다.
   *
   * @param snapshot 프로필 기능이 제공한 구독 상태 스냅샷
   * @param conversationCompletedSinceLaunch 유료 구독 도입 이후 시나리오 대화 완료 여부
   * @return 사용자 구독 상태 응답
   */
  public static UserSubscriptionResponse of(
      UserSubscriptionSnapshot snapshot, boolean conversationCompletedSinceLaunch) {
    return new UserSubscriptionResponse(
        snapshot.subscriptionStatus(),
        snapshot.premium(),
        snapshot.periodType(),
        snapshot.expiresAt(),
        conversationCompletedSinceLaunch,
        snapshot.productId(),
        snapshot.store(),
        false,
        0,
        false,
        true,
        null);
  }

  /** 기존 구독 상태와 서버의 새 시작 정책을 함께 전달한다. */
  public UserSubscriptionResponse withAccess(
      boolean effectivePremium,
      boolean paymentEnabled,
      long paymentPolicyVersion,
      boolean newStartsPaused,
      boolean canStartScenario,
      Long freeScenarioSessionId) {
    return new UserSubscriptionResponse(
        !effectivePremium && premium ? SubscriptionStatus.EXPIRED : subscriptionStatus,
        effectivePremium,
        effectivePremium ? periodType : null,
        effectivePremium ? expiresAt : null,
        conversationCompletedSinceLaunch,
        effectivePremium ? productId : null,
        effectivePremium ? store : null,
        paymentEnabled,
        paymentPolicyVersion,
        newStartsPaused,
        canStartScenario,
        freeScenarioSessionId);
  }
}
