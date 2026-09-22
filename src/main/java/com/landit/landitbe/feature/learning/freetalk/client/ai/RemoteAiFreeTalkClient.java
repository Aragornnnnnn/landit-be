// 원격 AI 서버의 프리톡 생성 API를 호출한다.

package com.landit.landitbe.feature.learning.freetalk.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.learning.conversation.domain.CharacterEmotion;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiConversationEmbeddingsRequest;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiConversationEmbeddingsResult;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiConversationExcerpt;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExistingExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendation;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsResult;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkUsedExpression;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkPatternUsageDraft;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingRequest;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkOpeningRequest;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.shared.client.ai.AiHttpClient;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** 원격 AI 호출 계약을 구현한다. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "landit.ai", name = "client-mode", havingValue = "remote")
public class RemoteAiFreeTalkClient implements AiFreeTalkClient {
  private final AiHttpClient http;

  /**
   * JSON 변환기와 AI 서버 설정으로 원격 프리톡 클라이언트를 구성한다.
   *
   * @param jsonMapper AI 요청과 응답 JSON 변환기
   * @param properties AI 서버 연결 설정
   */
  public RemoteAiFreeTalkClient(JsonMapper jsonMapper, AiClientProperties properties) {
    this.http = new AiHttpClient(jsonMapper, properties);
  }

  private static final String OPENING_PATH = "/api/v1/free-talk/opening";
  private static final String TURN_PATH = "/api/v1/free-talk/turn";
  private static final String INNER_THOUGHT_PATH = "/api/v1/free-talk/inner-thought";
  private static final String CLOSING_PATH = "/api/v1/free-talk/closing";
  private static final String EXPRESSION_RECOMMENDATIONS_PATH =
      "/api/v1/free-talk/expression-recommendations";
  private static final String CONVERSATION_EMBEDDINGS_PATH =
      "/api/v1/free-talk/conversation-embeddings";
  private static final int MAX_CONVERSATION_EXCERPTS = 4;

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkOpeningResult generateOpening(AiFreeTalkOpeningRequest request) {
    return http.post(OPENING_PATH, request, RemoteOpeningResponse.class)
        .toResult(request.memoryContext());
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkTurnResult generateTurn(AiFreeTalkTurnRequest request) {
    return http.post(TURN_PATH, request, RemoteTurnResponse.class)
        .toResult(request.memoryContext());
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkInnerThoughtResult generateInnerThought(AiFreeTalkInnerThoughtRequest request) {
    return http.post(INNER_THOUGHT_PATH, request, RemoteInnerThoughtResponse.class)
        .toResult(request);
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request) {
    return http.post(CLOSING_PATH, request, RemoteClosingResponse.class).toResult();
  }

  /**
   * 프리톡 대화에서 학습할 표현을 추천한다.
   *
   * @param request 표현 추천 요청
   * @return 검증된 표현 추천 결과
   * @throws ApiException 원격 AI 호출 또는 응답 검증에 실패했을 때
   */
  @Override
  public AiFreeTalkExpressionRecommendationsResult recommendExpressions(
      AiFreeTalkExpressionRecommendationsRequest request) {
    return http.post(
            EXPRESSION_RECOMMENDATIONS_PATH, request, RemoteExpressionRecommendationsResponse.class)
        .toResult(request);
  }

  /**
   * 완료된 프리톡 대화에서 핵심 사용자 발화를 추출하고 임베딩한다.
   *
   * @param request 대화 임베딩 요청
   * @return 검증된 핵심 발화와 임베딩 목록
   * @throws ApiException 원격 AI 호출 또는 응답 검증에 실패했을 때
   */
  @Override
  public AiConversationEmbeddingsResult extractConversationEmbeddings(
      AiConversationEmbeddingsRequest request) {
    return http.post(
            CONVERSATION_EMBEDDINGS_PATH, request, RemoteConversationEmbeddingsResponse.class)
        .toResult();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteOpeningResponse(
      String aiMessage,
      String translatedMessage,
      CharacterEmotion emotion,
      List<Long> usedMemoryIds) {

    /** 원격 첫 발화와 memory 사용 ID를 검증해 애플리케이션 결과로 변환한다. */
    private AiFreeTalkOpeningResult toResult(List<AiFreeTalkMemoryContext> memoryContext) {
      if (blank(aiMessage) || blank(translatedMessage)) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkOpeningResult(
          aiMessage, translatedMessage, emotion, validUsedMemoryIds(usedMemoryIds, memoryContext));
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteTurnResponse(
      Boolean userExitIntentDetected,
      String inferredTitle,
      String aiMessage,
      String translatedMessage,
      CharacterEmotion emotion,
      List<Long> usedMemoryIds) {

    /** 원격 후속 발화의 종료·생성 필드와 memory 사용 ID를 함께 검증한다. */
    private AiFreeTalkTurnResult toResult(List<AiFreeTalkMemoryContext> memoryContext) {
      if (userExitIntentDetected == null
          || (!userExitIntentDetected && hasMissingGeneratedField())) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      if ((userExitIntentDetected && hasGeneratedField())
          || (inferredTitle != null && blank(inferredTitle))) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkTurnResult(
          userExitIntentDetected,
          inferredTitle,
          aiMessage,
          translatedMessage,
          emotion,
          validUsedMemoryIds(usedMemoryIds, memoryContext));
    }

    private boolean hasMissingGeneratedField() {
      return blank(aiMessage) || blank(translatedMessage);
    }

    private boolean hasGeneratedField() {
      return aiMessage != null || translatedMessage != null || emotion != null;
    }
  }

  /** AI가 반환한 장기기억 식별자가 제공된 문맥의 유효한 부분집합인지 확인한다. */
  private static List<Long> validUsedMemoryIds(
      List<Long> usedMemoryIds, List<AiFreeTalkMemoryContext> memoryContext) {
    List<Long> normalized = usedMemoryIds == null ? List.of() : usedMemoryIds;
    if (hasInvalidUsedMemoryIds(normalized) || !isMemorySubset(normalized, memoryContext)) {
      return List.of();
    }
    return List.copyOf(normalized);
  }

  private static boolean hasInvalidUsedMemoryIds(List<Long> usedMemoryIds) {
    return usedMemoryIds.stream().anyMatch(id -> id == null || id <= 0)
        || usedMemoryIds.size() != usedMemoryIds.stream().distinct().count();
  }

  private static boolean isMemorySubset(
      List<Long> usedMemoryIds, List<AiFreeTalkMemoryContext> memoryContext) {
    return memoryContext != null
        && memoryContext.stream()
            .map(AiFreeTalkMemoryContext::memoryId)
            .collect(java.util.stream.Collectors.toSet())
            .containsAll(usedMemoryIds);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteInnerThoughtResponse(
      String innerThought,
      InnerThoughtType innerThoughtType,
      Boolean reactedToPartner,
      RemoteCorrection correction,
      List<RemotePatternUsage> patternUsages) {

    // 원격 속마음 응답을 검증해 애플리케이션 결과로 변환한다.
    private AiFreeTalkInnerThoughtResult toResult(AiFreeTalkInnerThoughtRequest request) {
      if (blank(innerThought) || innerThoughtType == null) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkInnerThoughtResult(
          innerThought, innerThoughtType, turnCorrection(request));
    }

    // 교정은 보조 판정이라 계약 위반이어도 속마음은 살리고 교정만 실패로 남긴다. 임의 값으로 채우지 않는다.
    private FreeTalkTurnCorrection turnCorrection(AiFreeTalkInnerThoughtRequest request) {
      long messageId = request.submittedMessageId();
      if (reactedToPartner == null) {
        // 교정 판정이 없는 응답에 사용례가 실려 오는 것은 계약 밖이다. 같이 버리되 흔적은 남긴다.
        if (patternUsages != null && !patternUsages.isEmpty()) {
          log.warn(
              "프리톡 실수 패턴 사용례가 교정 판정 없이 실려 와 버립니다. "
                  + "workflow=free_talk_pattern_usage_invalid reason=usages_without_judgment"
                  + " messageId={} count={}",
              messageId,
              patternUsages.size());
        }
        // 둘 다 없으면 AI 서버가 교정 판정을 돌려주지 못한 것(교정 호출의 타임아웃·일시 장애)이라 다시 해 볼 수 있다.
        return correction == null
            ? FreeTalkTurnCorrection.unavailable()
            : invalidCorrection(messageId, "correction_without_reaction");
      }
      // 사용례는 교정과 별개의 부가 판정이라 고칠 것이 없는 턴에도 온다. 교정이 계약 위반이면 같은 판정을 믿지 않고 함께 버린다.
      if (correction == null) {
        return FreeTalkTurnCorrection.completed(
            null, reactedToPartner, validPatternUsages(request));
      }
      String invalidReason = correction.invalidReason();
      if (invalidReason != null) {
        return invalidCorrection(messageId, invalidReason);
      }
      return FreeTalkTurnCorrection.completed(
          correction.toSentence(messageId, request.memoryContext()),
          reactedToPartner,
          validPatternUsages(request));
    }

    // 사용례는 항목 단위로 다시 확인해 맞지 않는 것만 버린다. 보낸 지켜볼 패턴 밖의 패턴, 원문에 없는 문장, 문장에 없는 구절은 믿지 않는다.
    private List<FreeTalkPatternUsageDraft> validPatternUsages(
        AiFreeTalkInnerThoughtRequest request) {
      if (patternUsages == null || patternUsages.isEmpty()) {
        return List.of();
      }
      String content = request.submittedContent();
      List<FreeTalkPatternUsageDraft> valid = new java.util.ArrayList<>();
      for (RemotePatternUsage usage : patternUsages) {
        String reason =
            usage == null ? "blank" : usage.invalidReason(request.watchPatterns(), content);
        if (reason != null) {
          log.warn(
              "프리톡 실수 패턴 사용례가 계약과 달라 해당 항목만 버립니다. "
                  + "workflow=free_talk_pattern_usage_invalid reason={} messageId={}",
              reason,
              request.submittedMessageId());
          continue;
        }
        valid.add(usage.toDraft());
      }
      return valid;
    }

    private static FreeTalkTurnCorrection invalidCorrection(long messageId, String reason) {
      log.warn(
          "프리톡 턴 교정 응답이 계약과 달라 교정을 실패로 기록합니다. "
              + "workflow=free_talk_turn_correction_invalid reason={} messageId={}",
          reason,
          messageId);
      return FreeTalkTurnCorrection.failed();
    }
  }

  // 지켜보던 실수 패턴이 이번 턴에 등장한 사용례 하나. 패턴은 문자열로 받아 모르는 값이 역직렬화를 실패시키지 않게 한다.
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemotePatternUsage(String pattern, String sentence, String span, Boolean correct) {

    // 계약을 어긴 이유를 로그용 코드로 돌려준다. 문제가 없으면 null이다.
    private String invalidReason(List<FreeTalkMistakePattern> watchPatterns, String content) {
      if (blank(sentence) || blank(span) || correct == null) {
        return "blank";
      }
      FreeTalkMistakePattern known = knownPattern(pattern);
      if (known == null || !watchPatterns.contains(known)) {
        return "unknown_pattern";
      }
      if (content == null || !content.contains(sentence.strip())) {
        return "sentence_not_in_message";
      }
      // 화면이 구절을 대소문자까지 그대로 찾아 강조하므로 대소문자를 무시하지 않고, 자리가 하나로 정해져야 하므로 정확히 한 번 나와야 한다.
      return spanRejection(sentence.strip(), span.strip(), "span");
    }

    private FreeTalkPatternUsageDraft toDraft() {
      return new FreeTalkPatternUsageDraft(
          knownPattern(pattern), sentence.strip(), span.strip(), correct);
    }
  }

  // 구절이 문장에 대소문자까지 그대로 정확히 한 번 나오지 않으면 그 이유를, 나오면 null을 돌려준다.
  private static String spanRejection(String sentence, String span, String field) {
    int first = sentence.indexOf(span);
    if (first < 0) {
      return field + "_not_in_sentence";
    }
    return sentence.indexOf(span, first + 1) < 0 ? null : field + "_not_unique";
  }

  private static FreeTalkMistakePattern knownPattern(String pattern) {
    if (pattern == null) {
      return null;
    }
    try {
      return FreeTalkMistakePattern.valueOf(pattern);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  // 실수 패턴은 문자열로 받아 모르는 값이 속마음 응답 전체의 역직렬화를 실패시키지 않게 한다.
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteCorrection(
      String originalSentence,
      String betterSentence,
      String reason,
      String mistakePattern,
      Long usedMemoryId,
      String memoryLabel,
      String wrongSpan,
      String betterSpan) {
    private static final int MAX_MEMORY_LABEL_LENGTH = 40;

    // 계약을 어긴 이유를 로그용 코드로 돌려준다. 문제가 없으면 null이다.
    private String invalidReason() {
      if (blank(originalSentence) || blank(betterSentence) || blank(reason)) {
        return "blank_correction_text";
      }
      return knownMistakePattern() == null ? "unknown_mistake_pattern" : null;
    }

    private FreeTalkTurnCorrection.Sentence toSentence(
        long messageId, List<AiFreeTalkMemoryContext> memoryContext) {
      AiFreeTalkMemoryContext memory = providedMemory(messageId, memoryContext);
      String validWrongSpan = validSpan(messageId, wrongSpan, originalSentence, "wrong_span");
      String validBetterSpan = validSpan(messageId, betterSpan, betterSentence, "better_span");
      if (memory == null) {
        return new FreeTalkTurnCorrection.Sentence(
            originalSentence,
            betterSentence,
            reason,
            knownMistakePattern(),
            validWrongSpan,
            validBetterSpan);
      }
      // 지난 기록이 바뀌지 않도록 기억을 말한 날짜를 지금 보낸 문맥에서 꺼내 교정과 함께 남긴다.
      return new FreeTalkTurnCorrection.Sentence(
          originalSentence,
          betterSentence,
          reason,
          knownMistakePattern(),
          memory.memoryId(),
          memory.observedAt().toLocalDate(),
          validMemoryLabel(messageId),
          validWrongSpan,
          validBetterSpan);
    }

    // 강조 구절은 부가 정보라 문장에 대소문자까지 그대로 정확히 한 번 나오지 않으면 그 구절만 버린다. AI가 구절을 주지 않은 것(null)은 정상이라 조용히
    // null이다.
    private static String validSpan(long messageId, String span, String sentence, String field) {
      if (span == null) {
        return null;
      }
      String stripped = span.strip();
      // 문장은 DB의 원문이거나 같은 응답의 교정문이라, 문장에 없는 제어문자가 구절에 있으면 여기서 함께 걸러진다.
      String reason =
          stripped.isEmpty() ? field + "_blank" : spanRejection(sentence, stripped, field);
      if (reason != null) {
        log.warn(
            "프리톡 턴 교정의 강조 구절이 계약과 달라 해당 값만 버립니다. "
                + "workflow=free_talk_turn_correction_span reason={} messageId={}",
            reason,
            messageId);
        return null;
      }
      return stripped;
    }

    // 근거 기억은 부가 정보라 계약과 달라도 교정 문장은 살리고 해당 값만 버린다.
    private AiFreeTalkMemoryContext providedMemory(
        long messageId, List<AiFreeTalkMemoryContext> memoryContext) {
      if (usedMemoryId == null) {
        if (!blank(memoryLabel)) {
          droppedCorrectionMemory(messageId, "label_without_memory");
        }
        return null;
      }
      AiFreeTalkMemoryContext memory =
          memoryContext == null
              ? null
              : memoryContext.stream()
                  .filter(context -> usedMemoryId.equals(context.memoryId()))
                  .findFirst()
                  .orElse(null);
      if (memory == null) {
        droppedCorrectionMemory(messageId, "unknown_memory_id");
        return null;
      }
      if (memory.observedAt() == null) {
        droppedCorrectionMemory(messageId, "memory_without_observed_at");
        return null;
      }
      return memory;
    }

    // 라벨이 없으면 조회할 때 기본 문구를 쓰므로 임의 값으로 채우지 않고 null로 남긴다.
    private String validMemoryLabel(long messageId) {
      if (blank(memoryLabel)) {
        droppedCorrectionMemory(messageId, "label_missing");
        return null;
      }
      String label = memoryLabel.strip();
      // 줄바꿈뿐 아니라 NUL 같은 제어문자도 DB가 거부해 속마음 저장까지 실패시키므로 여기서 거른다.
      if (label.codePointCount(0, label.length()) > MAX_MEMORY_LABEL_LENGTH
          || label.codePoints().anyMatch(Character::isISOControl)) {
        droppedCorrectionMemory(messageId, "label_invalid");
        return null;
      }
      return label;
    }

    private static void droppedCorrectionMemory(long messageId, String reason) {
      log.warn(
          "프리톡 턴 교정의 근거 기억 값이 계약과 달라 해당 값만 버립니다. "
              + "workflow=free_talk_turn_correction_memory reason={} messageId={}",
          reason,
          messageId);
    }

    private FreeTalkMistakePattern knownMistakePattern() {
      if (mistakePattern == null) {
        return null;
      }
      try {
        return FreeTalkMistakePattern.valueOf(mistakePattern);
      } catch (IllegalArgumentException exception) {
        return null;
      }
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteClosingResponse(
      String inferredTitle, String aiMessage, String translatedMessage, CharacterEmotion emotion) {

    // 원격 마무리 응답을 검증해 애플리케이션 결과로 변환한다.
    private AiFreeTalkClosingResult toResult() {
      if (blank(aiMessage) || blank(translatedMessage)) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      String normalizedTitle = blank(inferredTitle) ? null : inferredTitle;
      return new AiFreeTalkClosingResult(normalizedTitle, aiMessage, translatedMessage, emotion);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteExpressionRecommendationsResponse(
      List<AiFreeTalkExpressionRecommendation> recommendations,
      List<AiFreeTalkUsedExpression> usedExpressions) {

    // 원격 표현 추천 응답을 검증해 애플리케이션 결과로 변환한다.
    private AiFreeTalkExpressionRecommendationsResult toResult(
        AiFreeTalkExpressionRecommendationsRequest request) {
      if (recommendations == null
          || recommendations.isEmpty()
          || recommendations.size() > 3
          || request.existingExpressions() == null
          || hasInvalidRecommendation(recommendations, request.existingExpressions())) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkExpressionRecommendationsResult(
          recommendations, presentUsedExpressions());
    }

    // 다시 쓴 표현은 부가 결과라 없거나 빈 항목이 섞여도 추천을 실패시키지 않는다. 내용 검증은 저장하는 쪽에서 한다.
    // 목록이 아닌 값처럼 형식 자체가 깨진 응답은 추천과 한 본문이라 함께 거부된다. AI 서버의 응답 모델이 형식을 보장한다.
    private List<AiFreeTalkUsedExpression> presentUsedExpressions() {
      if (usedExpressions == null) {
        return List.of();
      }
      return usedExpressions.stream().filter(Objects::nonNull).toList();
    }
  }

  // 추천 목록에 순서나 출처가 잘못된 표현이 있는지 확인한다.
  private static boolean hasInvalidRecommendation(
      List<AiFreeTalkExpressionRecommendation> recommendations,
      List<AiFreeTalkExistingExpression> existingExpressions) {
    for (int index = 0; index < recommendations.size(); index++) {
      if (invalidRecommendation(recommendations.get(index), existingExpressions, index + 1)) {
        return true;
      }
    }
    return false;
  }

  // 개별 추천 표현의 필수 값과 기존 표현 참조를 검증한다.
  private static boolean invalidRecommendation(
      AiFreeTalkExpressionRecommendation recommendation,
      List<AiFreeTalkExistingExpression> existingExpressions,
      int expectedDisplayOrder) {
    if (recommendation == null
        || recommendation.displayOrder() != expectedDisplayOrder
        || recommendation.existingExpressionId() == null) {
      return true;
    }
    return existingExpressions.stream()
        .noneMatch(
            expression -> expression.expressionId().equals(recommendation.existingExpressionId()));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteConversationEmbeddingsResponse(List<AiConversationExcerpt> excerpts) {

    // 원격 대화 임베딩 응답을 검증해 애플리케이션 결과로 변환한다.
    private AiConversationEmbeddingsResult toResult() {
      if (excerpts == null
          || excerpts.isEmpty()
          || excerpts.size() > MAX_CONVERSATION_EXCERPTS
          || excerpts.stream().anyMatch(RemoteConversationEmbeddingsResponse::invalidExcerpt)) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiConversationEmbeddingsResult(excerpts);
    }

    // 추출 발화의 필수 값과 임베딩 차원을 검증한다.
    private static boolean invalidExcerpt(AiConversationExcerpt excerpt) {
      return excerpt == null
          || blank(excerpt.excerptText())
          || excerpt.embedding() == null
          || excerpt.embedding().size() != AiConversationExcerpt.EMBEDDING_DIMENSION
          || excerpt.embedding().contains(null);
    }
  }

  // 선택 질문의 원문과 번역이 함께 제공됐는지 검증한다.
  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
