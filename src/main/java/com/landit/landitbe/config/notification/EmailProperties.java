// SES 이메일 발송 설정과 발신 전용 주소를 바인딩한다.

package com.landit.landitbe.config.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이메일 전송 설정이다.
 *
 * @param from 인증한 발신 주소
 * @param configurationSet SES 반송 및 신고 이벤트 설정 이름
 */
@ConfigurationProperties("landit.notification.email")
public record EmailProperties(String from, String configurationSet) {
  /** 생략된 문자열을 빈 값으로 정규화한다. */
  public EmailProperties {
    from = from == null ? "" : from;
    configurationSet = configurationSet == null ? "" : configurationSet;
  }
}
