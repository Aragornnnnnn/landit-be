// 표현 추가 예문의 검증과 연습·작문 문제 구성을 담당한다.

package com.landit.landitbe.feature.content.expression.practice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.feature.content.expression.practice.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.expression.practice.dto.ParsedPracticeSentence;
import com.landit.landitbe.feature.content.expression.practice.dto.WritingSentenceResponse;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 표현 추가 예문의 검증과 연습·작문 문제 구성을 담당한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressionPracticeService {

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
      List.of(
          "sentenceWords",
          "sentenceWordChoices",
          "sentenceTranslateWords",
          "sentenceTranslateWordChoices");

  /**
   * 추가 예문 조회에 필요한 유효 예문 개수다.
   *
   * <p>payload 순서대로 앞 2건은 눈으로 익히는 예문, 뒤 2건은 직접 푸는 작문 문제로 쓴다. 이보다 적으면 응답을 만들 수 없다.
   */
  private static final int REQUIRED_PRACTICE_SENTENCE_COUNT = 4;

  /** 눈으로 익히는 예문으로 내보낼 개수다. payload의 앞에서부터 이만큼을 쓴다. */
  private static final int PRACTICE_SENTENCE_COUNT = 2;

  private static final String NOT_ENOUGH_PRACTICE_SENTENCE_LOG =
      "추가 예문 조회 실패: 유효한 추가 예문이 {}건뿐입니다. {}건이 필요합니다. expressionId={}";

  private static final String NO_VALID_PRACTICE_SENTENCE_LOG =
      "추가 예문 조회 실패: 표현에 유효한 추가 예문이 없습니다. expressionId={}";
  private static final String INVALID_PRACTICE_SENTENCE_EXCLUDED_LOG =
      "추가 예문 파싱 제외: 필수 값이 누락된 예문입니다. expressionId={}, index={}";
  private final Random random = new Random();

  /**
   * 저장 위치와 관계없이 동일한 검증 규칙으로 표현 연습 응답을 만든다.
   *
   * @param expressionId 로그와 오류 추적에 사용할 표현 ID
   * @param targetExpressionText 학습 언어 표현
   * @param baseExpressionMeaningText 기준 언어 뜻
   * @param usageDescription 상세 용법 설명
   * @param practiceExamplesPayload 추가 예문 JSON 배열
   * @return 유효한 추가 예문과 작문 문제를 포함한 연습 응답
   * @throws ApiException 유효한 추가 예문이 하나도 없을 때
   */
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
    if (parsedSentences.size() < REQUIRED_PRACTICE_SENTENCE_COUNT) {
      // 예문을 나눠 담을 수 없는 상태는 콘텐츠 결함이므로 줄여서 내보내지 않고 드러낸다.
      log.warn(
          NOT_ENOUGH_PRACTICE_SENTENCE_LOG,
          parsedSentences.size(),
          REQUIRED_PRACTICE_SENTENCE_COUNT,
          expressionId);
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // payload 순서를 그대로 따른다. 뒤 2건은 눈으로 익히는 예문, 앞 2건은 작문 문제로 고정한다.
    return new ExpressionPracticeResponse(
        targetExpressionText,
        baseExpressionMeaningText,
        usageDescription,
        parsedSentences.subList(PRACTICE_SENTENCE_COUNT, REQUIRED_PRACTICE_SENTENCE_COUNT).stream()
            .map(ParsedPracticeSentence::sentence)
            .toList(),
        writingSentences(parsedSentences.subList(0, PRACTICE_SENTENCE_COUNT)));
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

  /**
   * 작문 문제로 쓰는 예문 2건에 출제 언어를 하나씩 배정한다.
   *
   * <p>어느 예문이 영어 문제가 될지는 매 요청마다 무작위로 정한다. 난수를 한 번만 뽑아 서로 뒤집어 배정하므로 두 문제의 출제 언어는 항상 서로 다르다.
   */
  private List<WritingSentenceResponse> writingSentences(List<ParsedPracticeSentence> picked) {
    boolean firstIsEnglish = random.nextBoolean();
    return List.of(
        WritingSentenceResponse.from(picked.get(0), firstIsEnglish ? Locale.EN : Locale.KR),
        WritingSentenceResponse.from(picked.get(1), firstIsEnglish ? Locale.KR : Locale.EN));
  }
}
