// 사용자별 현재 시나리오와 재도전 여부를 계산한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.DailyScenarioType;
import com.landit.landitbe.feature.content.repository.ScenarioSequenceQueryRepository;
import com.landit.landitbe.feature.learning.service.ScenarioAccessService;
import com.landit.landitbe.shared.domain.Locale;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자별 현재 시나리오와 재도전 여부를 계산한다. */
@RequiredArgsConstructor
@Service
public class ScenarioProgressionService {

  private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final ScenarioSequenceQueryRepository scenarioSequenceQueryRepository;
  private final ScenarioAccessService scenarioAccessService;

  /**
   * 사용자에게 오늘 제공할 시나리오와 제공 유형을 조회한다.
   *
   * @param userId 사용자 ID
   * @param targetLocale 학습 대상 언어
   * @param evaluatedAt 현재 정책 평가 시각
   * @return 오늘 제공할 시나리오. 오늘 새 시나리오를 열 수 없으면 빈 값
   */
  @Transactional(readOnly = true)
  public Optional<CurrentScenario> findCurrentScenario(
      long userId, Locale targetLocale, Instant evaluatedAt) {
    List<Long> scenarioIds = scenarioSequenceQueryRepository.findScenarioIdsInDisplayOrder(userId);
    Set<Long> accessibleScenarioIds =
        Set.copyOf(scenarioAccessService.findAccessibleScenarioIds(userId, targetLocale));
    Optional<Long> firstUnclearedScenarioId =
        scenarioIds.stream()
            .filter(scenarioId -> !accessibleScenarioIds.contains(scenarioId))
            .findFirst();
    if (firstUnclearedScenarioId.isEmpty()
        || scenarioAccessService.hasAccessGrantedOn(
            userId, targetLocale, evaluatedAt.atZone(SERVICE_ZONE_ID).toLocalDate())) {
      return Optional.empty();
    }
    Long scenarioId = firstUnclearedScenarioId.get();
    DailyScenarioType type =
        scenarioAccessService.hasUncompletedSessionBefore(
                userId, scenarioId, targetLocale, evaluatedAt.atZone(SERVICE_ZONE_ID).toLocalDate())
            ? DailyScenarioType.RETRY
            : DailyScenarioType.NEW;
    return Optional.of(new CurrentScenario(scenarioId, type));
  }

  /**
   * 사용자에게 현재 제공 중인 시나리오인지 확인한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 확인할 시나리오 ID
   * @param targetLocale 학습 대상 언어
   * @param evaluatedAt 현재 정책 평가 시각
   * @return 현재 제공 중인 시나리오면 true
   */
  @Transactional(readOnly = true)
  public boolean isCurrentScenario(
      long userId, long scenarioId, Locale targetLocale, Instant evaluatedAt) {
    return findCurrentScenario(userId, targetLocale, evaluatedAt)
        .map(currentScenario -> currentScenario.scenarioId() == scenarioId)
        .orElse(false);
  }

  /**
   * 현재 제공 중인 시나리오의 식별자와 제공 유형을 담는다.
   *
   * @param scenarioId 현재 제공 중인 시나리오 ID
   * @param type 신규 또는 재도전 제공 유형
   */
  public record CurrentScenario(Long scenarioId, DailyScenarioType type) {}
}
