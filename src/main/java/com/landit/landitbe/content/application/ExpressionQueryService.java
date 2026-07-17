// 원어민 표현 조회(시나리오별 목록, 학습 시작 상세, 추가 예문)를 담당한다.

package com.landit.landitbe.content.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.auth.application.UserLocale;
import com.landit.landitbe.auth.application.UserProfileService;
import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.content.api.dto.ExpressionLearningResponse;
import com.landit.landitbe.content.api.dto.ExpressionPracticeResponse;
import com.landit.landitbe.content.api.dto.ExpressionResponse;
import com.landit.landitbe.content.api.dto.PracticeSentenceResponse;
import com.landit.landitbe.content.api.dto.WritingSentenceResponse;
import com.landit.landitbe.content.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.content.domain.WritingExpression;
import com.landit.landitbe.content.infrastructure.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.content.infrastructure.WritingExpressionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 원어민 표현 조회(시나리오별 목록, 학습 시작 상세, 추가 예문)를 담당한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressionQueryService {

  /**
   * 추가 예문 payload (practice_examples_payload)에서 반드시 값이 있어야 하는 키 목록. 하나라도 없거나 비어 있으면 그 예문은 응답에서
   * 제외한다.
   */
  private static final List<String> REQUIRED_PRACTICE_SENTENCE_KEYS =
      List.of(
          "sentenceText",
          "highlightingPart",
          "sentenceTranslation",
          "practiceQuestion",
          "practiceQuestionTranslation");

  private static final String EXPRESSION_NOT_FOUND_LOG =
      "추가 예문 조회 실패: 존재하지 않거나 비활성화된 표현입니다. expressionId={}";
  private static final String NO_VALID_PRACTICE_SENTENCE_LOG =
      "추가 예문 조회 실패: 표현에 유효한 추가 예문이 없습니다. expressionId={}";
  private static final String INVALID_PRACTICE_SENTENCE_EXCLUDED_LOG =
      "추가 예문 파싱 제외: 필수 값이 누락된 예문입니다. expressionId={}, index={}";

  private final Random random = new Random();

  private final ScenarioService scenarioService;
  private final UserProfileService userProfileService;
  private final WritingExpressionRepository writingExpressionRepository;
  private final UserWritingExpressionCompletionRepository userWritingExpressionCompletionRepository;

  /** 시나리오별 Writing 표현 목록을 사용자 locale 기준 학습 순서대로 조회하고 완료 여부를 반영한다. */
  @Transactional(readOnly = true)
  public List<ExpressionResponse> getExpressionsPerScenario(Long userId, Long scenarioId) {
    scenarioService.validateExists(scenarioId);

    // 사용자 프로필의 학습 locale 기준으로 조회한다. (locale 조합별로 displayOrder 시퀀스가 따로 존재)
    UserLocale userLocale = userProfileService.getUserLocale(userId);
    List<WritingExpression> expressions =
        writingExpressionRepository
            .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
                scenarioId,
                userLocale.targetLocale(),
                userLocale.baseLocale(),
                ActiveStatus.ACTIVE);

    // 해당 유저가 클리어한 Writing 표현의 ID를 Set으로 수집한다.
    Set<Long> completedExpressionIds =
        userWritingExpressionCompletionRepository
            .findAllByUserProfileIdAndScenarioId(userId, scenarioId)
            .stream()
            .map(UserWritingExpressionCompletion::getWritingExpressionId)
            .collect(Collectors.toSet());

    // 미완료 표현 중 학습 순서가 가장 앞선 하나만 해금되고 그 뒤로는 잠긴다. (리스트는 displayOrder 오름차순)
    Optional<Long> firstUnlockedExpressionId =
        firstIncompleteExpressionId(expressions, completedExpressionIds);

    return expressions.stream()
        .map(
            expression -> toResponse(expression, completedExpressionIds, firstUnlockedExpressionId))
        .toList();
  }

  /** 학습을 시작할 표현의 상세 정보를 조회한다. 표현이 없거나 INACTIVE(내려간 콘텐츠)면 RESOURCE_NOT_FOUND 예외를 던진다. */
  @Transactional(readOnly = true)
  public ExpressionLearningResponse getExpressionForLearning(Long expressionId) {
    WritingExpression expression =
        writingExpressionRepository
            .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

    return toLearningResponse(expression);
  }

  /**
   * 학습 중인 표현의 추가 예문 목록과 작문 문제(랜덤 1개)를 조회한다. 표현이 없거나 INACTIVE거나 예문이 비어 있으면 RESOURCE_NOT_FOUND 예외를
   * 던진다.
   */
  @Transactional(readOnly = true)
  public ExpressionPracticeResponse getExtraPracticeExamples(Long expressionId) {
    WritingExpression expression =
        writingExpressionRepository
            .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
            .orElseThrow(
                () -> {
                  log.warn(EXPRESSION_NOT_FOUND_LOG, expressionId);
                  return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
                });

    List<PracticeSentenceResponse> extraPracticeSentences =
        parseExtraPracticeSentences(expression.getPracticeExamplesPayload(), expressionId);
    if (extraPracticeSentences.isEmpty()) {
      log.warn(NO_VALID_PRACTICE_SENTENCE_LOG, expressionId);
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    return new ExpressionPracticeResponse(
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText(),
        expression.getUsageDescription(),
        extraPracticeSentences,
        pickRandomWritingSentence(extraPracticeSentences));
  }

  /**
   * JSONB payload(JSON 배열)를 예문 응답 목록으로 파싱한다. 필수 키가 없거나 값이 빈 불량 예문은 빈 값으로 노출하는 대신 목록에서 제외하고 경고 로그를
   * 남긴다. imageUrl은 유일한 선택 필드라 없으면 null로 둔다.
   */
  private List<PracticeSentenceResponse> parseExtraPracticeSentences(
      JsonNode payload, Long expressionId) {
    List<PracticeSentenceResponse> extraPracticeSentences = new ArrayList<>();
    if (payload == null || !payload.isArray()) {
      return extraPracticeSentences;
    }

    for (int index = 0; index < payload.size(); index++) {
      JsonNode node = payload.get(index);
      if (hasMissingRequiredValue(node)) {
        log.warn(INVALID_PRACTICE_SENTENCE_EXCLUDED_LOG, expressionId, index);
        continue;
      }

      extraPracticeSentences.add(
          new PracticeSentenceResponse(
              node.get("sentenceText").asText(),
              node.get("highlightingPart").asText(),
              node.get("sentenceTranslation").asText(),
              node.get("practiceQuestion").asText(),
              node.get("practiceQuestionTranslation").asText(),
              node.hasNonNull("imageUrl") ? node.get("imageUrl").asText() : null));
    }
    return extraPracticeSentences;
  }

  /** 예문 노드에 필수 키가 없거나 값이 비어 있는지 확인한다. */
  private boolean hasMissingRequiredValue(JsonNode node) {
    for (String requiredKey : REQUIRED_PRACTICE_SENTENCE_KEYS) {
      if (!node.hasNonNull(requiredKey) || node.get(requiredKey).asText().isBlank()) {
        return true;
      }
    }
    return false;
  }

  /** 예문 목록에서 랜덤으로 1개를 골라 작문 연습 문제로 변환한다. (인덱스 범위: 0 ~ 목록 길이-1) */
  private WritingSentenceResponse pickRandomWritingSentence(
      List<PracticeSentenceResponse> extraPracticeSentences) {
    PracticeSentenceResponse picked =
        extraPracticeSentences.get(random.nextInt(extraPracticeSentences.size()));

    return new WritingSentenceResponse(
        picked.sentenceText(),
        picked.sentenceTranslation(),
        picked.practiceQuestion(),
        picked.practiceQuestionTranslation());
  }

  /** 미완료 표현 중 학습 순서가 가장 앞선 표현의 ID를 반환한다. 모두 완료했으면 빈 값을 반환한다. */
  private Optional<Long> firstIncompleteExpressionId(
      List<WritingExpression> expressions, Set<Long> completedExpressionIds) {
    return expressions.stream()
        .map(WritingExpression::getId)
        .filter(expressionId -> !completedExpressionIds.contains(expressionId))
        .findFirst();
  }

  /** Writing 표현을 완료 여부와 잠김 여부를 계산한 응답으로 변환한다. */
  private ExpressionResponse toResponse(
      WritingExpression expression,
      Set<Long> completedExpressionIds,
      Optional<Long> firstUnlockedExpressionId) {
    // 미완료 표현 중 학습 순서가 가장 앞선(=지금 학습할 차례인) 표현인지 확인한다.
    // firstUnlockedExpressionId가 비어 있으면(모두 완료) 해금 대상 표현이 없으므로 false다.
    boolean isFirstUnlockedExpression =
        firstUnlockedExpressionId.isPresent()
            && firstUnlockedExpressionId.get().equals(expression.getId());

    // 완료했거나 지금 학습할 차례인 표현만 잠기지 않고, 나머지 미완료 표현은 잠긴다.
    boolean completed = completedExpressionIds.contains(expression.getId());
    boolean locked = !completed && !isFirstUnlockedExpression;

    return new ExpressionResponse(
        expression.getId(),
        expression.getDisplayOrder(),
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText(),
        completed,
        locked);
  }

  /** Writing 표현 엔티티를 학습 시작 응답 DTO로 변환한다. */
  private ExpressionLearningResponse toLearningResponse(WritingExpression expression) {
    return new ExpressionLearningResponse(
        expression.getId(),
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText(),
        expression.getUsageDescription(),
        expression.getRepresentativeQuestionText(),
        expression.getRepresentativeQuestionTranslation(),
        expression.getRepresentativeSentenceText(),
        expression.getRepresentativeSentenceTranslation(),
        expression.getRepresentativeSentenceWords(),
        expression.getRepresentativeSentenceWordChoices(),
        expression.getRepresentativeImageUrl());
  }
}
