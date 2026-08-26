// 발음 평가 자산의 2단계 임포트(기준 데이터 → TTS)와 커버리지 계산을 처리한다.

package com.landit.landitbe.feature.content.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.content.client.PronunciationManifestReader;
import com.landit.landitbe.feature.content.domain.ExpressionPronunciationAsset;
import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.dto.AdminPronunciationAssetCoverageResponse;
import com.landit.landitbe.feature.content.dto.AdminPronunciationAssetImportResult;
import com.landit.landitbe.feature.content.dto.PronunciationReferenceManifest;
import com.landit.landitbe.feature.content.dto.PronunciationTtsManifest;
import com.landit.landitbe.feature.content.repository.ExpressionPronunciationAssetRepository;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발음 평가 자산의 2단계 임포트와 커버리지 계산을 처리한다.
 *
 * <p>1단계(기준 데이터): AI 파이프라인이 만든 locale별 기준 데이터 JSON을 읽어 자산 행을 만든다 — 음성 URL은 아직 비어 있다. 2단계(TTS): TTS
 * 매니페스트의 URL을 기존 행에 붙이고, 단어별 audioUrl을 order로 조인해 words에 넣는다. 실패한 건은 조용히 건너뛰지 않고 사유와 함께 결과에 담아
 * 반환한다.
 */
@Service
@RequiredArgsConstructor
public class ExpressionPronunciationAssetService {

  private static final String AUDIT_TARGET_TYPE = "EXPRESSION_PRONUNCIATION_ASSET";

  // 매니페스트 항목 수의 안전 상한. 전체 데이터(981 표현 × 3 억양)보다 넉넉하게 잡되 폭주는 막는다.
  private static final int MAX_MANIFEST_ENTRIES = 10_000;

  private final ExpressionPronunciationAssetRepository assetRepository;
  private final WritingExpressionRepository writingExpressionRepository;
  private final AdminAuditService adminAuditService;
  private final PronunciationManifestReader manifestReader;

  // 매니페스트에 배치 메타데이터 같은 추가 필드가 있어도 무시하고 파싱한다.
  private final ObjectMapper objectMapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  /**
   * 1단계 — 기준 데이터 JSON(locale별)을 읽어 자산의 words를 upsert한다.
   *
   * <p>기준 데이터의 sentenceText가 DB의 대표 예문과 다르면 그 건은 실패 처리한다 — 문장이 바뀐 뒤 만든 낡은 기준 데이터가 들어오는 사고를 막는다.
   *
   * @param adminUserId 작업을 수행한 관리자 사용자 ID
   * @param manifestKey S3 기준 데이터 파일 키
   * @return 삽입·갱신 건수와 실패 목록
   */
  @Transactional
  public AdminPronunciationAssetImportResult importReference(Long adminUserId, String manifestKey) {
    PronunciationReferenceManifest manifest = parseReference(manifestReader.read(manifestKey));

    // 판별 재료를 미리 한 번에 조회한다 (건별 조회 방지).
    Set<Long> requestedIds =
        collectIds(manifest.entries(), PronunciationReferenceManifest.Entry::expressionId);
    Map<Long, WritingExpression> expressions = findExpressions(requestedIds);
    Map<AssetKey, ExpressionPronunciationAsset> existingAssets = findExistingAssets(requestedIds);

    int inserted = 0;
    int updated = 0;
    List<AdminPronunciationAssetImportResult.Failure> failures = new ArrayList<>();
    Set<AssetKey> processedKeys = new HashSet<>();

    for (PronunciationReferenceManifest.Entry entry : manifest.entries()) {
      AssetKey key = new AssetKey(entry.expressionId(), entry.accentLocale());
      String failureReason = validateReference(entry, expressions, processedKeys, key);
      if (failureReason != null) {
        failures.add(
            new AdminPronunciationAssetImportResult.Failure(
                entry.expressionId(), entry.accentLocale(), failureReason));
        continue;
      }
      processedKeys.add(key);

      ExpressionPronunciationAsset existing = existingAssets.get(key);
      if (existing != null) {
        // 기준 데이터를 교체하면 words 안의 audioUrl도 사라지므로, 이후 TTS 임포트를 다시 실행해야 한다.
        existing.replaceWords(entry.words());
        updated++;
      } else {
        assetRepository.save(
            new ExpressionPronunciationAsset(
                entry.expressionId(), entry.accentLocale(), entry.words()));
        inserted++;
      }
    }

    recordAudit(adminUserId, manifestKey, inserted, updated, failures.size());
    return new AdminPronunciationAssetImportResult(inserted, updated, failures);
  }

  /**
   * 2단계 — TTS 매니페스트의 음성 URL을 기존 자산에 붙인다.
   *
   * <p>문장·표현 URL 컬럼을 채우고, 단어별 audioUrl은 기준 데이터 words에 order로 조인해 넣는다. 기준 데이터가 아직 없는 (표현, 억양)은 실패
   * 처리한다 — 1단계를 먼저 실행해야 한다.
   *
   * @param adminUserId 작업을 수행한 관리자 사용자 ID
   * @param manifestKey S3 TTS 매니페스트 키
   * @return 갱신 건수와 실패 목록 (이 단계는 삽입이 없다)
   */
  @Transactional
  public AdminPronunciationAssetImportResult importTts(Long adminUserId, String manifestKey) {
    PronunciationTtsManifest manifest = parseTts(manifestReader.read(manifestKey));

    Set<Long> requestedIds =
        collectIds(manifest.assets(), PronunciationTtsManifest.Asset::expressionId);
    Map<AssetKey, ExpressionPronunciationAsset> existingAssets = findExistingAssets(requestedIds);

    int updated = 0;
    List<AdminPronunciationAssetImportResult.Failure> failures = new ArrayList<>();

    for (PronunciationTtsManifest.Asset ttsAsset : manifest.assets()) {
      AssetKey key = new AssetKey(ttsAsset.expressionId(), ttsAsset.accentLocale());
      ExpressionPronunciationAsset asset = existingAssets.get(key);
      String failureReason = validateTts(ttsAsset, asset);
      if (failureReason != null) {
        failures.add(
            new AdminPronunciationAssetImportResult.Failure(
                ttsAsset.expressionId(), ttsAsset.accentLocale(), failureReason));
        continue;
      }

      JsonNode joinedWords = joinWordAudio(asset.getWords(), ttsAsset.words());
      if (joinedWords == null) {
        failures.add(
            new AdminPronunciationAssetImportResult.Failure(
                ttsAsset.expressionId(),
                ttsAsset.accentLocale(),
                "TTS 매니페스트의 단어 order가 기준 데이터와 맞지 않습니다."));
        continue;
      }
      asset.attachTts(ttsAsset.expressionAudioUrl(), ttsAsset.sentenceAudioUrl(), joinedWords);
      updated++;
    }

    recordAudit(adminUserId, manifestKey, 0, updated, failures.size());
    return new AdminPronunciationAssetImportResult(0, updated, failures);
  }

  /**
   * 발음 자산이 빠진 표현이 없는지 억양별로 출석 체크한다.
   *
   * <p>"활성 표현 전체 명단"과 자산 상태를 대조해서, 억양마다 두 가지 결석자 명단을 만든다: 기준 데이터가 아예 없는 표현, 기준 데이터는 있는데 음성(TTS)이 아직
   * 없는 표현. 유저에게 노출되는 표현이 선별적이라 QA로는 전량 확인할 수 없으므로, 임포트 누락은 이 전수 대조로 잡는다. 두 missing이 모두 비어 있으면 완료
   * 확정이다.
   *
   * @return 전체 표현 수와, 억양별 (기준 데이터/음성 보유 수 + 빠진 표현 ID 목록)
   */
  @Transactional(readOnly = true)
  public AdminPronunciationAssetCoverageResponse coverage() {
    // 1단계: "우리 반 명단" = 활성 표현의 ID 전부를 가져온다.
    List<Long> activeExpressionIds =
        writingExpressionRepository.findIdsByStatus(ActiveStatus.ACTIVE);

    // 2단계: 자산 상태를 억양별로 묶는다 — 기준 데이터 보유 여부와 TTS 완성 여부를 나눠서.
    Map<AccentLocale, Set<Long>> referenceByLocale = new HashMap<>();
    Map<AccentLocale, Set<Long>> audioByLocale = new HashMap<>();
    for (ExpressionPronunciationAssetRepository.AssetLocaleView view :
        assetRepository.findAllBy()) {
      referenceByLocale
          .computeIfAbsent(view.getAccentLocale(), locale -> new HashSet<>())
          .add(view.getWritingExpressionId());
      if (view.getSentenceAudioUrl() != null && !view.getSentenceAudioUrl().isBlank()) {
        audioByLocale
            .computeIfAbsent(view.getAccentLocale(), locale -> new HashSet<>())
            .add(view.getWritingExpressionId());
      }
    }

    // 3단계: 억양마다 명단과 대조해 "결석자"를 골라낸다.
    List<AdminPronunciationAssetCoverageResponse.LocaleCoverage> locales = new ArrayList<>();
    for (AccentLocale locale : AccentLocale.values()) {
      Set<Long> hasReference = referenceByLocale.getOrDefault(locale, Set.of());
      Set<Long> hasAudio = audioByLocale.getOrDefault(locale, Set.of());
      List<Long> referenceMissing =
          activeExpressionIds.stream().filter(id -> !hasReference.contains(id)).sorted().toList();
      // 음성 결석은 "기준 데이터는 있는데 음성이 없는" 표현만 — 기준 데이터부터 없는 건 위 목록이 담당한다.
      List<Long> audioMissing =
          activeExpressionIds.stream()
              .filter(id -> hasReference.contains(id) && !hasAudio.contains(id))
              .sorted()
              .toList();
      locales.add(
          new AdminPronunciationAssetCoverageResponse.LocaleCoverage(
              locale,
              activeExpressionIds.size() - referenceMissing.size(),
              referenceMissing,
              (int) activeExpressionIds.stream().filter(hasAudio::contains).count(),
              audioMissing));
    }
    return new AdminPronunciationAssetCoverageResponse(activeExpressionIds.size(), locales);
  }

  // 기준 데이터 JSON을 파싱하고 목록의 기본 형태를 검증한다. 파일 최상위는 항목 배열이다.
  private PronunciationReferenceManifest parseReference(String manifestJson) {
    List<PronunciationReferenceManifest.Entry> entries;
    try {
      entries =
          objectMapper.readValue(
              manifestJson,
              objectMapper
                  .getTypeFactory()
                  .constructCollectionType(List.class, PronunciationReferenceManifest.Entry.class));
    } catch (Exception exception) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "기준 데이터 JSON 형식이 올바르지 않습니다.");
    }
    validateEntryCount(entries == null ? 0 : entries.size());
    return new PronunciationReferenceManifest(entries);
  }

  // TTS 매니페스트 JSON을 파싱하고 목록의 기본 형태를 검증한다.
  private PronunciationTtsManifest parseTts(String manifestJson) {
    PronunciationTtsManifest manifest;
    try {
      manifest = objectMapper.readValue(manifestJson, PronunciationTtsManifest.class);
    } catch (Exception exception) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "TTS 매니페스트 JSON 형식이 올바르지 않습니다.");
    }
    validateEntryCount(manifest.assets() == null ? 0 : manifest.assets().size());
    return manifest;
  }

  // 매니페스트 항목 수가 비어 있지 않고 상한 이하인지 확인한다.
  private void validateEntryCount(int count) {
    if (count == 0) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "매니페스트에 항목이 없습니다.");
    }
    if (count > MAX_MANIFEST_ENTRIES) {
      throw new ApiException(
          ErrorCode.INVALID_REQUEST, "매니페스트 항목이 상한(%d)을 초과했습니다.".formatted(MAX_MANIFEST_ENTRIES));
    }
  }

  // 기준 데이터 항목의 실패 사유를 판별한다. 정상이면 null.
  private String validateReference(
      PronunciationReferenceManifest.Entry entry,
      Map<Long, WritingExpression> expressions,
      Set<AssetKey> processedKeys,
      AssetKey key) {
    // sentenceText도 필수다 — 없으면 아래의 낡은 데이터 검증이 통째로 우회되기 때문이다.
    if (entry.expressionId() == null
        || entry.accentLocale() == null
        || entry.words() == null
        || entry.sentenceText() == null
        || entry.sentenceText().isBlank()) {
      return "필수 값이 누락됐습니다.";
    }
    WritingExpression expression = expressions.get(entry.expressionId());
    if (expression == null) {
      return "존재하지 않는 표현입니다.";
    }
    if (processedKeys.contains(key)) {
      return "매니페스트 안에 같은 (표현, 억양) 항목이 중복됩니다.";
    }
    if (!entry.words().isArray() || entry.words().isEmpty()) {
      return "words는 비어 있지 않은 배열이어야 합니다.";
    }
    // 낡은 기준 데이터 방지: 만들 때 쓴 문장과 지금 DB 문장이 다르면 거른다 (V60처럼 문장이 바뀐 경우).
    if (!Objects.equals(entry.sentenceText(), expression.getRepresentativeSentenceText())) {
      return "기준 데이터의 문장이 DB의 대표 예문과 다릅니다. 최신 문장으로 재생성이 필요합니다.";
    }
    return null;
  }

  // TTS 항목의 실패 사유를 판별한다. 정상이면 null.
  private String validateTts(
      PronunciationTtsManifest.Asset ttsAsset, ExpressionPronunciationAsset existingAsset) {
    if (ttsAsset.expressionId() == null
        || ttsAsset.accentLocale() == null
        || ttsAsset.sentenceAudioUrl() == null
        || ttsAsset.sentenceAudioUrl().isBlank()
        || ttsAsset.expressionAudioUrl() == null
        || ttsAsset.expressionAudioUrl().isBlank()
        || ttsAsset.words() == null
        || ttsAsset.words().isEmpty()) {
      return "필수 값이 누락됐습니다.";
    }
    if (existingAsset == null) {
      return "기준 데이터가 없습니다. 기준 데이터 임포트를 먼저 실행하세요.";
    }
    return null;
  }

  // 기준 데이터 words에 단어별 audioUrl을 order로 조인한 새 배열을 만든다. order가 안 맞으면 null.
  private JsonNode joinWordAudio(
      JsonNode referenceWords, List<PronunciationTtsManifest.Asset.WordAudio> wordAudios) {
    Map<Integer, String> audioByOrder =
        wordAudios.stream()
            .collect(
                Collectors.toMap(
                    PronunciationTtsManifest.Asset.WordAudio::order,
                    PronunciationTtsManifest.Asset.WordAudio::audioUrl,
                    (first, second) -> first));
    ArrayNode joined = objectMapper.createArrayNode();
    for (JsonNode word : referenceWords) {
      int order = word.path("order").asInt();
      String audioUrl = audioByOrder.get(order);
      if (audioUrl == null || audioUrl.isBlank()) {
        return null; // 기준 데이터의 단어인데 음성이 없으면 불완전한 매니페스트다.
      }
      ObjectNode withAudio = word.deepCopy();
      withAudio.put("audioUrl", audioUrl);
      joined.add(withAudio);
    }
    return joined;
  }

  // 목록에서 표현 ID를 중복 없이 모은다. 필수 값 검증 전이므로 null은 제외한다.
  private <T> Set<Long> collectIds(
      List<T> items, java.util.function.Function<T, Long> idExtractor) {
    return items.stream().map(idExtractor).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  // 요청된 표현들을 ID로 찾을 수 있게 Map으로 만든다.
  private Map<Long, WritingExpression> findExpressions(Set<Long> expressionIds) {
    return writingExpressionRepository.findAllById(expressionIds).stream()
        .collect(Collectors.toMap(WritingExpression::getId, expression -> expression));
  }

  // 요청된 표현들의 기존 자산을 (표현, 억양) 키로 찾을 수 있게 Map으로 만든다.
  private Map<AssetKey, ExpressionPronunciationAsset> findExistingAssets(Set<Long> expressionIds) {
    Map<AssetKey, ExpressionPronunciationAsset> assets = new HashMap<>();
    for (ExpressionPronunciationAsset asset :
        assetRepository.findAllByWritingExpressionIdIn(expressionIds)) {
      assets.put(new AssetKey(asset.getWritingExpressionId(), asset.getAccentLocale()), asset);
    }
    return assets;
  }

  // 임포트 결과 요약을 관리자 감사 로그로 남긴다. targetId에 매니페스트 키를 기록해 추적 가능하게 한다.
  private void recordAudit(
      Long adminUserId, String manifestKey, int inserted, int updated, int failed) {
    adminAuditService.record(
        adminUserId,
        AdminAction.EXPRESSION_PRONUNCIATION_ASSET_IMPORTED,
        AUDIT_TARGET_TYPE,
        manifestKey,
        null,
        "inserted=%d, updated=%d, failed=%d".formatted(inserted, updated, failed));
  }

  // upsert 판별에 사용하는 (표현 ID, 억양) 복합 키다.
  private record AssetKey(Long writingExpressionId, AccentLocale accentLocale) {}
}
