// 복습 생성과 사용자 잠금 아래 시작·채점·완료를 조율한다.

package com.landit.landitbe.feature.learning.review.service;

import com.landit.landitbe.config.learning.ReviewProperties;
import com.landit.landitbe.feature.content.expression.dto.ExpressionLearningMaterial;
import com.landit.landitbe.feature.content.expression.practice.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.expression.practice.service.ExpressionPracticeService;
import com.landit.landitbe.feature.content.expression.service.ExpressionQueryService;
import com.landit.landitbe.feature.learning.review.domain.ExpressionReview;
import com.landit.landitbe.feature.learning.review.dto.ReviewAnswerRequest;
import com.landit.landitbe.feature.learning.review.dto.ReviewAnswerResponse;
import com.landit.landitbe.feature.learning.review.dto.ReviewOffer;
import com.landit.landitbe.feature.learning.review.dto.ReviewQuestion;
import com.landit.landitbe.feature.learning.review.dto.ReviewResponse;
import com.landit.landitbe.feature.learning.review.exception.ReviewErrorCode;
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
import java.util.Comparator;
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
  private final ExpressionQueryService contentQuery;
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

  /**
   * 복습의 최초 시작 권한을 확인하고 시작 시간을 한 번만 기록한다.
   *
   * @param userId 로그인 사용자
   * @param id 푸시로 전달된 복습 ID
   * @return 같은 문제와 현재 진행 상태
   * @throws ApiException 복습을 소유하지 않거나 만료됐을 때
   */
  @Transactional
  public ReviewResponse start(long userId, UUID id) {
    profiles.requireActiveForUpdate(userId);
    ExpressionReview review = owned(userId, id);
    requireUnexpired(review);
    if (review.startedAt() == null) {
      access.requirePremiumStart(userId);
      LocalDateTime now = LocalDateTime.now(clock);
      repository.start(id, now, now.plusHours(properties.sessionHours()));
      review = owned(userId, id);
    }
    return response(review);
  }

  /**
   * 시작 전에는 메타데이터만, 시작 후에는 고정 문제와 진행을 조회한다.
   *
   * @param userId 로그인 사용자
   * @param id 복습 ID
   * @return 복습 상태. 만료되면 EXPIRED와 빈 문제 목록
   * @throws ApiException 소유한 복습이 없을 때
   */
  @Transactional(readOnly = true)
  public ReviewResponse get(long userId, UUID id) {
    profiles.requireActive(userId);
    ExpressionReview review = owned(userId, id);
    if (review.status(LocalDateTime.now(clock)).equals("READY")) {
      access.requirePremiumStart(userId);
    }
    return response(review);
  }

  /**
   * 현재 문제를 채점하고 모든 문제가 정답 또는 두 번째 오답으로 종료되면 복습을 완료한다.
   *
   * @param userId 로그인 사용자
   * @param id 복습 ID
   * @param request 제출 키와 답안
   * @return 서버 판정과 최신 복습 상태
   * @throws ApiException 만료·시작 전·다른 문제 순서·멱등 키 내용 충돌일 때
   */
  @Transactional
  public ReviewAnswerResponse answer(long userId, UUID id, ReviewAnswerRequest request) {
    profiles.requireActiveForUpdate(userId);
    ExpressionReview review = owned(userId, id);
    var previous = repository.submission(id, request.submissionId());
    if (previous.isPresent()) {
      var saved = previous.get();
      if (!saved.questionId().equals(request.questionId())
          || !saved.words().equals(request.words())) {
        throw new ApiException(ErrorCode.CONFLICT, "같은 제출 키의 내용이 다릅니다.");
      }
      return new ReviewAnswerResponse(saved.correct(), response(review));
    }
    requireUnexpired(review);
    if (review.startedAt() == null) {
      throw new ApiException(ReviewErrorCode.REVIEW_NOT_STARTED);
    }
    List<ReviewQuestion> questions = repository.questions(id);
    ReviewQuestion question =
        current(questions)
            .filter(value -> value.questionId().equals(request.questionId()))
            .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT, "현재 풀 문제와 다릅니다."));
    boolean correct = question.quiz().writingSentenceAcceptedAnswers().contains(request.words());
    repository.answer(id, request, correct, LocalDateTime.now(clock));
    return new ReviewAnswerResponse(correct, response(owned(userId, id)));
  }

  // 기존 콘텐츠의 복수 정답 계약을 유지하며 최대 세 표현을 고정한다.
  private List<ReviewQuestion> selectQuestions(long userId, LocalDateTime cutoff) {
    List<Long> ids = new ArrayList<>();
    List<ExpressionPracticeResponse> content = new ArrayList<>();
    int offset = 0;
    while (ids.size() < 3) {
      List<Long> candidates =
          new ArrayList<>(repository.candidateExpressions(userId, cutoff, offset));
      if (candidates.isEmpty()) {
        break;
      }
      offset += candidates.size();
      Collections.shuffle(candidates);
      selectValidCandidates(candidates, ids, content);
    }
    return assembleQuestions(ids, content);
  }

  private void selectValidCandidates(
      List<Long> candidates, List<Long> ids, List<ExpressionPracticeResponse> content) {
    for (Long id : candidates) {
      try {
        content.add(buildReviewPractice(id));
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
  }

  // 후보 조회에서 완료 이력·언어를 검증했으므로 현재 학습 난이도를 다시 적용하지 않는다.
  private ExpressionPracticeResponse buildReviewPractice(Long expressionId) {
    ExpressionLearningMaterial material =
        contentQuery
            .findLearningMaterial(expressionId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    return practice.buildPracticeResponse(
        expressionId,
        material.detail().targetExpressionText(),
        material.detail().baseExpressionMeaningText(),
        material.detail().usageDescription(),
        material.practiceExamplesPayload());
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

  private ExpressionReview owned(long userId, UUID id) {
    return repository
        .findOwned(userId, id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private void requireUnexpired(ExpressionReview review) {
    if (review.status(LocalDateTime.now(clock)).equals("EXPIRED")) {
      throw new ApiException(ReviewErrorCode.REVIEW_EXPIRED);
    }
  }

  private ReviewResponse response(ExpressionReview review) {
    String status = review.status(LocalDateTime.now(clock));
    List<ReviewQuestion> questions =
        status.equals("READY") || status.equals("EXPIRED")
            ? List.of()
            : repository.questions(review.id());
    return new ReviewResponse(
        review.id(),
        status,
        review.availableUntil(),
        review.expiresAt(),
        review.completedAt(),
        current(questions).map(ReviewQuestion::questionId).orElse(null),
        questions);
  }

  private Optional<ReviewQuestion> current(List<ReviewQuestion> questions) {
    return questions.stream()
        .filter(value -> value.completedAt() == null)
        .min(Comparator.comparingInt(ReviewQuestion::queueOrder));
  }
}
