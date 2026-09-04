// AI의 질문별 근거를 검증해 세션 수준을 계산하고 사용자 적용 수준을 갱신한다.

package com.landit.landitbe.feature.session.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.profile.domain.UserProfile;
import com.landit.landitbe.feature.profile.repository.UserProfileRepository;
import com.landit.landitbe.feature.session.client.ai.AiSessionLevelAssessment;
import com.landit.landitbe.feature.session.domain.LearningLevelPolicy;
import com.landit.landitbe.feature.session.domain.SessionLevelAssessment;
import com.landit.landitbe.feature.session.domain.TextLevelAssessmentPolicy;
import com.landit.landitbe.feature.session.domain.TextLevelAssessmentPolicy.Observation;
import com.landit.landitbe.feature.session.domain.UserLevelAssessment;
import com.landit.landitbe.feature.session.repository.UserLevelAssessmentRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 검증 가능한 Core만 모델 점수에 사용하고 나머지는 결정적 fallback으로 처리한다. */
@RequiredArgsConstructor
@Component
class SessionLevelAssessmentService {

  private static final String ASSESSMENT_VERSION = "text-level-v1";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final UserProfileRepository userProfileRepository;
  private final UserLevelAssessmentRepository userLevelAssessmentRepository;

  UserLevelAssessment assessApplyAndSave(
      long userId, LoadedSessionFeedbackContext context, AiSessionLevelAssessment aiAssessment) {
    UserProfile profile =
        userProfileRepository
            .findActiveByIdForUpdate(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
    Integer previousLevel = profile.getLearningLevel();
    TextLevelAssessmentPolicy.Score modelScore = modelScore(context, aiAssessment);
    boolean modelResult = modelScore != null;
    TextLevelAssessmentPolicy.Score score =
        modelResult ? modelScore : fallbackScore(previousLevel == null ? 3 : previousLevel);
    LearningLevelPolicy.Decision decision =
        LearningLevelPolicy.apply(
            previousLevel,
            profile.getPromotionStreak(),
            score.overallScore(),
            score.overallConfidence(),
            modelResult);
    if (modelResult) {
      profile.applyAssessedLearningLevel(decision.level(), decision.promotionStreak());
    }

    AiSessionLevelAssessment.Details details = modelResult ? aiAssessment.details() : null;
    SessionLevelAssessment assessment =
        new SessionLevelAssessment(
            domain(score.situationPerformance()),
            domain(score.grammar()),
            domain(score.vocabulary()),
            domain(score.discourse()),
            domain(score.interactionPragmatics()),
            score.overallScore(),
            score.assessedLevel(),
            modelResult
                ? SessionLevelAssessment.Source.MODEL
                : SessionLevelAssessment.Source.FALLBACK,
            decision.changeType(),
            previousLevel,
            decision.level(),
            validDetails(details)
                ? new SessionLevelAssessment.Details(details.strength(), details.improvement())
                : null,
            ASSESSMENT_VERSION);
    JsonNode corePayload = modelResult ? OBJECT_MAPPER.valueToTree(aiAssessment.core()) : null;
    JsonNode detailsPayload =
        assessment.details() == null ? null : OBJECT_MAPPER.valueToTree(assessment.details());
    return userLevelAssessmentRepository.save(
        UserLevelAssessment.completed(
            userId,
            context.sessionId(),
            assessment,
            decision.promotionStreak(),
            corePayload,
            detailsPayload));
  }

  UserLevelAssessment findBySessionId(long sessionId) {
    return userLevelAssessmentRepository.findByLearningSessionId(sessionId).orElse(null);
  }

  private TextLevelAssessmentPolicy.Score modelScore(
      LoadedSessionFeedbackContext context, AiSessionLevelAssessment assessment) {
    if (assessment == null || assessment.core() == null || assessment.core().messages() == null) {
      return null;
    }
    List<AiSessionLevelAssessment.Message> messages = assessment.core().messages();
    if (messages.size() != context.userMessages().size()) {
      return null;
    }
    List<Observation> observations = new ArrayList<>();
    for (int index = 0; index < messages.size(); index++) {
      UserMessageContext expected = context.userMessages().get(index);
      AiSessionLevelAssessment.Message message = messages.get(index);
      if (message == null
          || !expected.messageId().equals(message.messageId())
          || message.taskPerformance() == null
          || message.domains() == null) {
        return null;
      }
      AiSessionLevelAssessment.Domains domains = message.domains();
      if (!validDomain(domains.situationPerformance(), expected.content())
          || !validDomain(domains.grammar(), expected.content())
          || !validDomain(domains.vocabulary(), expected.content())
          || !validDomain(domains.discourse(), expected.content())
          || !validDomain(domains.interactionPragmatics(), expected.content())) {
        return null;
      }
      observations.add(
          new Observation(
              expected.responseDemand(),
              observedLevel(domains.situationPerformance()),
              observedLevel(domains.grammar()),
              observedLevel(domains.vocabulary()),
              observedLevel(domains.discourse()),
              observedLevel(domains.interactionPragmatics())));
    }
    return TextLevelAssessmentPolicy.calculate(observations, context.questionLevelGroup())
        .orElse(null);
  }

  private boolean validDomain(AiSessionLevelAssessment.Domain domain, String userMessage) {
    if (domain == null || domain.evidenceStatus() == null) {
      return false;
    }
    if (domain.evidenceStatus() != AiSessionLevelAssessment.EvidenceStatus.OBSERVED) {
      return domain.level() == null && domain.evidenceExcerpt() == null;
    }
    return domain.level() != null
        && domain.level() >= 1
        && domain.level() <= 5
        && domain.evidenceExcerpt() != null
        && !domain.evidenceExcerpt().isBlank()
        && userMessage.contains(domain.evidenceExcerpt());
  }

  private Integer observedLevel(AiSessionLevelAssessment.Domain domain) {
    return domain.evidenceStatus() == AiSessionLevelAssessment.EvidenceStatus.OBSERVED
        ? domain.level()
        : null;
  }

  private TextLevelAssessmentPolicy.Score fallbackScore(int level) {
    BigDecimal value = BigDecimal.valueOf(level).setScale(2);
    TextLevelAssessmentPolicy.DomainScore domain =
        new TextLevelAssessmentPolicy.DomainScore(value, new BigDecimal("0.00"));
    return new TextLevelAssessmentPolicy.Score(
        domain, domain, domain, domain, domain, value, BigDecimal.ZERO.setScale(2), level);
  }

  private SessionLevelAssessment.DomainScore domain(TextLevelAssessmentPolicy.DomainScore domain) {
    return new SessionLevelAssessment.DomainScore(domain.score(), domain.confidence());
  }

  private boolean validDetails(AiSessionLevelAssessment.Details details) {
    return details != null
        && details.strength() != null
        && !details.strength().isBlank()
        && details.improvement() != null
        && !details.improvement().isBlank();
  }
}
