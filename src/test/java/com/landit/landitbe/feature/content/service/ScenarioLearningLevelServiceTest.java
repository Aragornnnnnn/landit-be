// 승급 후에도 과거 질문과 표현 수준을 보존하고 진단 직후 새 수준을 적용하는지 검증한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.profile.dto.UserLearningLevelResponse;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.session.service.ScenarioSessionService;
import com.landit.landitbe.feature.session.service.ScenarioSessionService.CompletedScenarioLevel;
import com.landit.landitbe.shared.domain.Locale;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ScenarioLearningLevelServiceTest {
  @Test
  void preservesPastGroupsButUsesAssessedLevelForDiagnosticExpressions() {
    var sessions = mock(ScenarioSessionService.class);
    var profiles = mock(UserProfileService.class);
    var clock = Clock.fixed(Instant.parse("2026-09-07T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    var service = new ScenarioLearningLevelService(sessions, profiles, clock);
    when(profiles.getUserLocale(1L)).thenReturn(new UserLocale(Locale.EN, Locale.KR));
    when(profiles.getLearningLevel(1L)).thenReturn(new UserLearningLevelResponse(5));
    when(sessions.findFirstCompletedLevel(1L, 2L, Locale.EN))
        .thenReturn(
            Optional.of(
                new CompletedScenarioLevel(
                    ContentLearningLevel.LEVEL_1, null, LocalDateTime.now(clock).minusDays(1))));
    assertThat(service.questionLevel(1L, 2L)).isEqualTo(ContentLearningLevel.LEVEL_1);
    assertThat(service.expressionLevel(1L, 2L)).isEqualTo(ContentLearningLevel.LEVEL_1);
    when(sessions.findFirstCompletedLevel(1L, 1L, Locale.EN))
        .thenReturn(
            Optional.of(
                new CompletedScenarioLevel(
                    ContentLearningLevel.DIAGNOSTIC, 2, LocalDateTime.now(clock).minusDays(1))));
    assertThat(service.questionLevel(1L, 1L)).isEqualTo(ContentLearningLevel.DIAGNOSTIC);
    assertThat(service.expressionLevel(1L, 1L)).isEqualTo(ContentLearningLevel.LEVEL_2_TO_3);
    when(sessions.findFirstCompletedLevel(1L, 1L, Locale.EN))
        .thenReturn(
            Optional.of(
                new CompletedScenarioLevel(
                    ContentLearningLevel.DIAGNOSTIC, 5, LocalDateTime.now(clock))));
    assertThat(service.expressionLevel(1L, 1L)).isEqualTo(ContentLearningLevel.LEVEL_4_TO_5);
    assertThat(service.questionLevel(1L, 3L)).isEqualTo(ContentLearningLevel.LEVEL_4_TO_5);
  }
}
