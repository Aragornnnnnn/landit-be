// 최초 완료한 시나리오의 복습 콘텐츠 수준을 전달한다.

package com.landit.landitbe.feature.content.scenario.dto;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import java.time.LocalDateTime;

/**
 * 최초 완료 시점의 콘텐츠 기준이다.
 *
 * @param questionLevelGroup 당시 질문 그룹
 * @param currentLevel 당시 평가 이후 적용 수준
 * @param endedAt 최초 완료 시각
 */
public record CompletedScenarioLevel(
    ContentLearningLevel questionLevelGroup, Integer currentLevel, LocalDateTime endedAt) {}
