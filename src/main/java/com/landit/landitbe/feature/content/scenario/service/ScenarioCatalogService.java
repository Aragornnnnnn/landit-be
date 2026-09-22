// 사용자별 학습 정책과 분리된 시나리오 콘텐츠 조회를 제공한다.

package com.landit.landitbe.feature.content.scenario.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.scenario.dto.ScenarioCatalogItem;
import com.landit.landitbe.feature.content.scenario.repository.ScenarioListQueryRepository;
import com.landit.landitbe.feature.content.scenario.schedule.dto.ScenarioDetail;
import com.landit.landitbe.feature.content.scenario.schedule.dto.ScenarioSummary;
import com.landit.landitbe.feature.content.scenario.schedule.dto.ScenarioThumbnail;
import com.landit.landitbe.feature.content.scenario.schedule.dto.ScenarioTitle;
import com.landit.landitbe.feature.content.scenario.schedule.repository.DailyScenarioQueryRepository;
import com.landit.landitbe.feature.content.scenario.schedule.repository.ScenarioSequenceQueryRepository;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자별 학습 정책과 분리된 시나리오 콘텐츠 조회를 제공한다. */
@Service
@RequiredArgsConstructor
public class ScenarioCatalogService {

  private final ScenarioListQueryRepository listRepository;
  private final ScenarioSequenceQueryRepository sequenceRepository;
  private final DailyScenarioQueryRepository detailRepository;

  /**
   * 사용자 언어와 수준에 맞는 카탈로그를 조회한다.
   *
   * @param userId 사용자 ID
   * @param level 질문 수준
   * @return 정렬된 카탈로그 콘텐츠
   */
  @Transactional(readOnly = true)
  public List<ScenarioCatalogItem> findCatalog(long userId, ContentLearningLevel level) {
    return listRepository.findScenarioList(userId, level);
  }

  /**
   * 사용자 언어에 맞는 시나리오 요약을 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 시나리오 요약
   */
  @Transactional(readOnly = true)
  public Optional<ScenarioSummary> findSummary(long userId, long scenarioId) {
    return listRepository
        .findScenarioSummary(userId, scenarioId)
        .map(row -> new ScenarioSummary(row.scenarioId(), row.scenarioTitle(), row.displayOrder()));
  }

  /**
   * 활성 시나리오의 표시 순서를 조회한다.
   *
   * @param userId 사용자 ID
   * @return 표시 순서의 시나리오 ID
   */
  @Transactional(readOnly = true)
  public List<Long> findOrderedScenarioIds(long userId) {
    return sequenceRepository.findScenarioIdsInDisplayOrder(userId);
  }

  /**
   * 완료 이력에 표시할 썸네일을 일괄 조회한다.
   *
   * @param scenarioIds 시나리오 ID 목록
   * @return ID별 썸네일 목록
   */
  @Transactional(readOnly = true)
  public List<ScenarioThumbnail> findThumbnails(List<Long> scenarioIds) {
    return sequenceRepository.findThumbnailsByScenarioIds(scenarioIds);
  }

  /**
   * 지난 기록에 출처로 남길 시나리오 제목을 일괄 조회한다.
   *
   * @param scenarioIds 시나리오 ID 목록
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @return ID별 제목 목록. 그 언어 조합이 없는 시나리오는 빠진다
   */
  @Transactional(readOnly = true)
  public List<ScenarioTitle> findTitles(
      Collection<Long> scenarioIds, Locale targetLocale, Locale baseLocale) {
    if (scenarioIds.isEmpty()) {
      return List.of();
    }
    return sequenceRepository.findTitlesByScenarioIds(scenarioIds, targetLocale, baseLocale);
  }

  /**
   * 사용자 언어와 질문 수준에 맞는 시나리오 상세 콘텐츠를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param level 질문 수준
   * @return 상세 콘텐츠
   */
  @Transactional(readOnly = true)
  public Optional<ScenarioDetail> findDetail(
      long userId, long scenarioId, ContentLearningLevel level) {
    return detailRepository.findDailyScenario(userId, scenarioId, level);
  }
}
