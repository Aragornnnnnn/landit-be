// 이미지 실제 바이트·해상도 검증과 정확한 용량 경계를 검증한다.

package com.landit.landitbe.feature.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.mailbox.feedback.attachment.service.MailboxAttachmentImageService;
import com.landit.landitbe.shared.exception.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/** 이미지 실제 바이트·해상도 검증과 정확한 용량 경계를 검증한다. */
class MailboxAttachmentImageServiceTest {

  private final MailboxAttachmentImageService service = new MailboxAttachmentImageService();

  @Test
  void exactFileAndTotalSizeLimitsAndThreeImagesAreAccepted() throws Exception {
    byte[] png = png();
    var max = file(Arrays.copyOf(png, 5 * 1024 * 1024));
    assertThat(service.validate(List.of(max, max))).hasSize(2);
    assertThat(service.validate(List.of(file(png), file(png), file(png)))).hasSize(3);
  }

  @Test
  void oversizedDimensionsAreRejectedBeforeAllocatingDecodedPixels() throws Exception {
    byte[] png = png();
    ByteBuffer.wrap(png).putInt(16, 5000).putInt(20, 4001);
    var checksum = new CRC32();
    checksum.update(png, 12, 17);
    ByteBuffer.wrap(png).putInt(29, (int) checksum.getValue());
    assertThatThrownBy(() -> service.validate(List.of(file(png))))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("2천만 픽셀");
  }

  @Test
  void actualStreamLimitDoesNotTrustReportedSize() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getSize()).thenReturn(1L);
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[5 * 1024 * 1024 + 1]));
    assertThatThrownBy(() -> service.validate(List.of(file)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("5 MiB");
  }

  private MockMultipartFile file(byte[] bytes) {
    return new MockMultipartFile("images", "image.png", "image/png", bytes);
  }

  private byte[] png() throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
    return output.toByteArray();
  }
}
