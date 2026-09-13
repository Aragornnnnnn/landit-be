// 관리자에게 발송 작업의 접수 상태와 제공자 접수 식별자를 제공한다.

package com.landit.landitbe.feature.notification.dto;

import java.util.UUID;

/**
 * 수신 주소를 노출하지 않는 발송 작업 결과다.
 *
 * @param id 작업 ID
 * @param status 처리 상태. ACCEPTED는 제공자 접수이며 메일함 도착을 뜻하지 않는다
 * @param resultCode 처리 결과 코드
 * @param providerMessageId SES 접수 식별자
 */
public record NotificationJobView(
    UUID id, String status, String resultCode, String providerMessageId) {}
