// 발음 평가 자산의 S3 매니페스트 임포트와 커버리지 계산을 처리한다.

package com.landit.landitbe.feature.content.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.admin.domain.AdminAction;
import com.landit.landitbe.feature.admin.service.AdminAuditService;
import com.landit.landitbe.feature.content.client.PronunciationManifestReader;
import com.landit.landitbe.feature.content.domain.ExpressionPronunciationAsset;
import com.landit.landitbe.feature.content.dto.AdminPronunciationAssetCoverageResponse;
import com.landit.landitbe.feature.content.dto.AdminPronunciationAssetImportResult;
import com.landit.landitbe.feature.content.dto.PronunciationAssetManifest;
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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발음 평가 자산의 S3 매니페스트 임포트와 커버리지 계산을 처리한다.
 *
 * <p>TTS 사전 생성 배치(landit-iac)가 S3에 올린 매니페스트를 내려받아 (표현, 억양) 단위로 upsert한다. 실패한 건은 조용히 건너뛰지 않고 사유와 함께
 * 결과에 담아 반환한다.
 */
@Service
@RequiredArgsConstructor
public class ExpressionPronunciationAssetService {

  private static final String AUDIT_TARGET_TYPE = "EXPRESSION_PRONUNCIATION_ASSET";

  // 매니페스트 항목 수의 안전 상한. 전체 데이터(981 표현 × 3 억양)보다 넉넉하게 잡되 폭주는 막는다.
  private static final int MAX_MANIFEST_ASSETS = 10_000;

  private final ExpressionPronunciationAssetRepository assetRepository;
  private final WritingExpressionRepository writingExpressionRepository;
  private final AdminAuditService adminAuditService;
  private final PronunciationManifestReader manifestReader;

  // 매니페스트에 배치 메타데이터 같은 추가 필드가 있어도 무시하고 파싱한다.
  private final ObjectMapper objectMapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  /**
   * S3의 매니페스트를 내려받아 발음 평가 자산을 (표현, 억양) 단위로 upsert한다.
   *
   * @param adminUserId 작업을 수행한 관리자 사용자 ID
   * @param manifestKey S3 매니페스트 키
   * @return 삽입·갱신 건수와 실패 목록
   * @throws ApiException 매니페스트가 없거나(404) 형식이 잘못됐을 때(400)
   */
  @Transactional
  public AdminPronunciationAssetImportResult importFromManifest(
      Long adminUserId, String manifestKey) {
    // 0단계: S3(또는 로컬 스텁)에서 매니페스트 JSON을 읽어 파싱·기본 검증한다.
    PronunciationAssetManifest manifest = parseManifest(manifestReader.read(manifestKey));

    // 1단계: 판별에 필요한 데이터를 미리 한 번에 조회해 둔다.
    //   - 어떤 표현 ID가 실제로 존재하는지 (없는 표현이면 실패 처리해야 하므로)
    //   - 어떤 (표현, 억양) 자산이 이미 있는지 (있으면 갱신, 없으면 삽입이므로)
    //   건별로 조회하면 항목 수만큼 쿼리가 나가서, 여기서 각 1번씩만 조회한다.
    Set<Long> requestedExpressionIds = collectExpressionIds(manifest);
    Set<Long> existingExpressionIds = findExistingExpressionIds(requestedExpressionIds);
    Map<AssetKey, ExpressionPronunciationAsset> existingAssets =
        findExistingAssets(requestedExpressionIds);

    // 2단계: 항목을 하나씩 검증하고 upsert한다. 결과는 삽입/갱신/실패 셋 중 하나다.
    int inserted = 0;
    int updated = 0;
    List<AdminPronunciationAssetImportResult.Failure> failures = new ArrayList<>();
    Set<AssetKey> processedKeys = new HashSet<>(); // 매니페스트 안에서 같은 (표현, 억양)이 중복되는 것을 잡는다.

    for (PronunciationAssetManifest.Asset asset : manifest.assets()) {
      ImportOutcome outcome =
          importOne(asset, existingExpressionIds, existingAssets, processedKeys, failures);
      switch (outcome) {
        case INSERTED -> inserted++;
        case UPDATED -> updated++;
        case FAILED -> {}
      }
    }

    // 3단계: 관리자 쓰기 작업이므로 감사 로그를 요청당 1건 남기고 결과를 반환한다.
    //   어떤 매니페스트를 임포트했는지 추적할 수 있게 대상 식별자로 매니페스트 키를 기록한다.
    recordAudit(adminUserId, manifestKey, inserted, updated, failures.size());
    return new AdminPronunciationAssetImportResult(inserted, updated, failures);
  }

  /**
   * 발음 자산이 빠진 표현이 없는지 억양별로 출석 체크한다.
   *
   * <p>"활성 표현 전체 명단"과 "자산이 실제로 들어 있는 표현"을 대조해서, 억양마다 자산이 없는 표현 ID 목록(결석자 명단)을 만든다. 유저에게 노출되는 표현이
   * 선별적이라 QA로는 981개를 전부 확인할 수 없으므로, 임포트 누락은 이 전수 대조로 잡는다. 임포트 후 모든 억양의 missing이 비어 있으면 완료 확정이다.
   *
   * @return 전체 표현 수와, 억양별 (자산 보유 수 + 자산이 없는 표현 ID 목록)
   */
  @Transactional(readOnly = true)
  public AdminPronunciationAssetCoverageResponse coverage() {
    // 1단계: "우리 반 명단" = 활성 표현의 ID 전부를 가져온다.
    List<Long> activeExpressionIds =
        writingExpressionRepository.findIdsByStatus(ActiveStatus.ACTIVE);

    // 2단계: "출석한 애들" = 자산 테이블에 행이 있는 (표현, 억양)을 억양별 Set으로 묶는다.
    //   words JSONB까지 다 읽으면 무거우니 (표현 ID, 억양) 두 컬럼만 가볍게 조회한다.
    Map<AccentLocale, Set<Long>> coveredByLocale = new HashMap<>();
    for (ExpressionPronunciationAssetRepository.AssetLocaleView view :
        assetRepository.findAllBy()) {
      coveredByLocale
          .computeIfAbsent(view.getAccentLocale(), locale -> new HashSet<>())
          .add(view.getWritingExpressionId());
    }

    // 3단계: 억양마다 명단과 대조해 "결석자"(활성 표현인데 자산이 없는 ID)를 골라낸다.
    List<AdminPronunciationAssetCoverageResponse.LocaleCoverage> locales = new ArrayList<>();
    for (AccentLocale locale : AccentLocale.values()) {
      Set<Long> covered = coveredByLocale.getOrDefault(locale, Set.of());
      List<Long> missing =
          activeExpressionIds.stream().filter(id -> !covered.contains(id)).sorted().toList();
      locales.add(
          new AdminPronunciationAssetCoverageResponse.LocaleCoverage(
              locale, activeExpressionIds.size() - missing.size(), missing));
    }
    return new AdminPronunciationAssetCoverageResponse(activeExpressionIds.size(), locales);
  }

  // 매니페스트 JSON을 파싱하고 목록의 기본 형태를 검증한다.
  private PronunciationAssetManifest parseManifest(String manifestJson) {
    PronunciationAssetManifest manifest;
    try {
      manifest = objectMapper.readValue(manifestJson, PronunciationAssetManifest.class);
    } catch (Exception exception) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "매니페스트 JSON 형식이 올바르지 않습니다.");
    }
    if (manifest.assets() == null || manifest.assets().isEmpty()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "매니페스트에 assets 목록이 없습니다.");
    }
    if (manifest.assets().size() > MAX_MANIFEST_ASSETS) {
      throw new ApiException(
          ErrorCode.INVALID_REQUEST, "매니페스트 항목이 상한(%d)을 초과했습니다.".formatted(MAX_MANIFEST_ASSETS));
    }
    return manifest;
  }

  // 자산 1건을 검증하고 upsert한다. 검증에 걸리면 실패 목록에 사유를 담고 FAILED를 반환한다.
  private ImportOutcome importOne(
      PronunciationAssetManifest.Asset asset,
      Set<Long> existingExpressionIds,
      Map<AssetKey, ExpressionPronunciationAsset> existingAssets,
      Set<AssetKey> processedKeys,
      List<AdminPronunciationAssetImportResult.Failure> failures) {
    AssetKey key = new AssetKey(asset.expressionId(), asset.accentLocale());

    String failureReason = validate(asset, existingExpressionIds, processedKeys, key);
    if (failureReason != null) {
      failures.add(
          new AdminPronunciationAssetImportResult.Failure(
              asset.expressionId(), asset.accentLocale(), failureReason));
      return ImportOutcome.FAILED;
    }
    processedKeys.add(key);

    // 이미 있는 (표현, 억양)이면 내용만 교체하고, 없으면 새 행을 만든다.
    ExpressionPronunciationAsset existing = existingAssets.get(key);
    if (existing != null) {
      existing.replaceContents(asset.expressionAudioUrl(), asset.sentenceAudioUrl(), asset.words());
      return ImportOutcome.UPDATED;
    }
    assetRepository.save(
        new ExpressionPronunciationAsset(
            asset.expressionId(),
            asset.accentLocale(),
            asset.expressionAudioUrl(),
            asset.sentenceAudioUrl(),
            asset.words()));
    return ImportOutcome.INSERTED;
  }

  // 매니페스트 항목의 실패 사유를 판별한다. 정상이면 null을 반환한다.
  private String validate(
      PronunciationAssetManifest.Asset asset,
      Set<Long> existingExpressionIds,
      Set<AssetKey> processedKeys,
      AssetKey key) {
    // HTTP 요청 본문이 아니라 S3 파일이라 bean validation이 없으므로 필수 값을 코드로 검사한다.
    if (asset.expressionId() == null
        || asset.accentLocale() == null
        || asset.expressionAudioUrl() == null
        || asset.expressionAudioUrl().isBlank()
        || asset.sentenceAudioUrl() == null
        || asset.sentenceAudioUrl().isBlank()
        || asset.words() == null) {
      return "필수 값이 누락됐습니다.";
    }
    if (!existingExpressionIds.contains(asset.expressionId())) {
      return "존재하지 않는 표현입니다.";
    }
    if (processedKeys.contains(key)) {
      return "매니페스트 안에 같은 (표현, 억양) 항목이 중복됩니다.";
    }
    if (!asset.words().isArray() || asset.words().isEmpty()) {
      return "words는 비어 있지 않은 배열이어야 합니다.";
    }
    return null;
  }

  // 매니페스트에 담긴 표현 ID를 중복 없이 모은다. 필수 값 검증 전이므로 null은 제외한다.
  private Set<Long> collectExpressionIds(PronunciationAssetManifest manifest) {
    return manifest.assets().stream()
        .map(PronunciationAssetManifest.Asset::expressionId)
        .filter(id -> id != null)
        .collect(Collectors.toSet());
  }

  // 요청된 표현 ID 중 실제로 존재하는 것만 골라낸다.
  private Set<Long> findExistingExpressionIds(Set<Long> expressionIds) {
    return writingExpressionRepository.findAllById(expressionIds).stream()
        .map(expression -> expression.getId())
        .collect(Collectors.toSet());
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

  // 자산 1건의 처리 결과다. 삽입됨 / 갱신됨 / 실패함.
  private enum ImportOutcome {
    INSERTED,
    UPDATED,
    FAILED
  }

  // upsert 판별에 사용하는 (표현 ID, 억양) 복합 키다.
  private record AssetKey(Long writingExpressionId, AccentLocale accentLocale) {}
}
