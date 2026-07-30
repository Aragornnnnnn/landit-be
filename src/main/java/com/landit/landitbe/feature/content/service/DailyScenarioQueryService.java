// 날짜별 시나리오 조회 정책과 콘텐츠 응답 조립을 담당한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.DailyScenarioType;
import com.landit.landitbe.feature.content.dto.DailyScenarioResponse;
import com.landit.landitbe.feature.content.dto.DailyScenarioResponse.ScenarioResponse;
import com.landit.landitbe.feature.content.repository.DailyScenarioQueryRepository;
import com.landit.landitbe.feature.content.repository.projection.DailyScenarioProjection;
import com.landit.landitbe.feature.learning.service.ScenarioAccessService;
import com.landit.landitbe.feature.learning.service.ScenarioAccessService.ScenarioAccessHistory;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 날짜별 시나리오 조회 정책과 콘텐츠 응답 조립을 담당한다. */
@RequiredArgsConstructor
@Service
public class DailyScenarioQueryService {

  private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final UserProfileService userProfileService;
  private final ScenarioAccessService scenarioAccessService;
  private final ScenarioProgressionService scenarioProgressionService;
  private final DailyScenarioQueryRepository dailyScenarioQueryRepository;
  private final ExpressionQueryService expressionQueryService;
  private final Clock clock;

  /**
   * 사용자의 오늘 배정 시나리오 또는 과거 최초 완료 이력을 조회한다.
   *
   * @param userId 사용자 ID
   * @param date 조회 날짜
   * @return 날짜별 시나리오 조회 응답
   * @throws ApiException 미래 날짜이거나 사용자·시나리오 정보를 찾을 수 없을 때
   */
  @Transactional(readOnly = true)
  public DailyScenarioResponse getDailyScenario(long userId, LocalDate date) {
    Instant evaluatedAt = clock.instant();
    LocalDate today = evaluatedAt.atZone(SERVICE_ZONE_ID).toLocalDate();
    validateNotFuture(date, today);

    UserLocale userLocale = userProfileService.getUserLocale(userId);
    return scenarioAccessService
        .findAccessGrantedOn(userId, userLocale.targetLocale(), date)
        .map(history -> completedResponse(userId, date, history))
        .orElseGet(() -> currentOrEmptyResponse(userId, date, today, userLocale, evaluatedAt));
  }

  /** 미래 날짜는 사용자 완료 여부에 따라 배정이 확정되지 않았으므로 조회를 거절한다. */
  private void validateNotFuture(LocalDate date, LocalDate today) {
    if (date.isAfter(today)) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
  }

  /** 완료 이력이 없는 과거 날짜는 비어 있고, 오늘은 현재 제공 시나리오를 반환한다. */
  private DailyScenarioResponse currentOrEmptyResponse(
      long userId, LocalDate date, LocalDate today, UserLocale userLocale, Instant evaluatedAt) {
    if (date.isBefore(today)) {
      return DailyScenarioResponse.empty(date);
    }
    return scenarioProgressionService
        .findCurrentScenario(userId, userLocale.targetLocale(), evaluatedAt)
        .map(current -> currentResponse(userId, date, current))
        .orElseGet(() -> DailyScenarioResponse.empty(date));
  }

  /** 현재 제공 중인 미완료 시나리오를 응답으로 변환한다. */
  private DailyScenarioResponse currentResponse(
      long userId, LocalDate date, ScenarioProgressionService.CurrentScenario currentScenario) {
    ScenarioResponse scenario =
        scenarioResponse(userId, currentScenario.scenarioId(), currentScenario.type(), false, null);
    return DailyScenarioResponse.playable(date, scenario);
  }

  /** 특정 날짜에 최초 완료한 시나리오를 복습 가능한 응답으로 변환한다. */
  private DailyScenarioResponse completedResponse(
      long userId, LocalDate date, ScenarioAccessHistory accessHistory) {
    ScenarioResponse scenario =
        scenarioResponse(
            userId,
            accessHistory.scenarioId(),
            DailyScenarioType.CLEARED,
            true,
            accessHistory.grantedAt().atZone(SERVICE_ZONE_ID).toOffsetDateTime());
    return DailyScenarioResponse.playable(date, scenario);
  }

  /** 콘텐츠·표현 진행도를 조회해 날짜별 시나리오 상세 응답을 조립한다. */
  private ScenarioResponse scenarioResponse(
      long userId,
      Long scenarioId,
      DailyScenarioType dailyScenarioType,
      boolean completed,
      OffsetDateTime completedAt) {
    DailyScenarioProjection projection =
        dailyScenarioQueryRepository
            .findDailyScenario(userId, scenarioId)
            .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));
    return ScenarioResponse.from(
        projection,
        dailyScenarioType,
        completed,
        completedAt,
        expressionQueryService.getExpressionProgress(userId, scenarioId));
  }
}
