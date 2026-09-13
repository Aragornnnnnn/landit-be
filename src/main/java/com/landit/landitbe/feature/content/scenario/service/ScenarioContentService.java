// 세션 기능에 시나리오 고정 질문 조회 경계를 제공한다.

package com.landit.landitbe.feature.content.scenario.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.scenario.dto.NextQuestionContext;
import com.landit.landitbe.feature.content.scenario.dto.ScenarioStartContext;
import com.landit.landitbe.feature.content.scenario.repository.ScenarioQuestionQueryRepository;
import com.landit.landitbe.feature.content.scenario.repository.ScenarioSessionStartQueryRepository;
import com.landit.landitbe.shared.domain.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 세션 기능에 시나리오 고정 질문 조회 경계를 제공한다. */
@Service
@RequiredArgsConstructor
public class ScenarioContentService {
  private final ScenarioSessionStartQueryRepository startQueryRepository;

  /**
   * 사용자 언어와 수준에 맞는 세션 시작 콘텐츠를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @param questionLevelGroup 질문 레벨 그룹
   * @return 세션 시작에 필요한 콘텐츠
   */
  public Optional<ScenarioStartContext> findStartContext(
      long userId, long scenarioId, ContentLearningLevel questionLevelGroup) {
    return startQueryRepository.findStartRow(userId, scenarioId, questionLevelGroup);
  }

  private final ScenarioQuestionQueryRepository scenarioQuestionQueryRepository;

  /**
   * 시나리오의 활성 고정 질문을 순서와 언어 조합으로 조회한다.
   *
   * @param scenarioId 시나리오 ID
   * @param displayOrder 질문 순서
   * @param questionLevelGroup 질문 레벨 그룹
   * @param targetLocale 학습 대상 locale
   * @param baseLocale 기준 locale
   * @return 조건에 맞는 다음 질문 컨텍스트
   */
  public Optional<NextQuestionContext> findActiveQuestion(
      long scenarioId,
      int displayOrder,
      ContentLearningLevel questionLevelGroup,
      Locale targetLocale,
      Locale baseLocale) {
    return scenarioQuestionQueryRepository
        .findActiveQuestion(scenarioId, displayOrder, questionLevelGroup, targetLocale, baseLocale)
        .map(NextQuestionContext::from);
  }
}
