// Landit 백엔드 애플리케이션의 진입점을 정의한다.
package com.landit.landitbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LanditBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(LanditBeApplication.class, args);
	}

}
