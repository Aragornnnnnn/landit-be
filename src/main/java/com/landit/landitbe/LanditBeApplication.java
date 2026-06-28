// Landit 백엔드 애플리케이션의 진입점을 정의한다.
package com.landit.landitbe;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LanditBeApplication {

	private static final String APPLICATION_TIME_ZONE = "Asia/Seoul";

	public static void main(String[] args) {
		SpringApplication.run(LanditBeApplication.class, args);
	}

	@PostConstruct
	void setApplicationTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_TIME_ZONE));
	}

}
