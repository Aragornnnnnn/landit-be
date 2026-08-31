// 예약 푸시 개인화 정책의 유형 선택과 문구 fallback을 검증한다.

package com.landit.landitbe.feature.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.notification.domain.NotificationContentVariant;
import com.landit.landitbe.feature.notification.domain.NotificationType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 예약 푸시 개인화 정책의 유형 선택과 문구 fallback을 검증한다. */
class ScheduledNotificationPolicyTest {

  private static final LocalDate SCHEDULED_DATE = LocalDate.of(2026, 7, 30);

  /** 표현과 스몰톡이 동시에 가능하면 같은 날짜·사용자에서 선택 결과가 변하지 않는다. */
  @Test
  void selectsSameEligibleTypeDeterministically() {
    NotificationTargetSelectionInput input =
        input(
            1L,
            "민수",
            true,
            0L,
            List.of(new ExpressionNotificationCandidate(10L, 100L, false, "break the ice")));
    NotificationTargetSelectionService service = new NotificationTargetSelectionService();

    SelectedNotificationTarget first = service.select(input, SCHEDULED_DATE).orElseThrow();
    SelectedNotificationTarget second = service.select(input, SCHEDULED_DATE).orElseThrow();

    assertThat(second).isEqualTo(first);
    assertThat(first.notificationType())
        .isIn(NotificationType.CONTINUE_EXPRESSION, NotificationType.SMALL_TALK_REMINDER);
  }

  /** 표현 원문이 있으면 정확한 표현 문구를 만들고 시나리오 딥링크를 유지한다. */
  @Test
  void createsDynamicExpressionContent() {
    NotificationTargetSelectionInput input =
        input(
            1L,
            "민수",
            true,
            1L,
            List.of(new ExpressionNotificationCandidate(10L, 100L, false, "break the ice")));
    ScheduledNotificationContent content =
        ScheduledNotificationContent.from(
            new SelectedNotificationTarget(NotificationType.CONTINUE_EXPRESSION, 100L, 10L),
            input,
            SCHEDULED_DATE);

    assertThat(content.contentVariant()).isEqualTo(NotificationContentVariant.EXPRESSION_DYNAMIC);
    assertThat(content.title()).isEqualTo("“break the ice”, 어떤 상황에서 쓸까요?");
    assertThat(content.body()).isEqualTo("오늘 시나리오에서 이어지는 표현을 배워보세요.");
    assertThat(content.deepLink())
        .isEqualTo(
            "/expressions/scenario/10/100?utm_source=push&utm_medium=notification&"
                + "utm_campaign=continue_expression");
  }

  /** 최신 스몰톡 주제가 없으면 미해결 치환 문자열 없이 일반 문구로 대체한다. */
  @Test
  void fallsBackToGenericSmallTalkContentWithoutTitle() {
    NotificationTargetSelectionInput input = input(1L, "민수", true, 0L, List.of());
    ScheduledNotificationContent content =
        ScheduledNotificationContent.from(
            new SelectedNotificationTarget(NotificationType.SMALL_TALK_REMINDER, null, null),
            input,
            SCHEDULED_DATE);

    assertThat(content.contentVariant()).isEqualTo(NotificationContentVariant.SMALL_TALK_GENERIC);
    assertThat(content.title()).isEqualTo("오늘은 스몰톡 안 하시나요? 🥺");
    assertThat(content.body()).isEqualTo("테디가 당신과의 대화를 애타게 기다려요.");
    assertThat(content.body()).doesNotContain("null", "{latestFreeTalkTitle}");
  }

  /** 표현 제목은 Unicode code point 255자까지 허용하고 초과하면 일반 문구를 사용한다. */
  @Test
  void appliesExpressionCodePointBoundary() {
    String expressionText = "😀".repeat(239);
    NotificationTargetSelectionInput atLimit =
        input(
            1L,
            "민수",
            true,
            1L,
            List.of(new ExpressionNotificationCandidate(10L, 100L, false, expressionText)));
    ScheduledNotificationContent dynamic =
        ScheduledNotificationContent.from(
            new SelectedNotificationTarget(NotificationType.CONTINUE_EXPRESSION, 100L, 10L),
            atLimit,
            SCHEDULED_DATE);
    NotificationTargetSelectionInput overLimit =
        input(
            1L,
            "민수",
            true,
            1L,
            List.of(new ExpressionNotificationCandidate(10L, 100L, false, expressionText + "😀")));
    ScheduledNotificationContent generic =
        ScheduledNotificationContent.from(
            new SelectedNotificationTarget(NotificationType.CONTINUE_EXPRESSION, 100L, 10L),
            overLimit,
            SCHEDULED_DATE);

    assertThat(dynamic.contentVariant()).isEqualTo(NotificationContentVariant.EXPRESSION_DYNAMIC);
    assertThat(dynamic.title().codePointCount(0, dynamic.title().length())).isEqualTo(255);
    assertThat(generic.contentVariant()).isEqualTo(NotificationContentVariant.EXPRESSION_GENERIC);
    assertThat(generic.title()).isEqualTo("표현 학습을 이어가 볼까요?");
  }

  /** 스몰톡 본문은 Unicode code point 500자까지 허용하고 초과하면 일반 문구를 사용한다. */
  @Test
  void appliesSmallTalkCodePointBoundary() {
    String suffix = " 이야기, 테디와 조금 더 나눠볼까요?";
    int suffixCodePointCount = suffix.codePointCount(0, suffix.length());
    String titleAtLimit = "😀".repeat(500 - suffixCodePointCount);
    String titleOverLimit = "😀".repeat(501 - suffixCodePointCount);
    ScheduledNotificationContent dynamic = smallTalkContent(titleAtLimit);
    ScheduledNotificationContent generic = smallTalkContent(titleOverLimit);

    assertThat(dynamic.contentVariant()).isEqualTo(NotificationContentVariant.SMALL_TALK_DYNAMIC);
    assertThat(dynamic.body().codePointCount(0, dynamic.body().length())).isEqualTo(500);
    assertThat(generic.contentVariant()).isEqualTo(NotificationContentVariant.SMALL_TALK_GENERIC);
    assertThat(generic.body()).isEqualTo("테디가 당신과의 대화를 애타게 기다려요.");
  }

  /** R3은 닉네임 치환 결과의 고정된 문구와 개행을 유지한다. */
  @Test
  void preservesR3MessageWording() {
    NotificationTargetSelectionInput input =
        new NotificationTargetSelectionInput(
            1L, "민수", 10L, false, 0L, false, false, null, null, true, 4, null, List.of());

    ScheduledNotificationContent content =
        ScheduledNotificationContent.from(
            new SelectedNotificationTarget(NotificationType.DAILY_SCENARIO_REMINDER, 10L, null),
            input,
            SCHEDULED_DATE);

    if (content.contentVariant() == NotificationContentVariant.SCENARIO_R3) {
      assertThat(content.body()).isEqualTo("하지만 민수님은 아직입니다!!\n습관이 되기 전에 영어 공부 5분만 해봐요🥺");
    }
  }

  /** 어제 활동으로 최고 기록을 갱신하면 시나리오 A4를 강제한다. */
  @Test
  void usesA4WhenYesterdayActivitySetsNewRecord() {
    NotificationTargetSelectionInput input =
        new NotificationTargetSelectionInput(
            1L, "민수", 10L, false, 0L, false, true, 4, 4, true, 0, null, List.of());

    ScheduledNotificationContent content =
        ScheduledNotificationContent.from(
            new SelectedNotificationTarget(NotificationType.DAILY_SCENARIO_REMINDER, 10L, null),
            input,
            SCHEDULED_DATE);

    assertThat(content.contentVariant()).isEqualTo(NotificationContentVariant.SCENARIO_A4);
    assertThat(content.title()).isEqualTo("🚨 오늘의 시나리오를 깨면 연속 5일 달성");
    assertThat(content.body()).isEqualTo("5분 투자로 최고 기록을 달성해보세요!");
  }

  private NotificationTargetSelectionInput input(
      Long userProfileId,
      String nickname,
      boolean dailyScenarioCompleted,
      long freeTalkUsedSpeakingDurationMs,
      List<ExpressionNotificationCandidate> expressions) {
    return new NotificationTargetSelectionInput(
        userProfileId,
        nickname,
        10L,
        dailyScenarioCompleted,
        freeTalkUsedSpeakingDurationMs,
        false,
        false,
        null,
        null,
        false,
        null,
        null,
        expressions);
  }

  private ScheduledNotificationContent smallTalkContent(String latestTitle) {
    return ScheduledNotificationContent.from(
        new SelectedNotificationTarget(NotificationType.SMALL_TALK_REMINDER, null, null),
        inputWithLatestFreeTalkTitle(latestTitle),
        SCHEDULED_DATE);
  }

  private NotificationTargetSelectionInput inputWithLatestFreeTalkTitle(String latestTitle) {
    return new NotificationTargetSelectionInput(
        1L, "민수", 10L, true, 0L, false, false, null, null, false, null, latestTitle, List.of());
  }
}
