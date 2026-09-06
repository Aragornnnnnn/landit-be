// 장기기억 생성 기능의 실행 설정을 바인딩한다.

package com.landit.landitbe.config.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 장기기억 생성 기능의 실행 설정을 바인딩한다.
 *
 * @param writeEnabled 장기기억 생성 작업 등록 허용 여부
 * @param useEnabled 프리톡 세션 시작 장기기억 검색 허용 여부
 */
@ConfigurationProperties(prefix = "landit.memory")
public record MemoryProperties(boolean writeEnabled, boolean useEnabled) {}
