// 관리자가 직접 지정한 테스트 이메일 수신 주소를 검증한다.

package com.landit.landitbe.feature.notification.job.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이메일 테스트 요청이다. 임의 본문은 받지 않는다.
 *
 * @param recipient 테스트 메일을 받을 주소
 */
public record AdminEmailTestRequest(@NotBlank @Email @Size(max = 255) String recipient) {}
