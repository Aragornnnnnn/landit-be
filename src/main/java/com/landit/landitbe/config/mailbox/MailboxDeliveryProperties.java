// 프런트 계약 배포 이후에만 직접 편지 발송을 허용하는 설정을 정의한다.

package com.landit.landitbe.config.mailbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 편지 발송 기능의 활성화 설정이다.
 *
 * @param directLetterEnabled DIRECT 렌더링을 지원하는 프런트 배포 확인 후에만 활성화한다
 */
@ConfigurationProperties(prefix = "landit.mailbox-delivery")
public record MailboxDeliveryProperties(boolean directLetterEnabled) {}
