// Apple 사용자 이전 단계에서 처리할 OAuth identity 정보를 전달한다.

package com.landit.landitbe.feature.auth.migration;

/**
 * Apple 사용자 이전 단계에서 처리할 OAuth identity 정보다.
 *
 * @param migrationId 사용자 이전 상태 ID
 * @param oauthIdentityId OAuth identity ID
 * @param providerUserId 현재 Apple 팀 사용자 식별자
 * @param transferSub Apple 이전 식별자
 */
public record AppleUserMigrationCandidate(
    long migrationId, long oauthIdentityId, String providerUserId, String transferSub) {}
