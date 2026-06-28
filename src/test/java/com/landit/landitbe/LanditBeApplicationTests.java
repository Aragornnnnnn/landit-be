// Landit 백엔드 애플리케이션 컨텍스트 부팅을 검증한다.
package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class LanditBeApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void applicationTimeZoneUsesAsiaSeoul() {
		assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Seoul");
	}

}
