// 복습 생성과 사용자 잠금 아래 시작·채점·완료를 조율한다.

package com.landit.landitbe.feature.learning.review.service;

import com.landit.landitbe.config.learning.ReviewProperties;
import com.landit.landitbe.feature.content.expression.practice.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.expression.practice.service.ExpressionPracticeService;
import com.landit.landitbe.feature.learning.review.domain.ExpressionReview;
import com.landit.landitbe.feature.learning.review.dto.ReviewOffer;
import com.landit.landitbe.feature.learning.review.dto.ReviewQuestion;
import com.landit.landitbe.feature.learning.review.repository.ExpressionReviewRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.feature.subscription.service.LearningAccessGrantService;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 복습은 다른 학습의 완료 이력이나 보상을 변경하지 않는다. */
@Service
@RequiredArgsConstructor
public class ExpressionReviewService {
  private final ExpressionReviewRepository repository;
  private final ExpressionPracticeService practice;
  private final UserProfileService profiles;
  private final LearningAccessGrantService access;
  private final ReviewProperties properties;
  private final Clock clock;

  /**
   * 완료 이력이 있는 활성 사용자를 배치로 조회한다.
   *
   * @param after 사용자 커서
   * @param size 최대 사용자 수
   * @return 사용자 ID 목록
   */
  @Transactional(readOnly = true)
  public List<Long> candidateUsers(long after, int size) {
    return repository.candidateUsers(after, size);
  }

  /**
   * 발송 시점의 구독·간격을 확인해 복습을 생성하거나 같은 날의 복습을 재사용한다.
   *
   * @param userId 수신 사용자
   * @param date Scheduler 기준 날짜
   * @return 발송 가능한 복습. 구독·간격·콘텐츠 조건을 만족하지 않으면 빈 값
   */
  @Transactional
  public Optional<ReviewOffer> offer(long userId, LocalDate date) {
    profiles.requireActiveForUpdate(userId);
    LocalDateTime now = LocalDateTime.now(clock);
    if (!date.equals(now.toLocalDate())
        || (access.paymentEnabled(userId) && !access.premium(userId))) {
      return Optional.empty();
    }
    Optional<ExpressionReview> existing = repository.findScheduled(userId, date);
    if (existing.isPresent()) {
      ExpressionReview review = existing.get();
      return review.status(now).equals("READY")
          ? Optional.of(
              new ReviewOffer(review.id(), userId, repository.questions(review.id()).size()))
          : Optional.empty();
    }
    if (repository.recentlyOffered(userId, now.minusDays(properties.intervalDays()))) {
      return Optional.empty();
    }
    List<ReviewQuestion> questions =
        selectQuestions(userId, now.minusDays(properties.exclusionDays()));
    if (questions.isEmpty()) {
      return Optional.empty();
    }
    ExpressionReview review =
        new ExpressionReview(
            UUID.randomUUID(),
            userId,
            date,
            now,
            now.plusDays(properties.availableDays()),
            null,
            null,
            null);
    repository.insert(review, questions);
    return Optional.of(new ReviewOffer(review.id(), userId, questions.size()));
  }

  // 기존 콘텐츠의 복수 정답 계약을 유지하며 최대 세 표현을 고정한다.
  private List<ReviewQuestion> selectQuestions(long userId, LocalDateTime cutoff) {
    List<Long> candidates = new ArrayList<>(repository.candidateExpressions(userId, cutoff));
    Collections.shuffle(candidates);
    List<Long> ids = new ArrayList<>();
    List<ExpressionPracticeResponse> content = new ArrayList<>();
    for (Long id : candidates) {
      try {
        content.add(practice.getReviewPracticeExamples(id));
        ids.add(id);
      } catch (ApiException exception) {
        if (exception.getErrorCode() != ErrorCode.RESOURCE_NOT_FOUND) {
          throw exception;
        }
      }
      if (ids.size() == 3) {
        break;
      }
    }
    return assembleQuestions(ids, content);
  }

  private List<ReviewQuestion> assembleQuestions(
      List<Long> ids, List<ExpressionPracticeResponse> content) {
    int size = ids.size();
    int koreanIndex = size == 0 ? -1 : ThreadLocalRandom.current().nextInt(size);
    boolean singleEnglish = size == 1 && ThreadLocalRandom.current().nextBoolean();
    List<ReviewQuestion> questions = new ArrayList<>();
    for (int index = 0; index < size; index++) {
      Locale language = index == koreanIndex && !singleEnglish ? Locale.KR : Locale.EN;
      ExpressionPracticeResponse expression = content.get(index);
      var quiz =
          expression.writingSentence().stream()
              .filter(value -> value.quizLanguage() == language)
              .findFirst()
              .orElseThrow();
      questions.add(
          new ReviewQuestion(
              UUID.randomUUID(),
              ids.get(index),
              expression.targetExpressionText(),
              expression.baseExpressionMeaningText(),
              quiz,
              index,
              index,
              0,
              null));
    }
    return List.copyOf(questions);
  }
}
