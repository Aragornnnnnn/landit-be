// Apple 사용자 이전 단계의 처리 결과 집계를 전달한다.

package com.landit.landitbe.feature.auth.migration;

/**
 * Apple 사용자 이전 단계의 처리 결과 집계다.
 *
 * @param targetCount 전체 대상 수
 * @param successCount 현재 단계 성공 수
 * @param failureCount 현재 단계 실패 수
 * @param unresolvedCount 현재 단계 미완료 수
 */
public record AppleUserMigrationSummary(
    long targetCount, long successCount, long failureCount, long unresolvedCount) {

  /**
   * 현재 단계의 모든 대상이 완료됐는지 반환한다.
   *
   * @return 미완료 대상이 없으면 true
   */
  public boolean completed() {
    return unresolvedCount == 0;
  }
}
