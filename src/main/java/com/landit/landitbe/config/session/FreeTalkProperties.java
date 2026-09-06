// 프리톡 발화 제한시간 설정을 바인딩한다.

package com.landit.landitbe.config.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프리톡 발화 제한시간 설정을 바인딩한다.
 *
 * @param speakingTimeLimitMs 일일 사용자 발화 제한시간 밀리초
 */
@ConfigurationProperties(prefix = "landit.free-talk")
public record FreeTalkProperties(long speakingTimeLimitMs) {}
