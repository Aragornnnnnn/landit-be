// 문장 발화 발음 평가를 오케스트레이션한다.

package com.landit.landitbe.feature.content.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.feature.content.client.ai.AiPronunciationAnalysisRequest;
import com.landit.landitbe.feature.content.client.ai.AiPronunciationAnalysisResult;
import com.landit.landitbe.feature.content.client.ai.AiPronunciationClient;
import com.landit.landitbe.feature.content.client.ai.AiPronunciationWordStatus;
import com.landit.landitbe.feature.content.domain.ExpressionPronunciationAsset;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.dto.PronunciationAnalysisResponse;
import com.landit.landitbe.feature.content.repository.ExpressionPronunciationAssetRepository;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 문장 발화 발음 평가를 오케스트레이션한다.
 *
 * <p>흐름: 녹음 검증 → 표현·유저 억양·자산 조회 → AI 서버 판정 → 점수 계산·코칭 문구 조립 → 자산 데이터 병합 → 응답. 사용자 음성은 판정 후 저장하지
 * 않는다.
 */
@Service
@RequiredArgsConstructor
public class ExpressionPronunciationService {

  // 허용하는 녹음 파일 확장자와 최대 크기 (명세 확정값).
  private static final Set<String> ALLOWED_AUDIO_FORMATS = Set.of("m4a", "wav", "mp3");
  private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;

  private final WritingExpressionRepository writingExpressionRepository;
  private final ExpressionPronunciationAssetRepository assetRepository;
  private final UserAccentLocaleResolver accentLocaleResolver;
  private final AiPronunciationClient aiPronunciationClient;
  private final PronunciationCoachingTemplate coachingTemplate;

  /**
   * 사용자 발화 녹음을 분석해 점수와 단어별 판정·코칭을 반환한다.
   *
   * @param userId 사용자 ID
   * @param expressionId Writing 표현 ID
   * @param audio 사용자 발화 녹음 파일
   * @return 점수·통과 여부·단어별 판정
   * @throws ApiException 오디오가 잘못됐거나(400), 표현·발음 데이터가 없거나(404), AI 분석이 실패했을 때(502)
   */
  @Transactional(readOnly = true)
  public PronunciationAnalysisResponse analyze(
      Long userId, Long expressionId, MultipartFile audio) {
    // 1단계: 녹음 파일이 형식·크기 제한에 맞는지 먼저 확인한다 (AI 호출 비용을 쓰기 전에 거른다).
    String audioFormat = validateAudio(audio);

    // 2단계: 판정에 필요한 재료를 모은다 — 표현(정답 문장), 유저의 목표 억양, 그 억양의 발음 자산.
    // 자산은 TTS까지 완성된 것만 쓴다 (기준 데이터만 있는 반쪽 자산은 판정 기준 음성이 없다).
    WritingExpression expression = requireActiveExpression(expressionId);
    AccentLocale accentLocale = accentLocaleResolver.require(userId);
    ExpressionPronunciationAsset asset = requireCompleteAsset(expressionId, accentLocale);
    Map<Integer, AssetWord> assetWords = parseAssetWords(asset.getWords());

    // 3단계: AI 서버에 판정을 요청한다. 유저 오디오는 base64로 실어 보낸다.
    // 단어 목록은 퀴즈 배열이 아니라 자산 words 기준이다 — 발음 정렬은 "late-night"을 2단어로 나누는 등
    // 퀴즈와 토큰화가 다르다. 억양 대조 힌트(accentContrast)도 있는 단어만 같이 실어 보낸다.
    AiPronunciationAnalysisResult aiResult =
        aiPronunciationClient.analyze(
            buildAiRequest(expression, asset, accentLocale, assetWords, audio, audioFormat));

    // 4단계: AI 판정 + 자산 기준 데이터 + 코칭 문구를 합쳐 응답을 조립한다.
    return buildResponse(assetWords, aiResult);
  }

  // 녹음 파일의 형식과 크기를 검증하고, AI 서버에 알려줄 형식 문자열을 반환한다.
  private String validateAudio(MultipartFile audio) {
    if (audio == null || audio.isEmpty()) {
      throw new ApiException(ErrorCode.INVALID_AUDIO, "오디오 파일이 비어 있습니다.");
    }
    if (audio.getSize() > MAX_AUDIO_BYTES) {
      throw new ApiException(ErrorCode.INVALID_AUDIO, "오디오 파일이 10MB를 초과했습니다.");
    }
    String format = extractFormat(audio.getOriginalFilename());
    if (!ALLOWED_AUDIO_FORMATS.contains(format)) {
      throw new ApiException(ErrorCode.INVALID_AUDIO, "m4a·wav·mp3 형식만 지원합니다.");
    }
    return format;
  }

  // 파일명에서 확장자를 소문자로 뽑는다. 없으면 빈 문자열.
  private String extractFormat(String filename) {
    if (filename == null) {
      return "";
    }
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      return "";
    }
    return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
  }

  // 활성 표현을 조회한다. 없으면 404.
  private WritingExpression requireActiveExpression(Long expressionId) {
    return writingExpressionRepository
        .findByIdAndStatus(expressionId, ActiveStatus.ACTIVE)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  // 표현·억양의 발음 자산을 조회한다. 자산이 없거나 TTS가 아직 없으면 404 — 반쪽 자산으로는 평가를 열지 않는다.
  private ExpressionPronunciationAsset requireCompleteAsset(
      Long expressionId, AccentLocale accentLocale) {
    ExpressionPronunciationAsset asset =
        assetRepository
            .findByWritingExpressionIdAndAccentLocale(expressionId, accentLocale)
            .orElseThrow(() -> new ApiException(ErrorCode.PRONUNCIATION_DATA_NOT_FOUND));
    if (!asset.hasTts()) {
      throw new ApiException(ErrorCode.PRONUNCIATION_DATA_NOT_FOUND);
    }
    return asset;
  }

  // AI 서버 판정 요청을 조립한다. 단어 목록은 자산 words 기준이며 억양 대조 힌트를 함께 싣는다.
  private AiPronunciationAnalysisRequest buildAiRequest(
      WritingExpression expression,
      ExpressionPronunciationAsset asset,
      AccentLocale accentLocale,
      Map<Integer, AssetWord> assetWords,
      MultipartFile audio,
      String audioFormat) {
    List<AiPronunciationAnalysisRequest.Word> words = new ArrayList<>();
    assetWords.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry ->
                words.add(
                    new AiPronunciationAnalysisRequest.Word(
                        entry.getKey(),
                        entry.getValue().word(),
                        entry.getValue().accentContrast())));
    return new AiPronunciationAnalysisRequest(
        encodeAudio(audio),
        audioFormat,
        expression.getRepresentativeSentenceText(),
        asset.getSentenceAudioUrl(),
        accentLocale,
        words);
  }

  // 녹음 파일을 base64 문자열로 바꾼다.
  private String encodeAudio(MultipartFile audio) {
    try {
      return Base64.getEncoder().encodeToString(audio.getBytes());
    } catch (IOException exception) {
      throw new ApiException(ErrorCode.INVALID_AUDIO, "오디오 파일을 읽을 수 없습니다.");
    }
  }

  // AI 판정 + 자산 기준 데이터를 order로 병합하고 점수·코칭을 계산해 응답을 만든다.
  private PronunciationAnalysisResponse buildResponse(
      Map<Integer, AssetWord> assetWords, AiPronunciationAnalysisResult aiResult) {
    int totalWords = assetWords.size();

    // AI 판정이 자산 단어와 1:1이 아니면 조용히 넘기지 않고 응답 오류로 처리한다.
    if (aiResult.words() == null || aiResult.words().size() != totalWords) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }

    List<PronunciationAnalysisResponse.Word> responseWords = new ArrayList<>();
    int errorCount = 0;
    for (AiPronunciationAnalysisResult.Word judged : aiResult.words()) {
      if (judged.status() != AiPronunciationWordStatus.CORRECT) {
        errorCount++;
      }
      responseWords.add(toResponseWord(judged, assetWords.get(judged.order())));
    }

    // 점수는 BE가 계산한다: 정상 단어 비율. 오류가 하나도 없으면 자연히 100이고 통과다.
    int score = Math.round((totalWords - errorCount) * 100f / totalWords);
    boolean passed = errorCount == 0;
    return new PronunciationAnalysisResponse(score, passed, responseWords);
  }

  // 단어 1개의 응답을 만든다. 오류 유형에 따라 채우는 필드가 다르다.
  // 저장 필드명(pronunciationDisplay·audioUrl)과 프론트 명세 필드명(nativeDisplay·nativeWordAudioUrl)이
  // 달라서 여기서 매핑한다 — 프론트 계약은 동결, 내부 저장은 AI 파이프라인 계약을 따른 결과다.
  private PronunciationAnalysisResponse.Word toResponseWord(
      AiPronunciationAnalysisResult.Word judged, AssetWord assetWord) {
    return switch (judged.status()) {
      case CORRECT ->
          new PronunciationAnalysisResponse.Word(
              judged.order(),
              judged.word(),
              judged.status().name(),
              judged.startMs(),
              judged.endMs(),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null);
      case PHONEME_ERROR ->
          new PronunciationAnalysisResponse.Word(
              judged.order(),
              judged.word(),
              judged.status().name(),
              judged.startMs(),
              judged.endMs(),
              assetWord == null ? null : assetWord.audioUrl(),
              assetWord == null ? null : assetWord.pronunciationDisplay(),
              judged.userDisplay(),
              judged.errorTargetSpan(),
              judged.errorUserSpan(),
              null,
              null,
              null,
              coachingTemplate.phonemeCoaching(judged.errorTargetSpan(), judged.errorUserSpan()));
      case STRESS_ERROR ->
          new PronunciationAnalysisResponse.Word(
              judged.order(),
              judged.word(),
              judged.status().name(),
              judged.startMs(),
              judged.endMs(),
              assetWord == null ? null : assetWord.audioUrl(),
              null,
              null,
              null,
              null,
              assetWord == null ? null : assetWord.syllables(),
              assetWord == null ? null : assetWord.stressIndex(),
              judged.userStressIndex(),
              coachingTemplate.stressCoaching(
                  assetWord == null ? null : assetWord.syllables(),
                  assetWord == null ? null : assetWord.stressIndex()));
    };
  }

  // 자산 words JSONB를 order로 찾을 수 있게 파싱한다.
  // 항목 계약: order, word, syllables[], stressIndex(무강세 -1), pronunciationDisplay,
  //   accentContrast{expected, other, errorType}(억양 대조 단어에만), audioUrl(TTS 임포트가 채움)
  private Map<Integer, AssetWord> parseAssetWords(JsonNode wordsNode) {
    Map<Integer, AssetWord> assetWords = new HashMap<>();
    for (JsonNode node : wordsNode) {
      List<String> syllables = null;
      if (node.hasNonNull("syllables") && node.get("syllables").isArray()) {
        syllables = new ArrayList<>();
        for (JsonNode syllable : node.get("syllables")) {
          syllables.add(syllable.asText());
        }
      }
      AiPronunciationAnalysisRequest.AccentContrast accentContrast = null;
      if (node.hasNonNull("accentContrast")) {
        JsonNode contrastNode = node.get("accentContrast");
        accentContrast =
            new AiPronunciationAnalysisRequest.AccentContrast(
                contrastNode.path("expected").asText(null),
                contrastNode.path("other").asText(null),
                contrastNode.path("errorType").asText(null));
      }
      assetWords.put(
          node.path("order").asInt(),
          new AssetWord(
              node.path("word").asText(null),
              node.hasNonNull("audioUrl") ? node.get("audioUrl").asText() : null,
              node.hasNonNull("pronunciationDisplay")
                  ? node.get("pronunciationDisplay").asText()
                  : null,
              syllables,
              node.hasNonNull("stressIndex") ? node.get("stressIndex").asInt() : null,
              accentContrast));
    }
    return assetWords;
  }

  // 자산 words JSONB의 단어 1건이다. 원어민 기준 데이터와 AI 요청에 실을 억양 대조 힌트를 담는다.
  private record AssetWord(
      String word,
      String audioUrl,
      String pronunciationDisplay,
      List<String> syllables,
      Integer stressIndex,
      AiPronunciationAnalysisRequest.AccentContrast accentContrast) {}
}
