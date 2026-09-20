// 시나리오 목록 조회 결과를 사용자별 응답 형태로 조립한다.

package com.landit.landitbe.feature.learning.scenario.selection.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.scenario.dto.OpeningPreviewResponse;
import com.landit.landitbe.feature.content.scenario.dto.ScenarioCatalogItem;
import com.landit.landitbe.feature.content.scenario.schedule.dto.ScenarioSummary;
import com.landit.landitbe.feature.content.scenario.service.ScenarioCatalogService;
import com.landit.landitbe.feature.learning.scenario.access.service.ScenarioAccessService;
import com.landit.landitbe.feature.learning.scenario.selection.domain.DailyScenarioType;
import com.landit.landitbe.feature.learning.scenario.selection.domain.ScenarioAvailabilityStatus;
import com.landit.landitbe.feature.learning.scenario.selection.dto.CurrentScenario;
import com.landit.landitbe.feature.learning.scenario.selection.dto.ScenarioListResponse;
import com.landit.landitbe.feature.learning.scenario.selection.dto.ScenarioListResponse.CategoryResponse;
import com.landit.landitbe.feature.learning.scenario.selection.dto.ScenarioListResponse.ScenarioResponse;
import com.landit.landitbe.feature.profile.learning.dto.UserLocale;
import com.landit.landitbe.feature.profile.learning.service.ProfileLearningService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 목록 조회 결과를 사용자별 응답 형태로 조립한다. */
@RequiredArgsConstructor
@Service
public class ScenarioQueryService {

  private static final String CATEGORY_LOCK_REASON = "현재 사용할 수 없는 카테고리입니다.";
  private static final String SCENARIO_LOCK_REASON = "현재 사용할 수 없는 시나리오입니다.";
  private static final String DAILY_SCENARIO_NOT_AVAILABLE = "DAILY_SCENARIO_NOT_AVAILABLE";

  private final ScenarioCatalogService scenarioCatalogService;
  private final CurrentScenarioSelectionService currentScenarioSelectionService;
  private final ScenarioAccessService scenarioAccessService;
  private final ProfileLearningService profileLearningService;
  private final Clock clock;

  /**
   * 인증된 사용자의 시나리오 목록 응답을 조회한다.
   *
   * @param userId 인증된 사용자 ID
   * @return 사용자별 진행 상태가 반영된 시나리오 전체 조회 응답
   */
  @Transactional(readOnly = true)
  public ScenarioListResponse getScenarioList(long userId) {
    // 접근 권한과 오늘 시나리오를 동일한 기준 시각으로 계산한다.
    Instant evaluatedAt = clock.instant();
    UserLocale userLocale = profileLearningService.getUserLocale(userId);
    ContentLearningLevel questionLevelGroup =
        ContentLearningLevel.from(profileLearningService.getLearningLevel(userId).learningLevel());

    Set<Long> accessibleScenarioIds =
        Set.copyOf(
            scenarioAccessService.findAccessibleScenarioIds(userId, userLocale.targetLocale()));
    List<ScenarioCatalogItem> scenarioRows =
        scenarioCatalogService.findCatalog(userId, questionLevelGroup);

    CurrentScenario currentScenario =
        currentScenarioSelectionService
            .findCurrentScenario(userId, userLocale.targetLocale(), evaluatedAt)
            .orElse(null);

    return ScenarioListResponse.from(
        groupByCategory(scenarioRows).stream()
            .map(
                categoryGroup ->
                    categoryGroup.asCategoryResponse(accessibleScenarioIds, currentScenario))
            .toList());
  }

  /**
   * 사용자의 언어 설정에 맞는 시나리오 기본 정보를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 사용자 언어 설정에 맞는 시나리오 기본 정보
   */
  @Transactional(readOnly = true)
  public Optional<ScenarioSummary> findScenarioSummary(long userId, long scenarioId) {
    return scenarioCatalogService
        .findSummary(userId, scenarioId)
        .map(row -> new ScenarioSummary(row.scenarioId(), row.scenarioTitle(), row.displayOrder()));
  }

  /** 평탄한 조회 결과를 응답 구조에 맞게 카테고리 단위로 묶는다. */
  private List<CategoryGroup> groupByCategory(List<ScenarioCatalogItem> scenarioRows) {
    Map<Long, CategoryGroup> categoryGroupsById = new LinkedHashMap<>();
    for (ScenarioCatalogItem scenarioRow : scenarioRows) {
      CategoryGroup categoryGroup =
          categoryGroupsById.computeIfAbsent(
              scenarioRow.categoryId(), ignored -> new CategoryGroup(scenarioRow));
      categoryGroup.addScenarioRow(scenarioRow);
    }

    return categoryGroupsById.values().stream().toList();
  }

  /** 조회 row 하나에 접근 상태와 잠금 규칙을 적용해 시나리오 응답으로 조립한다. */
  private static ScenarioResponse toScenarioResponse(
      ScenarioCatalogItem scenarioRow,
      Set<Long> accessibleScenarioIds,
      CurrentScenario currentScenario) {
    ScenarioAvailabilityStatus availabilityStatus =
        availabilityStatus(scenarioRow, accessibleScenarioIds, currentScenario);

    return ScenarioResponse.from(
        scenarioRow,
        availabilityStatus,
        dailyScenarioType(scenarioRow, currentScenario),
        lockReason(scenarioRow, availabilityStatus),
        openingPreview(scenarioRow, availabilityStatus));
  }

  /** 콘텐츠 활성 상태와 접근 권한, 현재 제공 시나리오 순으로 시나리오 접근 상태를 계산한다. */
  private static ScenarioAvailabilityStatus availabilityStatus(
      ScenarioCatalogItem scenarioRow,
      Set<Long> accessibleScenarioIds,
      CurrentScenario currentScenario) {
    if (inactive(scenarioRow.categoryStatus())
        || inactive(scenarioRow.scenarioStatus())
        || inactive(scenarioRow.variantStatus())) {
      return ScenarioAvailabilityStatus.LOCKED;
    }

    if (accessibleScenarioIds.contains(scenarioRow.scenarioId())) {
      return ScenarioAvailabilityStatus.CLEARED;
    }

    if (currentScenario != null && scenarioRow.scenarioId().equals(currentScenario.scenarioId())) {
      return ScenarioAvailabilityStatus.TODAY;
    }

    return ScenarioAvailabilityStatus.LOCKED;
  }

  /** 오늘 시나리오에만 신규·재도전 구분을 반환한다. */
  private static DailyScenarioType dailyScenarioType(
      ScenarioCatalogItem scenarioRow, CurrentScenario currentScenario) {
    if (currentScenario == null || !scenarioRow.scenarioId().equals(currentScenario.scenarioId())) {
      return null;
    }

    return currentScenario.type();
  }

  /** 잠금된 시나리오의 콘텐츠 상태와 일일 접근 사유를 결정한다. */
  private static String lockReason(
      ScenarioCatalogItem scenarioRow, ScenarioAvailabilityStatus availabilityStatus) {
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
      ScenarioCatalogItem scenarioRow, ScenarioAvailabilityStatus availabilityStatus) {
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

  private record CategoryGroup(
      Long categoryId,
      String categoryName,
      int displayOrder,
      boolean categoryLocked,
      String categoryLockReason,
      List<ScenarioCatalogItem> scenarioRows) {

    /** 카테고리 메타데이터는 같은 카테고리의 첫 조회 결과에서 가져오고, 시나리오는 이후에 누적한다. */
    private CategoryGroup(ScenarioCatalogItem firstScenarioRow) {
      this(
          firstScenarioRow.categoryId(),
          firstScenarioRow.categoryName(),
          firstScenarioRow.categoryDisplayOrder(),
          inactive(firstScenarioRow.categoryStatus()),
          inactive(firstScenarioRow.categoryStatus()) ? CATEGORY_LOCK_REASON : null,
          new ArrayList<>());
    }

    /** 같은 카테고리에 속한 시나리오 조회 row를 표시 순서대로 누적한다. */
    private void addScenarioRow(ScenarioCatalogItem scenarioRow) {
      scenarioRows.add(scenarioRow);
    }

    /** 누적한 시나리오에 일일 접근 규칙을 적용해 카테고리 응답을 만든다. */
    private CategoryResponse asCategoryResponse(
        Set<Long> accessibleScenarioIds, CurrentScenario currentScenario) {
      List<ScenarioResponse> scenarios = new ArrayList<>();
      for (ScenarioCatalogItem scenarioRow : scenarioRows) {
        ScenarioResponse scenario =
            toScenarioResponse(scenarioRow, accessibleScenarioIds, currentScenario);
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
