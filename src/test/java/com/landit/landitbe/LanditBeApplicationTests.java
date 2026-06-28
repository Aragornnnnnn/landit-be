// Landit 백엔드 애플리케이션 컨텍스트 부팅을 검증한다.
package com.landit.landitbe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class LanditBeApplicationTests {

	@Test
	void contextLoads() {
	}

}
