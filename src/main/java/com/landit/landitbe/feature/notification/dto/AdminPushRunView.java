// 관리자 푸시 실행의 진행 상태와 토큰 기준 집계를 전달한다.

package com.landit.landitbe.feature.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 푸시 실행의 진행 상태와 집계다. 성공은 APNs/FCM 인계 성공을 의미한다.
 *
 * @param id 실행 ID
 * @param campaignId 캠페인 ID
 * @param mode 전체 발송 또는 관리자 테스트
 * @param status 실행 상태
 * @param audienceCapturedAt 대상 확정 시각, 미확정이면 null
 * @param targetUserCount 확정 사용자 수, 미확정이면 null
 * @param targetTokenCount 확정 토큰 수, 미확정이면 null
 * @param pendingCount 제출 또는 Receipt 확인 대기 수
 * @param succeededCount APNs/FCM 인계 성공 수
 * @param failedCount 명시적인 실패 수
 * @param excludedCount 발송 전 제외 수
 * @param unknownCount 결과 확인 불가 수
 * @param ticketAcceptedCount Ticket 접수 수인 별도 참고 지표
 * @param reasonCounts 사유별 제외·오류·재시도 건수
 * @param lastErrorCode 마지막 작업 오류 코드
 * @param createdAt 실행 생성 시각
 * @param updatedAt 마지막 상태 갱신 시각
 */
public record AdminPushRunView(
    UUID id,
    UUID campaignId,
    String mode,
    String status,
    LocalDateTime audienceCapturedAt,
    Long targetUserCount,
    Long targetTokenCount,
    long pendingCount,
    long succeededCount,
    long failedCount,
    long excludedCount,
    long unknownCount,
    long ticketAcceptedCount,
    java.util.Map<String, Long> reasonCounts,
    String lastErrorCode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
