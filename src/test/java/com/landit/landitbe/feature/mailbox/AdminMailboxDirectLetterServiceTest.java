// 직접 편지 발송이 기본 비활성 상태에서 부수 효과 없이 차단되는지 검증한다.

package com.landit.landitbe.feature.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.landit.landitbe.config.mailbox.MailboxDeliveryProperties;
import com.landit.landitbe.feature.audit.service.AdminAuditService;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxDirectLetterRequest;
import com.landit.landitbe.feature.mailbox.admin.letter.service.AdminMailboxDirectLetterService;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AdminMailboxDirectLetterServiceTest {

  @DisplayName("직접 편지는 설정이 없으면 프로필 잠금·편지 저장·감사 기록 전에 차단된다.")
  @Test
  void defaultConfigurationBlocksSendingWithoutSideEffects() {
    new ApplicationContextRunner()
        .withUserConfiguration(DeliveryConfiguration.class)
        .run(
            context -> {
              MailboxDeliveryProperties properties =
                  context.getBean(MailboxDeliveryProperties.class);
              assertThat(properties.directLetterEnabled()).isFalse();
              assertSendingDisabled(properties);
            });
  }

  private void assertSendingDisabled(MailboxDeliveryProperties properties) {
    AdminMailboxLetterRepository letters = mock(AdminMailboxLetterRepository.class);
    AdminMailboxLetterRecipientRepository recipients =
        mock(AdminMailboxLetterRecipientRepository.class);
    UserProfileService profiles = mock(UserProfileService.class);
    AdminAuditService audit = mock(AdminAuditService.class);
    AdminMailboxDirectLetterService service =
        new AdminMailboxDirectLetterService(letters, recipients, profiles, audit, properties);

    assertThatThrownBy(
            () ->
                service.sendLetter(
                    1L, new AdminMailboxDirectLetterRequest(List.of(2L), "제목", "본문")))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE));
    verifyNoInteractions(letters, recipients, profiles, audit);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(MailboxDeliveryProperties.class)
  static class DeliveryConfiguration {}
}
