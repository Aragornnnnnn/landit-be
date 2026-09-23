// 이메일 발송 경계에서 수신 주소의 형식과 길이를 검증한다.

package com.landit.landitbe.feature.notification.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이메일 발송에 사용할 수신 주소다.
 *
 * @param recipient 수신 이메일 주소
 */
public record EmailRecipient(@NotBlank @Email @Size(max = 255) String recipient) {}
