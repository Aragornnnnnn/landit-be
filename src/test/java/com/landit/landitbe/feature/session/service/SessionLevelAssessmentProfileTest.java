// 수준 변경과 무관한 프로필 수정이 평가 적용을 방해하지 않는지 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.subscription.SubscriptionProperties;
import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.domain.ResponseDemand;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.feature.session.client.ai.AiSessionLevelAssessment;
import com.landit.landitbe.feature.session.domain.LearningLevelPolicy.ChangeType;
import com.landit.landitbe.feature.session.domain.UserLevelAssessment;
import com.landit.landitbe.feature.session.repository.UserLevelAssessmentRepository;
import com.landit.landitbe.shared.domain.AccentLocale;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SessionLevelAssessmentProfileTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  void unrelatedProfileChangeStillInitializesLevel() {
    UserProfile profile = new UserProfile("test@example.com", "test", 1L);
    LocalDateTime requestedAt = LocalDateTime.now(CLOCK).minusSeconds(10);
    profile.updateAccentLocale(AccentLocale.EN_US);
    ReflectionTestUtils.setField(profile, "updatedAt", LocalDateTime.now());
    assertThat(assess(profile, requestedAt, 4, true).getChangeType())
        .isEqualTo(ChangeType.INITIALIZED);
    assertThat(profile.getLearningLevel()).isEqualTo(4);
    assertThat(profile.getLearningLevelUpdatedAt()).isEqualTo(LocalDateTime.now(CLOCK));
  }

  @Test
  void manualLevelChangeRejectsEarlierAssessment() {
    UserProfile profile = new UserProfile("test@example.com", "test", 1L);
    LocalDateTime requestedAt = LocalDateTime.now(CLOCK).minusSeconds(10);
    profile.updateLearningLevel(1, LocalDateTime.now(CLOCK));
    assertThat(assess(profile, requestedAt, 4, true).getChangeType())
        .isEqualTo(ChangeType.NOT_APPLIED);
    assertThat(profile.getLearningLevel()).isEqualTo(1);
    assertThat(profile.getPromotionStreak()).isZero();
  }

  @Test
  void replacesPreviouslySelectedLevelAndRecordsBothValues() {
    UserProfile profile = new UserProfile("test@example.com", "test", 1L);
    profile.updateLearningLevel(4, LocalDateTime.now(CLOCK).minusMinutes(1));
    var result = assess(profile, LocalDateTime.now(CLOCK).minusSeconds(10), 3, true);
    assertThat(result.getChangeType()).isEqualTo(ChangeType.INITIALIZED);
    assertThat(result.getPreviousLevel()).isEqualTo(4);
    assertThat(result.getCurrentLevel()).isEqualTo(3);
    assertThat(result.getAssessedLevel()).isEqualTo(3);
    assertThat(result.getAssessmentVersion()).isEqualTo("text-level-v1.3");
    assertThat(profile.getLearningLevel()).isEqualTo(3);
    assertThat(profile.getPromotionStreak()).isZero();
  }

  @Test
  void nonLatestSessionPreservesProfileButKeepsItsAssessment() {
    UserProfile profile = new UserProfile("test@example.com", "test", 1L);
    profile.updateLearningLevel(4, LocalDateTime.now(CLOCK).minusMinutes(1));
    var result = assess(profile, LocalDateTime.now(CLOCK).minusSeconds(10), 3, false);
    assertThat(result.getChangeType()).isEqualTo(ChangeType.NOT_APPLIED);
    assertThat(result.getAssessedLevel()).isEqualTo(3);
    assertThat(profile.getLearningLevel()).isEqualTo(4);
  }

  @Test
  void initializedLevelDoesNotDecreaseOnLaterAssessment() {
    UserProfile profile = new UserProfile("test@example.com", "test", 1L);
    profile.updateLearningLevel(4, LocalDateTime.now(CLOCK).minusMinutes(1));
    var result = assess(profile, LocalDateTime.now(CLOCK), 2, true, true);
    assertThat(result.getChangeType()).isEqualTo(ChangeType.UNCHANGED);
    assertThat(result.getAssessedLevel()).isEqualTo(2);
    assertThat(profile.getLearningLevel()).isEqualTo(4);
  }

  @Test
  void manuallyLoweredInitializedLevelStillRequiresTwoPromotionSignals() {
    UserProfile profile = new UserProfile("test@example.com", "test", 1L);
    profile.updateLearningLevel(1, LocalDateTime.now(CLOCK).minusMinutes(1));
    assertThat(assess(profile, LocalDateTime.now(CLOCK), 5, true, true).getChangeType())
        .isEqualTo(ChangeType.UNCHANGED);
    assertThat(profile.getLearningLevel()).isEqualTo(1);
    assertThat(profile.getPromotionStreak()).isEqualTo(1);
    assertThat(assess(profile, LocalDateTime.now(CLOCK), 5, true, true).getChangeType())
        .isEqualTo(ChangeType.PROMOTED);
    assertThat(profile.getLearningLevel()).isEqualTo(2);
    assertThat(profile.getPromotionStreak()).isZero();
  }

  private UserLevelAssessment assess(
      UserProfile profile, LocalDateTime requestedAt, int assessedLevel, boolean applyToProfile) {
    return assess(profile, requestedAt, assessedLevel, applyToProfile, false);
  }

  private UserLevelAssessment assess(
      UserProfile profile,
      LocalDateTime requestedAt,
      int assessedLevel,
      boolean applyToProfile,
      boolean levelInitialized) {
    var profiles = mock(UserProfileRepository.class);
    var assessments = mock(UserLevelAssessmentRepository.class);
    when(profiles.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(profile));
    when(assessments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var launch =
        new SessionLevelAssessmentLaunchService(
            new com.landit.landitbe.feature.subscription.service.SubscriptionLaunchPolicyService(
                new SubscriptionProperties("2026-06-01T00:00:00Z"), CLOCK),
            CLOCK);
    when(assessments.existsInitializedLevelSince(1L, launch.requireLaunchedAt()))
        .thenReturn(levelInitialized);
    var context = mock(LoadedSessionFeedbackContext.class);
    when(context.sessionId()).thenReturn(10L);
    when(context.questionLevelGroup()).thenReturn(ContentLearningLevel.DIAGNOSTIC);
    var inputs = List.of(input(1L), input(2L));
    when(context.userMessages()).thenReturn(inputs);
    var domain =
        new AiSessionLevelAssessment.Domain(
            assessedLevel, AiSessionLevelAssessment.EvidenceStatus.OBSERVED, "I like coffee.");
    var domains = new AiSessionLevelAssessment.Domains(domain, domain, domain, domain, domain);
    var core =
        new AiSessionLevelAssessment.Core(
            inputs.stream()
                .map(
                    message ->
                        new AiSessionLevelAssessment.Message(
                            message.messageId(),
                            AiSessionLevelAssessment.TaskPerformance.ACHIEVED,
                            domains))
                .toList());
    return new SessionLevelAssessmentService(profiles, assessments, CLOCK, launch)
        .assessApplyAndSave(
            1L, context, new AiSessionLevelAssessment(core, null), applyToProfile, requestedAt);
  }

  private UserMessageContext input(long id) {
    var input = mock(UserMessageContext.class);
    when(input.messageId()).thenReturn(id);
    when(input.content()).thenReturn("I like coffee.");
    when(input.responseDemand()).thenReturn(ResponseDemand.HIGH);
    return input;
  }
}
