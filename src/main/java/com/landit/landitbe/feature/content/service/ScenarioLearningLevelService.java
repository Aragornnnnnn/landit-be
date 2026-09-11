// 신규 학습은 현재 수준으로, 과거 복습은 최초 완료 수준으로 콘텐츠를 선택한다.

package com.landit.landitbe.feature.content.service;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.service.ScenarioSessionService;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시나리오 질문과 표현에 사용할 콘텐츠 수준 정책이다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioLearningLevelService {
  private final ScenarioSessionService scenarioSessionService;
  private final UserProfileService userProfileService;
  private final Clock clock;

  /**
   * 복습 질문은 최초 완료 그룹을, 신규 진단은 공통 그룹을 사용한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 사용할 질문 그룹
   */
  public ContentLearningLevel questionLevel(long userId, long scenarioId) {
    var locale = userProfileService.getUserLocale(userId).targetLocale();
    return scenarioSessionService
        .findFirstCompletedLevel(userId, scenarioId, locale)
        .map(ScenarioSessionService.CompletedScenarioLevel::questionLevelGroup)
        .orElseGet(() -> scenarioId == 1L ? ContentLearningLevel.DIAGNOSTIC : currentLevel(userId));
  }

  /**
   * 오늘 표현은 현재 적용 수준을, 과거 표현은 최초 완료 평가의 수준을 사용한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 사용할 표현 수준 그룹
   */
  public ContentLearningLevel expressionLevel(long userId, long scenarioId) {
    var locale = userProfileService.getUserLocale(userId).targetLocale();
    return scenarioSessionService
        .findFirstCompletedLevel(userId, scenarioId, locale)
        .filter(history -> history.endedAt().toLocalDate().isBefore(LocalDate.now(clock)))
        .map(
            history ->
                history.currentLevel() != null
                    ? ContentLearningLevel.from(history.currentLevel())
                    : history.questionLevelGroup() == ContentLearningLevel.DIAGNOSTIC
                        ? ContentLearningLevel.from(null)
                        : history.questionLevelGroup())
        .orElseGet(() -> currentLevel(userId));
  }

  private ContentLearningLevel currentLevel(long userId) {
    return ContentLearningLevel.from(userProfileService.getLearningLevel(userId).learningLevel());
  }
}
