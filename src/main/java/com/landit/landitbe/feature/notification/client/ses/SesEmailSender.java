// AWS SES로 일반 텍스트 이메일을 보내고 접수 여부를 분류한다.

package com.landit.landitbe.feature.notification.client.ses;

import com.landit.landitbe.config.notification.EmailProperties;
import com.landit.landitbe.feature.notification.client.EmailSendResult;
import com.landit.landitbe.feature.notification.client.EmailSendResult.Status;
import com.landit.landitbe.feature.notification.client.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/** IAM 역할로 인증하고 Reply-To 없이 발신 전용 이메일을 보낸다. */
@Component
@RequiredArgsConstructor
public class SesEmailSender implements EmailSender {
  private final SesV2Client client;
  private final EmailProperties properties;

  /** {@inheritDoc} */
  @Override
  public EmailSendResult send(String recipient, String subject, String body) {
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
                                .body(b -> b.text(t -> t.data(body).charset("UTF-8")))));
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
}
