// 예약 학습 알림의 결정적 문구 변형과 유형별 딥링크를 정의한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.NotificationContentVariant;
import java.time.LocalDate;
import java.util.List;

/**
 * 예약 학습 알림의 결정적 문구 변형과 유형별 딥링크다.
 *
 * @param contentVariant 선정된 문구 변형
 * @param title 푸시 제목
 * @param body 푸시 본문
 * @param deepLink 알림 선택 시 이동할 앱 딥링크
 */
record ScheduledNotificationContent(
    NotificationContentVariant contentVariant, String title, String body, String deepLink) {

  private static final String CONTENT_DECISION_SCOPE = "scheduled-notification:content:v1";
  private static final String SCENARIO_A2_BODY = "오늘이 지나면 이 시나리오가 사라진대요😵‍💫\n자기 전 5분만 투자하세요";
  private static final String SCENARIO_R0_TITLE = "어떤 하얀 뱁새가 그러는데,,";
  private static final List<String> RESERVATION_MARKERS =
      List.of(
          "OO",
          "{nickname}",
          "{missedDayCount}",
          "{expectedStreakDays}",
          "{targetExpressionText}",
          "{latestFreeTalkTitle}");
  private static final List<NotificationContentVariant> SCENARIO_DEFAULT_VARIANTS =
      List.of(
          NotificationContentVariant.SCENARIO_A1,
          NotificationContentVariant.SCENARIO_A2,
          NotificationContentVariant.SCENARIO_A3);

  static ScheduledNotificationContent from(SelectedNotificationTarget target) {
    return from(
        target,
        new NotificationTargetSelectionInput(
            0L, null, null, false, 0L, false, false, null, null, false, null, null, List.of()),
        LocalDate.of(1970, 1, 1));
  }

  static ScheduledNotificationContent from(
      SelectedNotificationTarget target,
      NotificationTargetSelectionInput input,
      LocalDate scheduledDate) {
    return switch (target.notificationType()) {
      case DAILY_SCENARIO_REMINDER -> scenarioContent(target, input, scheduledDate);
      case CONTINUE_EXPRESSION -> expressionContent(target, input);
      case SMALL_TALK_REMINDER -> smallTalkContent(target, input);
      default -> throw new IllegalArgumentException("예약 알림에 지원하지 않는 유형입니다.");
    };
  }

  private static ScheduledNotificationContent scenarioContent(
      SelectedNotificationTarget target,
      NotificationTargetSelectionInput input,
      LocalDate scheduledDate) {
    NotificationContentVariant variant = scenarioVariant(input, scheduledDate);
    String title = scenarioTitle(variant, input);
    String body = scenarioBody(variant, input);
    return new ScheduledNotificationContent(
        variant,
        title,
        body,
        "/conversation/scenario/" + target.targetId() + campaign("daily_scenario_reminder"));
  }

  private static NotificationContentVariant scenarioVariant(
      NotificationTargetSelectionInput input, LocalDate scheduledDate) {
    if (isNewStreakRecord(input)) {
      return NotificationContentVariant.SCENARIO_A4;
    }
    List<NotificationContentVariant> variants = scenarioVariants(input);
    return variants.get(
        DeterministicNotificationChoice.choose(
            CONTENT_DECISION_SCOPE, scheduledDate, input.userProfileId(), variants.size()));
  }

  private static boolean isNewStreakRecord(NotificationTargetSelectionInput input) {
    return input.activeYesterday()
        && !input.activeToday()
        && input.currentStreakDays() != null
        && input.longestStreakDays() != null
        && input.currentStreakDays() + 1 > input.longestStreakDays();
  }

  private static List<NotificationContentVariant> scenarioVariants(
      NotificationTargetSelectionInput input) {
    List<NotificationContentVariant> variants;
    Integer missedDayCount = input.missedDayCount();
    if (!input.priorActiveDayHistory()) {
      variants = List.of(NotificationContentVariant.SCENARIO_R0);
    } else if (missedDayCount == null) {
      variants = List.of(NotificationContentVariant.SCENARIO_R0);
    } else if (missedDayCount == 1) {
      variants =
          List.of(NotificationContentVariant.SCENARIO_R0, NotificationContentVariant.SCENARIO_R1);
    } else if (missedDayCount == 2 || missedDayCount == 3) {
      variants =
          List.of(
              NotificationContentVariant.SCENARIO_R1,
              NotificationContentVariant.SCENARIO_R2,
              NotificationContentVariant.SCENARIO_R5);
    } else if (missedDayCount >= 4) {
      variants =
          List.of(
              NotificationContentVariant.SCENARIO_R2,
              NotificationContentVariant.SCENARIO_R3,
              NotificationContentVariant.SCENARIO_R4,
              NotificationContentVariant.SCENARIO_R5,
              NotificationContentVariant.SCENARIO_R6);
    } else if (missedDayCount == 0) {
      variants = SCENARIO_DEFAULT_VARIANTS;
    } else {
      variants = List.of(NotificationContentVariant.SCENARIO_R0);
    }
    if (input.nickname() == null
        || input.nickname().isBlank()
        || containsReservationMarker(input.nickname())) {
      variants =
          variants.stream()
              .filter(
                  variant ->
                      variant != NotificationContentVariant.SCENARIO_R2
                          && variant != NotificationContentVariant.SCENARIO_R3
                          && variant != NotificationContentVariant.SCENARIO_R5)
              .toList();
    }
    if (input.missedDayCount() == null) {
      variants =
          variants.stream()
              .filter(variant -> variant != NotificationContentVariant.SCENARIO_R2)
              .toList();
    }
    return variants.isEmpty() ? List.of(NotificationContentVariant.SCENARIO_R0) : variants;
  }

  private static String scenarioTitle(
      NotificationContentVariant variant, NotificationTargetSelectionInput input) {
    return switch (variant) {
      case SCENARIO_A1 -> "오늘만 가능한 시나리오 도착 💌";
      case SCENARIO_A2 -> SCENARIO_R0_TITLE;
      case SCENARIO_A3 -> "오늘 학습 포기하실 건가요? 🥺";
      case SCENARIO_A4 -> "🚨 오늘의 시나리오를 깨면 연속 " + ((long) input.currentStreakDays() + 1) + "일 달성";
      case SCENARIO_R0 -> SCENARIO_R0_TITLE;
      case SCENARIO_R1 -> "공든 탑이 무너지랴";
      case SCENARIO_R2 -> input.missedDayCount() + "일째 " + input.nickname() + "님을 기다리고 있어요…";
      case SCENARIO_R3 -> "포기도 습관이다!";
      case SCENARIO_R4 -> "우리가 마음에 안 드시나요..?";
      case SCENARIO_R5 -> "어라 이상하다 왜 공부하러 안 오지?";
      case SCENARIO_R6 -> "제가 어떻게 해야 공부하러 오실까요?";
      default -> throw new IllegalArgumentException("시나리오 문구가 아닌 변형입니다.");
    };
  }

  private static String scenarioBody(
      NotificationContentVariant variant, NotificationTargetSelectionInput input) {
    return switch (variant) {
      case SCENARIO_A1 -> "자기 전 5분으로 래디에게 열매를 먹여주세요";
      case SCENARIO_A2, SCENARIO_R0 -> SCENARIO_A2_BODY;
      case SCENARIO_A3 -> "5분만 투자하면 열매를 얻을 수 있어요.\n오늘만 할 수 있는 시나리오가 당신을 기다리고 있어요 💌";
      case SCENARIO_A4 -> "5분 투자로 최고 기록을 달성해보세요!";
      case SCENARIO_R1 -> "어제 못했어도 오늘 공부하면 돼요‼️\n자기 전 5분으로 다시 학습을 시작하세요";
      case SCENARIO_R2 -> "배고픈 래디에게 열매를 주세요😭";
      case SCENARIO_R3 -> "하지만 " + input.nickname() + "님은 아직입니다!!\n습관이 되기 전에 영어 공부 5분만 해봐요🥺";
      case SCENARIO_R4 -> "영어 공부를 안 하시는 이유가 궁금해요.";
      case SCENARIO_R5 -> input.nickname() + "님이 이럴 사람이 아닌데…";
      case SCENARIO_R6 -> "제발 5분만 영어 공부해요 우리";
      default -> throw new IllegalArgumentException("시나리오 문구가 아닌 변형입니다.");
    };
  }

  private static ScheduledNotificationContent expressionContent(
      SelectedNotificationTarget target, NotificationTargetSelectionInput input) {
    String targetExpressionText =
        input.expressions().stream()
            .filter(expression -> expression.expressionId().equals(target.targetId()))
            .map(ExpressionNotificationCandidate::targetExpressionText)
            .findFirst()
            .orElse(null);
    String title =
        targetExpressionText == null
                || targetExpressionText.isBlank()
                || containsReservationMarker(targetExpressionText)
            ? null
            : "“" + targetExpressionText + "”, 어떤 상황에서 쓸까요?";
    NotificationContentVariant variant =
        title != null && codePointCount(title) <= 255
            ? NotificationContentVariant.EXPRESSION_DYNAMIC
            : NotificationContentVariant.EXPRESSION_GENERIC;
    return new ScheduledNotificationContent(
        variant,
        variant == NotificationContentVariant.EXPRESSION_DYNAMIC ? title : "표현 학습을 이어가 볼까요?",
        "오늘 시나리오에서 이어지는 표현을 배워보세요.",
        "/expressions/scenario/"
            + target.scenarioId()
            + "/"
            + target.targetId()
            + campaign("continue_expression"));
  }

  private static ScheduledNotificationContent smallTalkContent(
      SelectedNotificationTarget target, NotificationTargetSelectionInput input) {
    String title = "하던 얘기 이어서 해봐요";
    String latestFreeTalkTitle = input.latestFreeTalkTitle();
    String dynamicBody =
        latestFreeTalkTitle != null
                && !latestFreeTalkTitle.isBlank()
                && !containsReservationMarker(latestFreeTalkTitle)
            ? latestFreeTalkTitle + " 이야기, 테디와 조금 더 나눠볼까요?"
            : null;
    boolean dynamic = dynamicBody != null && codePointCount(dynamicBody) <= 500;
    String body = dynamic ? dynamicBody : "테디가 당신과의 대화를 애타게 기다려요.";
    NotificationContentVariant variant =
        dynamic
            ? NotificationContentVariant.SMALL_TALK_DYNAMIC
            : NotificationContentVariant.SMALL_TALK_GENERIC;
    return new ScheduledNotificationContent(
        variant,
        dynamic ? title : "오늘은 스몰톡 안 하시나요? 🥺",
        body,
        "/smalltalk" + campaign("small_talk_reminder"));
  }

  private static boolean containsReservationMarker(String value) {
    return RESERVATION_MARKERS.stream().anyMatch(value::contains);
  }

  private static int codePointCount(String value) {
    return value.codePointCount(0, value.length());
  }

  private static String campaign(String campaign) {
    return "?utm_source=push&utm_medium=notification&utm_campaign=" + campaign;
  }
}
