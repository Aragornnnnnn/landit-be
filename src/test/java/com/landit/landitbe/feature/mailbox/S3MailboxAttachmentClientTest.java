// 문의 첨부 S3 요청의 비공개 저장 계약과 실패 응답을 검증한다.

package com.landit.landitbe.feature.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.mailbox.MailboxAttachmentProperties;
import com.landit.landitbe.feature.mailbox.feedback.attachment.client.S3MailboxAttachmentClient;
import com.landit.landitbe.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** 문의 첨부 S3 요청의 비공개 저장 계약과 실패 응답을 검증한다. */
class S3MailboxAttachmentClientTest {

  private final S3Client s3 = mock(S3Client.class);
  private final S3MailboxAttachmentClient client =
      new S3MailboxAttachmentClient(
          s3, new MailboxAttachmentProperties("private-app", "ap-northeast-2"));

  @Test
  void uploadUsesPrivateBucketAndPreservesBytesWithoutPublicAcl() throws Exception {
    byte[] bytes = {1, 2, 3};
    client.upload("mailbox/feedback/key", bytes, "image/png");
    var request = ArgumentCaptor.forClass(PutObjectRequest.class);
    var body = ArgumentCaptor.forClass(RequestBody.class);
    verify(s3).putObject(request.capture(), body.capture());
    assertThat(request.getValue().bucket()).isEqualTo("private-app");
    assertThat(request.getValue().key()).isEqualTo("mailbox/feedback/key");
    assertThat(request.getValue().contentType()).isEqualTo("image/png");
    assertThat(request.getValue().contentLength()).isEqualTo(3);
    assertThat(request.getValue().aclAsString()).isNull();
    assertThat(request.getValue().cacheControl()).isEqualTo("private, no-store");
    try (var input = body.getValue().contentStreamProvider().newStream()) {
      assertThat(input.readAllBytes()).containsExactly(bytes);
    }
  }

  @Test
  void downloadBoundsBytesAndDeleteUsesSamePrivateBucket() {
    when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenReturn(
            ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[] {1, 2}));
    assertThat(client.download("key", 2)).containsExactly((byte) 1, (byte) 2);
    var request = ArgumentCaptor.forClass(GetObjectRequest.class);
    verify(s3).getObjectAsBytes(request.capture());
    assertThat(request.getValue().bucket()).isEqualTo("private-app");
    assertThat(request.getValue().range()).isEqualTo("bytes=0-1");
    assertThatThrownBy(() -> client.download("key", 3)).isInstanceOf(ApiException.class);
    client.delete("key");
    verify(s3).deleteObject(DeleteObjectRequest.builder().bucket("private-app").key("key").build());
  }

  @Test
  void sdkFailureDoesNotExposeProviderDetails() {
    when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenThrow(SdkClientException.create("sensitive-provider-details"));
    assertThatThrownBy(() -> client.download("key", 2))
        .isInstanceOf(ApiException.class)
        .hasMessageNotContaining("sensitive-provider-details");
  }

  @Test
  void unconfiguredBucketFailsBeforeCallingS3() {
    var disabled =
        new S3MailboxAttachmentClient(s3, new MailboxAttachmentProperties("", "ap-northeast-2"));
    assertThatThrownBy(() -> disabled.upload("key", new byte[] {1}, "image/png"))
        .isInstanceOf(ApiException.class);
    verifyNoInteractions(s3);
  }
}
