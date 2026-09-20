// 학습 시작 시 결정한 권한 근거를 저장 단계까지 전달한다.

package com.landit.landitbe.feature.subscription.dto;

/**
 * 시작 시점에 한 번 결정해 저장까지 유지하는 권한 근거다.
 *
 * @param policyVersion 시작 시 적용한 공개 정책 버전
 * @param basis 학습 허용 근거
 */
public record StartAccess(long policyVersion, String basis) {}
