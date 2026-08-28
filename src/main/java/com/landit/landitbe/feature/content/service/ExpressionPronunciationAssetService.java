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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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

  // 발화 불가능한 패턴형 표현("be busy ~ing", "+목적어" 등) 판별 문자. 이런 표현은 TTS 배치가
  // 표현 음성을 아예 만들지 않아 expressionAudioUrl이 null인 것이 정상이다.
  // landit-ai scripts/build_tts_source.py의 _TEMPLATED 정규식과 같은 규칙이다 — 한쪽을 바꾸면
  // 반드시 같이 바꿔야 한다.
  private static final Pattern TEMPLATED_EXPRESSION_CHARS = Pattern.compile("[~가-힣()+]");

  private final ExpressionPronunciationAssetRepository assetRepository;
  private final WritingExpressionRepository writingExpressionRepository;
  private final AdminAuditService adminAuditService;
  private final PronunciationManifestReader manifestReader;
  private final PlatformTransactionManager transactionManager;

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
  public AdminPronunciationAssetImportResult importReference(Long adminUserId, String manifestKey) {
    // S3 다운로드·파싱은 트랜잭션 밖에서 한다 — 외부 I/O가 느려질 때 DB 커넥션을 점유하지 않게.
    // upsert만 수동 트랜잭션 경계(TransactionTemplate)로 감싼다. (@Transactional을 내부 메서드에
    // 붙이면 자기 호출이라 프록시를 타지 않아 적용되지 않는다.)
    PronunciationReferenceManifest manifest = parseReference(manifestReader.read(manifestKey));
    return new TransactionTemplate(transactionManager)
        .execute(status -> upsertReference(adminUserId, manifestKey, manifest));
  }

  // 기준 데이터 upsert의 트랜잭션 본문이다. 전 건이 하나의 트랜잭션으로 묶인다.
  private AdminPronunciationAssetImportResult upsertReference(
      Long adminUserId, String manifestKey, PronunciationReferenceManifest manifest) {
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
  public AdminPronunciationAssetImportResult importTts(Long adminUserId, String manifestKey) {
    // 기준 데이터 임포트와 같은 이유로 S3 읽기는 트랜잭션 밖, upsert만 트랜잭션 안이다.
    PronunciationTtsManifest manifest = parseTts(manifestReader.read(manifestKey));
    return new TransactionTemplate(transactionManager)
        .execute(status -> attachTtsInTransaction(adminUserId, manifestKey, manifest));
  }

  // TTS URL 부착의 트랜잭션 본문이다.
  private AdminPronunciationAssetImportResult attachTtsInTransaction(
      Long adminUserId, String manifestKey, PronunciationTtsManifest manifest) {
    Set<Long> requestedIds =
        collectIds(manifest.assets(), PronunciationTtsManifest.Asset::expressionId);
    Map<AssetKey, ExpressionPronunciationAsset> existingAssets = findExistingAssets(requestedIds);
    // 표현 텍스트로 패턴형 여부를 판별해야 해서 (validateTts 참고) 표현도 함께 조회한다.
    Map<Long, WritingExpression> expressions = findExpressions(requestedIds);

    int updated = 0;
    List<AdminPronunciationAssetImportResult.Failure> failures = new ArrayList<>();

    for (PronunciationTtsManifest.Asset ttsAsset : manifest.assets()) {
      AssetKey key = new AssetKey(ttsAsset.expressionId(), ttsAsset.accentLocale());
      ExpressionPronunciationAsset asset = existingAssets.get(key);
      String failureReason = validateTts(ttsAsset, asset, expressions.get(ttsAsset.expressionId()));
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
    // 배열 안 항목의 품질도 확인한다 — order 누락/중복이나 빈 word가 저장되면
    // 런타임 병합(order 조인)이 그때서야 깨진다. 임포트 시점에 거르는 게 낫다.
    String wordItemFailure = validateWordItems(entry.words());
    if (wordItemFailure != null) {
      return wordItemFailure;
    }
    // 낡은 기준 데이터 방지: 만들 때 쓴 문장과 지금 DB 문장이 다르면 거른다 (V61처럼 문장이 바뀐 경우).
    if (!Objects.equals(entry.sentenceText(), expression.getRepresentativeSentenceText())) {
      return "기준 데이터의 문장이 DB의 대표 예문과 다릅니다. 최신 문장으로 재생성이 필요합니다.";
    }
    return null;
  }

  // TTS 항목의 실패 사유를 판별한다. 정상이면 null.
  private String validateTts(
      PronunciationTtsManifest.Asset ttsAsset,
      ExpressionPronunciationAsset existingAsset,
      WritingExpression expression) {
    if (ttsAsset.expressionId() == null
        || ttsAsset.accentLocale() == null
        || ttsAsset.sentenceAudioUrl() == null
        || ttsAsset.sentenceAudioUrl().isBlank()
        || ttsAsset.words() == null
        || ttsAsset.words().isEmpty()) {
      return "필수 값이 누락됐습니다.";
    }
    // 단어 음성 URL이 비면 조인 단계에서 NPE로 임포트 전체가 죽는다. 여기서 실패 목록행으로 거른다.
    for (PronunciationTtsManifest.Asset.WordAudio wordAudio : ttsAsset.words()) {
      if (wordAudio.audioUrl() == null || wordAudio.audioUrl().isBlank()) {
        return "TTS 매니페스트 words 항목에 audioUrl이 없습니다.";
      }
    }
    if (existingAsset == null) {
      return "기준 데이터가 없습니다. 기준 데이터 임포트를 먼저 실행하세요.";
    }
    // 표현 음성(expressionAudioUrl)은 패턴형 표현에만 null이 허용된다. 발화 가능한 일반 표현인데
    // null이면 생성 누락 사고이므로 실패 목록행으로 잡는다.
    boolean hasExpressionAudio =
        ttsAsset.expressionAudioUrl() != null && !ttsAsset.expressionAudioUrl().isBlank();
    if (!hasExpressionAudio && !isTemplatedExpression(expression)) {
      return "발화 가능한 표현인데 표현 음성(expressionAudioUrl)이 없습니다.";
    }
    return null;
  }

  // 표현이 발화 불가능한 패턴형인지 판별한다. 표현을 못 찾으면(이론상 자산이 있으면 항상 있음)
  // 보수적으로 일반 표현 취급해 표현 음성 누락을 잡는다.
  private boolean isTemplatedExpression(WritingExpression expression) {
    return expression != null
        && TEMPLATED_EXPRESSION_CHARS.matcher(expression.getTargetExpressionText()).find();
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

  // words 배열 항목들의 order·word 품질을 검증한다. 정상이면 null.
  // order 누락/중복이나 빈 word가 저장되면 런타임 병합(order 조인)이 그때서야 깨지므로 임포트 시점에 거른다.
  private String validateWordItems(JsonNode words) {
    Set<Integer> orders = new HashSet<>();
    for (JsonNode word : words) {
      if (!word.path("order").isInt() || word.path("order").asInt() < 1) {
        return "words 항목에 order가 없거나 올바르지 않습니다.";
      }
      if (!orders.add(word.path("order").asInt())) {
        return "words 항목의 order가 중복됩니다.";
      }
      if (word.path("word").asText("").isBlank()) {
        return "words 항목에 word가 없습니다.";
      }
    }
    return null;
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
