// 시나리오 목록 조회 결과를 사용자별 응답 형태로 조립한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.DailyScenarioSchedule;
import com.landit.landitbe.feature.content.domain.ScenarioAvailabilityStatus;
import com.landit.landitbe.feature.content.dto.ScenarioListResponse;
import com.landit.landitbe.feature.content.dto.ScenarioListResponse.CategoryResponse;
import com.landit.landitbe.feature.content.dto.ScenarioListResponse.OpeningPreviewResponse;
import com.landit.landitbe.feature.content.dto.ScenarioListResponse.ScenarioResponse;
import com.landit.landitbe.feature.content.repository.ScenarioListQueryRepository;
import com.landit.landitbe.feature.content.repository.projection.ScenarioListProjection;
import com.landit.landitbe.feature.learning.service.ScenarioAccessService;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 목록 조회 결과를 사용자별 응답 형태로 조립한다. */
@RequiredArgsConstructor
@Service
@Slf4j
public class ScenarioQueryService {

  private static final String CATEGORY_LOCK_REASON = "현재 사용할 수 없는 카테고리입니다.";
  private static final String SCENARIO_LOCK_REASON = "현재 사용할 수 없는 시나리오입니다.";
  private static final String DAILY_SCENARIO_NOT_AVAILABLE = "DAILY_SCENARIO_NOT_AVAILABLE";
  private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final ScenarioListQueryRepository scenarioListQueryRepository;
  private final DailyScenarioScheduleService dailyScenarioScheduleService;
  private final ScenarioAccessService scenarioAccessService;
  private final UserProfileService userProfileService;
  private final Clock clock;

  /** 인증된 사용자의 시나리오 목록 응답을 조회한다. */
  @Transactional(readOnly = true)
  public ScenarioListResponse getScenarioList(long userId) {
    Instant evaluatedAt = clock.instant();
    DailyScenarioScheduleService.TodaySchedule todaySchedule =
        dailyScenarioScheduleService.findAt(evaluatedAt);
    UserLocale userLocale = userProfileService.getUserLocale(userId);
    Set<Long> accessibleScenarioIds =
        Set.copyOf(
            scenarioAccessService.findAccessibleScenarioIds(userId, userLocale.targetLocale()));
    List<ScenarioListProjection> scenarioRows =
        scenarioListQueryRepository.findScenarioList(userId);
    return ScenarioListResponse.from(
        categoryGroups(scenarioRows).stream()
            .map(
                categoryGroup ->
                    categoryGroup.asCategoryResponse(
                        accessibleScenarioIds,
                        todaySchedule
                            .schedule()
                            .map(DailyScenarioSchedule::getScenarioId)
                            .orElse(null),
                        expiresAt(todaySchedule.nextDayStart())))
            .toList());
  }

  /** 평탄한 조회 결과를 응답 구조에 맞게 카테고리 단위로 묶는다. */
  private List<CategoryGroup> categoryGroups(List<ScenarioListProjection> scenarioRows) {
    Map<Long, CategoryGroup> categoryGroupsById = new LinkedHashMap<>();
    for (ScenarioListProjection scenarioRow : scenarioRows) {
      CategoryGroup categoryGroup =
          categoryGroupsById.computeIfAbsent(
              scenarioRow.categoryId(), ignored -> new CategoryGroup(scenarioRow));
      categoryGroup.addScenarioRow(scenarioRow);
    }
    return categoryGroupsById.values().stream().toList();
  }

  /** 조회 row 하나에 접근 상태와 잠금 규칙을 적용해 시나리오 응답으로 조립한다. */
  private static ScenarioResponse toScenarioResponse(
      ScenarioListProjection scenarioRow,
      Set<Long> accessibleScenarioIds,
      Long todayScenarioId,
      OffsetDateTime expiresAt) {
    ScenarioAvailabilityStatus availabilityStatus =
        availabilityStatus(scenarioRow, accessibleScenarioIds, todayScenarioId);
    return ScenarioResponse.from(
        scenarioRow,
        availabilityStatus,
        expiresAt,
        lockReason(scenarioRow, availabilityStatus),
        openingPreview(scenarioRow, availabilityStatus));
  }

  /** 콘텐츠 활성 상태와 접근 권한, 오늘 일정 순으로 시나리오 접근 상태를 계산한다. */
  private static ScenarioAvailabilityStatus availabilityStatus(
      ScenarioListProjection scenarioRow, Set<Long> accessibleScenarioIds, Long todayScenarioId) {
    if (inactive(scenarioRow.categoryStatus())
        || inactive(scenarioRow.scenarioStatus())
        || inactive(scenarioRow.variantStatus())) {
      if (scenarioRow.scenarioId().equals(todayScenarioId)) {
        log.warn(
            "daily scenario schedule references inactive content: scenarioId={}, "
                + "categoryStatus={}, scenarioStatus={}, variantStatus={}",
            scenarioRow.scenarioId(),
            scenarioRow.categoryStatus(),
            scenarioRow.scenarioStatus(),
            scenarioRow.variantStatus());
      }
      return ScenarioAvailabilityStatus.LOCKED;
    }
    if (accessibleScenarioIds.contains(scenarioRow.scenarioId())) {
      return ScenarioAvailabilityStatus.CLEARED;
    }
    if (scenarioRow.scenarioId().equals(todayScenarioId)) {
      return ScenarioAvailabilityStatus.TODAY;
    }
    return ScenarioAvailabilityStatus.LOCKED;
  }

  /** 잠금된 시나리오의 콘텐츠 상태와 일일 접근 사유를 결정한다. */
  private static String lockReason(
      ScenarioListProjection scenarioRow, ScenarioAvailabilityStatus availabilityStatus) {
    if (availabilityStatus != ScenarioAvailabilityStatus.LOCKED) {
      return null;
    }
    if (inactive(scenarioRow.categoryStatus())) {
      return CATEGORY_LOCK_REASON;
    }
    if (inactive(scenarioRow.scenarioStatus()) || inactive(scenarioRow.variantStatus())) {
      return SCENARIO_LOCK_REASON;
    }
    return DAILY_SCENARIO_NOT_AVAILABLE;
  }

  /** 잠기지 않은 시나리오의 첫 화자에 맞춰 시작 화면 미리보기를 조립한다. */
  private static OpeningPreviewResponse openingPreview(
      ScenarioListProjection scenarioRow, ScenarioAvailabilityStatus availabilityStatus) {
    if (availabilityStatus == ScenarioAvailabilityStatus.LOCKED) {
      return null;
    }
    // 첫 발화자가 AI인 경우에만 AI 시작 메시지와 속마음을 미리보기로 내려준다.
    if (scenarioRow.firstSpeaker() == ConversationSpeaker.AI) {
      return OpeningPreviewResponse.fromAi(scenarioRow);
    }
    return OpeningPreviewResponse.fromUser(scenarioRow);
  }

  /** 활성 상태가 아닌 콘텐츠를 잠금 대상으로 판단한다. */
  private static boolean inactive(ActiveStatus status) {
    return status != ActiveStatus.ACTIVE;
  }

  /** 다음 서울 자정을 API 응답용 offset datetime으로 변환한다. */
  private static OffsetDateTime expiresAt(Instant nextDayStart) {
    return nextDayStart.atZone(SERVICE_ZONE_ID).toOffsetDateTime();
  }

  private record CategoryGroup(
      Long categoryId,
      String categoryName,
      int displayOrder,
      boolean categoryLocked,
      String categoryLockReason,
      List<ScenarioListProjection> scenarioRows) {

    /** 카테고리 메타데이터는 같은 카테고리의 첫 조회 결과에서 가져오고, 시나리오는 이후에 누적한다. */
    private CategoryGroup(ScenarioListProjection firstScenarioRow) {
      this(
          firstScenarioRow.categoryId(),
          firstScenarioRow.categoryName(),
          firstScenarioRow.categoryDisplayOrder(),
          inactive(firstScenarioRow.categoryStatus()),
          inactive(firstScenarioRow.categoryStatus()) ? CATEGORY_LOCK_REASON : null,
          new ArrayList<>());
    }

    /** 같은 카테고리에 속한 시나리오 조회 row를 표시 순서대로 누적한다. */
    private void addScenarioRow(ScenarioListProjection scenarioRow) {
      scenarioRows.add(scenarioRow);
    }

    /** 누적한 시나리오에 일일 접근 규칙을 적용해 카테고리 응답을 만든다. */
    private CategoryResponse asCategoryResponse(
        Set<Long> accessibleScenarioIds, Long todayScenarioId, OffsetDateTime expiresAt) {
      List<ScenarioResponse> scenarios = new ArrayList<>();
      for (ScenarioListProjection scenarioRow : scenarioRows) {
        ScenarioResponse scenario =
            toScenarioResponse(scenarioRow, accessibleScenarioIds, todayScenarioId, expiresAt);
        scenarios.add(scenario);
      }
      return CategoryResponse.from(
          categoryId,
          categoryName,
          displayOrder,
          categoryLocked,
          categoryLockReason,
          List.copyOf(scenarios));
    }
  }
}
