// Landit 백엔드 애플리케이션 컨텍스트 부팅을 검증한다.
package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.session.infrastructure.ai.RemoteAiConversationClient;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "landit.ai.client-mode=remote")
class LanditBeApplicationTests {

	@Autowired
	private RemoteAiConversationClient remoteAiConversationClient;

	@Test
	void remoteAiClientModeLoadsApplicationContext() {
		assertThat(remoteAiConversationClient).isNotNull();
	}

	@Test
	void applicationTimeZoneUsesAsiaSeoul() {
		assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Seoul");
	}

}
