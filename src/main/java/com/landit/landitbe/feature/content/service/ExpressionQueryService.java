// 원어민 표현 조회(시나리오별 목록, 학습 시작 상세, 추가 예문)를 담당한다.

package com.landit.landitbe.feature.content.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.feature.content.domain.FreeTalkGeneratedExpressionContent;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.dto.ExpressionRecommendationCandidate;
import com.landit.landitbe.feature.content.dto.ExpressionResponse;
import com.landit.landitbe.feature.content.dto.ParsedPracticeSentence;
import com.landit.landitbe.feature.content.dto.PracticeSentenceResponse;
import com.landit.landitbe.feature.content.dto.WritingSentenceResponse;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.learning.dto.CompletedExpressionIds;
import com.landit.landitbe.feature.learning.service.LearningProgressService;
import com.landit.landitbe.feature.profile.dto.UserLocale;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 원어민 표현 조회(시나리오별 목록, 학습 시작 상세, 추가 예문)를 담당한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressionQueryService {

  private static final int FREE_TALK_EXPRESSION_CANDIDATE_LIMIT = 100;

  /**
   * 추가 예문 payload의 필수 문자열 키 목록이다.
   *
   * <p>하나라도 없거나 비어 있으면 해당 예문을 응답에서 제외한다.
   */
  private static final List<String> REQUIRED_PRACTICE_SENTENCE_KEYS =
      List.of(
          "sentenceText",
          "highlightingPart",
          "sentenceTranslation",
          "practiceQuestion",
          "practiceQuestionTranslation");

  /**
   * 추가 예문 payload에서 반드시 값이 있어야 하는 작문 단어 배열 키 목록이다.
   *
   * <p>배열이 없거나 비어 있거나 공백 원소가 있으면 해당 예문을 제외한다.
   */
  private static final List<String> REQUIRED_PRACTICE_SENTENCE_WORD_ARRAY_KEYS =
      List.of("sentenceWords", "sentenceWordChoices");

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
  private final LearningProgressService learningProgressService;

  /**
   * 사용자 locale에 맞는 시나리오 표현을 학습 순서대로 조회하고 완료 여부를 반영한다.
   *
   * @param userId 표현 목록을 조회할 사용자 ID
   * @param scenarioId 표현이 속한 시나리오 ID
   * @return 사용자 완료 이력과 잠금 상태를 반영한 표현 목록
   * @throws ApiException 시나리오가 존재하지 않을 때
   */
  @Transactional(readOnly = true)
  public List<ExpressionResponse> getExpressionsPerScenario(Long userId, Long scenarioId) {
    scenarioService.validateExists(scenarioId);

    // 사용자 로케일에 맞는 표현을 로케일별 노출 순서로 조회한다.
    UserLocale userLocale = userProfileService.getUserLocale(userId);
    List<WritingExpression> expressions =
        writingExpressionRepository
            .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
                scenarioId,
                userLocale.targetLocale(),
                userLocale.baseLocale(),
                ActiveStatus.ACTIVE);

    // 해당 유저가 클리어한 Writing 표현의 ID를 Set으로 수집한다.
    CompletedExpressionIds completedExpressionIds =
        learningProgressService.findCompletedExpressionIds(userId, scenarioId);

    // 미완료 표현 중 학습 순서가 가장 앞선 하나만 잠금을 해제한다.
    Optional<Long> firstUnlockedExpressionId =
        firstIncompleteExpressionId(expressions, completedExpressionIds.values());

    return expressions.stream()
        .map(
            expression ->
                responseFor(expression, completedExpressionIds.values(), firstUnlockedExpressionId))
        .toList();
  }

  /**
   * 날짜별 시나리오 화면에 표시할 표현 학습 진행도를 조회한다.
   *
   * @param userId 사용자 ID
   * @param scenarioId 시나리오 ID
   * @return 활성 표현 전체 수와 완료 표현 수
   */
  @Transactional(readOnly = true)
  public ExpressionProgress getExpressionProgress(Long userId, Long scenarioId) {
    UserLocale userLocale = userProfileService.getUserLocale(userId);
    List<WritingExpression> expressions =
        writingExpressionRepository
            .findByScenarioIdAndTargetLocaleAndBaseLocaleAndStatusOrderByDisplayOrderAsc(
                scenarioId,
                userLocale.targetLocale(),
                userLocale.baseLocale(),
                ActiveStatus.ACTIVE);
    Set<Long> completedExpressionIds =
        learningProgressService.findCompletedExpressionIds(userId, scenarioId).values();
    int completedExpressionCount =
        (int)
            expressions.stream()
                .map(WritingExpression::getId)
                .filter(completedExpressionIds::contains)
                .count();
    return new ExpressionProgress(expressions.size(), completedExpressionCount);
  }

  /**
   * 학습할 표현의 상세 정보를 조회한다.
   *
   * <p>표현이 없거나 비활성 상태면 {@link ErrorCode#RESOURCE_NOT_FOUND} 예외를 던진다.
   *
   * @param userId 표현을 조회할 사용자 ID
   * @param expressionId 학습을 시작할 표현 ID
   * @return 학습 화면에 필요한 표현 상세 정보
   * @throws ApiException 표현이 없거나 비활성 상태일 때, 다른 사용자의 전용 표현일 때
   */
  @Transactional(readOnly = true)
  public ExpressionLearningResponse getExpressionForLearning(Long userId, Long expressionId) {
    return ExpressionLearningResponse.from(requireAccessibleExpression(userId, expressionId));
  }

  /**
   * 프리톡 AI가 재사용할 활성 표현 후보를 제한된 개수로 조회한다.
   *
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @return 공용 활성 표현 후보 목록
   */
  @Transactional(readOnly = true)
  public List<ExpressionRecommendationCandidate> getActiveExpressionCandidates(
      Locale targetLocale, Locale baseLocale) {
    return writingExpressionRepository
        .findByTargetLocaleAndBaseLocaleAndStatusAndOwnerUserProfileIdIsNullOrderByIdAsc(
            targetLocale,
            baseLocale,
            ActiveStatus.ACTIVE,
            PageRequest.of(0, FREE_TALK_EXPRESSION_CANDIDATE_LIMIT))
        .stream()
        .map(
            expression ->
                new ExpressionRecommendationCandidate(
                    expression.getId(),
                    expression.getTargetExpressionText(),
                    expression.getBaseExpressionMeaningText(),
                    expression.getUsageSummary()))
        .toList();
  }

  /**
   * 프리톡에서 생성한 사용자 전용 표현을 저장한다.
   *
   * @param userProfileId 표현을 소유할 사용자 ID
   * @param targetLocale 학습 언어 locale
   * @param baseLocale 기준 언어 locale
   * @param content 저장할 생성 표현 콘텐츠
   * @return 저장된 사용자 전용 표현
   */
  @Transactional
  public WritingExpression saveFreeTalkGeneratedExpression(
      Long userProfileId,
      Locale targetLocale,
      Locale baseLocale,
      FreeTalkGeneratedExpressionContent content) {
    return writingExpressionRepository.save(
        WritingExpression.freeTalkGenerated(userProfileId, targetLocale, baseLocale, content));
  }

  /**
   * 프리톡 세션에 연결할 기존 표현이 활성 상태인지 검증한다.
   *
   * @param expressionId 프리톡에 연결할 표현 ID
   * @throws ApiException 표현이 없거나 비활성 상태일 때
   */
  @Transactional(readOnly = true)
  public void validateActiveExpression(Long expressionId) {
    writingExpressionRepository
        .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
        .orElseThrow(() -> new ApiException(ErrorCode.AI_RESPONSE_INVALID));
  }

  /**
   * 학습 중인 표현의 추가 예문 목록과 무작위 작문 문제 한 개를 조회한다.
   *
   * @param userId 표현을 조회할 사용자 ID
   * @param expressionId 연습할 표현 ID
   * @return 추가 예문과 무작위 작문 문제
   * @throws ApiException 표현이 없거나 접근할 수 없거나 유효한 추가 예문이 없을 때
   */
  @Transactional(readOnly = true)
  public ExpressionPracticeResponse getExtraPracticeExamples(Long userId, Long expressionId) {
    WritingExpression expression = requireAccessibleExpression(userId, expressionId);
    return practiceResponse(expression, expressionId);
  }

  // 표현의 유효한 예문을 학습 응답으로 변환한다.
  private ExpressionPracticeResponse practiceResponse(
      WritingExpression expression, Long expressionId) {

    return buildPracticeResponse(
        expressionId,
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText(),
        expression.getUsageDescription(),
        expression.getPracticeExamplesPayload());
  }

  /** 저장 위치와 관계없이 동일한 검증 규칙으로 표현 연습 응답을 만든다. */
  public ExpressionPracticeResponse buildPracticeResponse(
      Long expressionId,
      String targetExpressionText,
      String baseExpressionMeaningText,
      String usageDescription,
      JsonNode practiceExamplesPayload) {
    List<ParsedPracticeSentence> parsedSentences =
        parseExtraPracticeSentences(practiceExamplesPayload, expressionId);
    if (parsedSentences.isEmpty()) {
      log.warn(NO_VALID_PRACTICE_SENTENCE_LOG, expressionId);
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    List<PracticeSentenceResponse> extraPracticeSentences =
        parsedSentences.stream().map(ParsedPracticeSentence::sentence).toList();
    return new ExpressionPracticeResponse(
        targetExpressionText,
        baseExpressionMeaningText,
        usageDescription,
        extraPracticeSentences,
        pickRandomWritingSentence(parsedSentences));
  }

  // 사용자가 접근할 수 있는 활성 표현을 조회한다.
  private WritingExpression requireAccessibleExpression(Long userId, Long expressionId) {
    WritingExpression expression =
        writingExpressionRepository
            .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
            .orElseThrow(
                () -> {
                  log.warn(EXPRESSION_NOT_FOUND_LOG, expressionId);
                  return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
                });
    if (expression.isOwnedByAnother(userId)) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    return expression;
  }

  /**
   * JSONB 배열 payload를 파싱된 예문 목록으로 변환한다.
   *
   * <p>필수 값이 없는 예문은 제외한다. {@code imageUrl}은 없으면 {@code null}로 둔다.
   */
  private List<ParsedPracticeSentence> parseExtraPracticeSentences(
      JsonNode payload, Long expressionId) {
    List<ParsedPracticeSentence> parsedSentences = new ArrayList<>();
    if (payload == null || !payload.isArray()) {
      return parsedSentences;
    }

    for (int index = 0; index < payload.size(); index++) {
      JsonNode node = payload.get(index);
      if (hasMissingRequiredValue(node) || hasInvalidWordArray(node)) {
        log.warn(INVALID_PRACTICE_SENTENCE_EXCLUDED_LOG, expressionId, index);
        continue;
      }

      parsedSentences.add(ParsedPracticeSentence.from(node));
    }
    return parsedSentences;
  }

  /** 예문 노드에 필수 문자열 키가 없거나 값이 비어 있는지 확인한다. */
  private boolean hasMissingRequiredValue(JsonNode node) {
    for (String requiredKey : REQUIRED_PRACTICE_SENTENCE_KEYS) {
      if (!node.hasNonNull(requiredKey) || node.get(requiredKey).asText().isBlank()) {
        return true;
      }
    }
    return false;
  }

  /** 예문의 작문용 단어 배열이 없거나 올바르지 않은지 확인한다. */
  private boolean hasInvalidWordArray(JsonNode node) {
    for (String requiredKey : REQUIRED_PRACTICE_SENTENCE_WORD_ARRAY_KEYS) {
      JsonNode arrayNode = node.get(requiredKey);
      if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
        return true;
      }
      for (JsonNode element : arrayNode) {
        if (!element.isTextual() || element.asText().isBlank()) {
          return true;
        }
      }
    }
    return false;
  }

  /** 예문 목록에서 무작위로 한 개를 골라 작문 연습 문제로 변환한다. */
  private WritingSentenceResponse pickRandomWritingSentence(
      List<ParsedPracticeSentence> parsedSentences) {
    ParsedPracticeSentence picked = parsedSentences.get(random.nextInt(parsedSentences.size()));

    return WritingSentenceResponse.from(picked);
  }

  /** 가장 앞선 미완료 표현의 ID를 반환하며, 모두 완료했으면 빈 값을 반환한다. */
  private Optional<Long> firstIncompleteExpressionId(
      List<WritingExpression> expressions, Set<Long> completedExpressionIds) {
    return expressions.stream()
        .map(WritingExpression::getId)
        .filter(expressionId -> !completedExpressionIds.contains(expressionId))
        .findFirst();
  }

  /** Writing 표현을 완료 여부와 잠김 여부를 계산한 응답으로 변환한다. */
  private ExpressionResponse responseFor(
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

    return ExpressionResponse.from(expression, completed, locked);
  }

  /**
   * 시나리오에 속한 활성 표현 수와 사용자의 완료 표현 수를 담는다.
   *
   * @param expressionCount 활성 표현 수
   * @param completedExpressionCount 완료한 활성 표현 수
   */
  public record ExpressionProgress(int expressionCount, int completedExpressionCount) {}
}
