// AWS SES로 텍스트와 이미지가 포함된 HTML 이메일을 보내고 접수 여부를 분류한다.

package com.landit.landitbe.feature.notification.client.ses;

import com.landit.landitbe.config.notification.EmailProperties;
import com.landit.landitbe.feature.notification.client.EmailSendResult;
import com.landit.landitbe.feature.notification.client.EmailSendResult.Status;
import com.landit.landitbe.feature.notification.client.EmailSender;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Attachment;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/** IAM 역할로 인증하고 Reply-To 없이 발신 전용 이메일을 보낸다. */
@Component
@RequiredArgsConstructor
public class SesEmailSender implements EmailSender {
  private static final Attachment BANNER = loadBanner();

  private final SesV2Client client;
  private final EmailProperties properties;

  /** {@inheritDoc} */
  @Override
  public EmailSendResult send(String recipient, String subject, String body, String html) {
    if (properties.from().isBlank()) {
      throw new IllegalStateException("이메일 전송이 설정되지 않았습니다.");
    }
    var builder =
        SendEmailRequest.builder()
            .fromEmailAddress(properties.from())
            .destination(d -> d.toAddresses(recipient))
            .content(
                c ->
                    c.simple(
                        m ->
                            m.subject(s -> s.data(subject).charset("UTF-8"))
                                .body(
                                    b ->
                                        b.text(t -> t.data(body).charset("UTF-8"))
                                            .html(h -> h.data(html).charset("UTF-8")))
                                .attachments(BANNER)));
    if (!properties.configurationSet().isBlank()) {
      builder.configurationSetName(properties.configurationSet());
    }
    try {
      return new EmailSendResult(Status.ACCEPTED, client.sendEmail(builder.build()).messageId());
    } catch (SesV2Exception exception) {
      // SES가 명시적으로 거절한 요청만 안전한 재시도로 분류한다. 응답 본문은 수신 주소를 포함할 수 있다.
      Status status =
          exception.statusCode() == 429
              ? Status.RETRYABLE
              : exception.statusCode() >= 500 ? Status.UNKNOWN : Status.FAILED;
      return new EmailSendResult(status, null);
    } catch (SdkClientException exception) {
      return new EmailSendResult(Status.UNKNOWN, null);
    }
  }

  private static Attachment loadBanner() {
    try (var input = new ClassPathResource("email/landit-banner.png").getInputStream()) {
      return Attachment.builder()
          .fileName("landit-banner.png")
          .contentId("landit-banner")
          .contentType("image/png")
          .contentDisposition("INLINE")
          .contentTransferEncoding("BASE64")
          .rawContent(SdkBytes.fromInputStream(input))
          .build();
    } catch (IOException exception) {
      throw new UncheckedIOException("이메일 배너 이미지를 읽을 수 없습니다.", exception);
    }
  }
}
