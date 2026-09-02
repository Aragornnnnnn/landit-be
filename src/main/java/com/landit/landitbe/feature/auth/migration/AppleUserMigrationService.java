// Apple 사용자 식별자의 이전 준비와 완료 단계를 일괄 처리한다.

package com.landit.landitbe.feature.auth.migration;

import java.util.List;

/** Apple 사용자 식별자의 이전 준비와 완료 단계를 일괄 처리한다. */
public class AppleUserMigrationService {

  private final AppleUserMigrationRepository repository;
  private final AppleUserMigrationClient client;

  /**
   * 사용자 이전 저장소와 Apple API 클라이언트로 서비스를 생성한다.
   *
   * @param repository 사용자 이전 상태 저장소
   * @param client Apple 사용자 이전 API 클라이언트
   */
  public AppleUserMigrationService(
      AppleUserMigrationRepository repository, AppleUserMigrationClient client) {
    this.repository = repository;
    this.client = client;
  }

  /**
   * 선택한 사용자 이전 단계를 실행하고 결과를 집계한다.
   *
   * @param phase 실행 단계
   * @return 대상, 성공, 실패, 미완료 집계
   * @throws AppleUserMigrationException access token 발급이나 상태 저장 자체가 실패할 때
   */
  public AppleUserMigrationSummary run(AppleUserMigrationPhase phase) {
    if (phase == AppleUserMigrationPhase.PREPARE) {
      repository.initializePending();
    }
    List<AppleUserMigrationCandidate> candidates = repository.findCandidates(phase);
    if (candidates.isEmpty()) {
      return repository.summarize(phase);
    }

    String accessToken = client.requestAccessToken();
    for (AppleUserMigrationCandidate candidate : candidates) {
      migrateOne(phase, accessToken, candidate);
    }
    return repository.summarize(phase);
  }

  private void migrateOne(
      AppleUserMigrationPhase phase, String accessToken, AppleUserMigrationCandidate candidate) {
    try {
      if (phase == AppleUserMigrationPhase.PREPARE) {
        String transferSub = client.createTransferSub(accessToken, candidate.providerUserId());
        repository.markPrepared(candidate.migrationId(), transferSub);
        return;
      }

      if (candidate.transferSub() == null || candidate.transferSub().isBlank()) {
        throw new AppleUserMigrationException("TRANSFER_SUB_MISSING");
      }
      AppleRecipientUser recipientUser =
          client.exchangeTransferSub(accessToken, candidate.transferSub());
      repository.complete(
          candidate.migrationId(), recipientUser.providerUserId(), recipientUser.providerEmail());
    } catch (AppleUserMigrationException exception) {
      repository.markFailed(candidate.migrationId(), phase, exception.failureCode());
    }
  }
}
