// Apple 이전 수신 Team의 사용자 식별 정보를 전달한다.

package com.landit.landitbe.feature.auth.migration;

/**
 * Apple 이전 수신 Team의 사용자 식별 정보다.
 *
 * @param providerUserId 수신 Team 범위의 Apple 사용자 식별자
 * @param providerEmail 수신 Team private relay 이메일, 없으면 null
 */
public record AppleRecipientUser(String providerUserId, String providerEmail) {}
