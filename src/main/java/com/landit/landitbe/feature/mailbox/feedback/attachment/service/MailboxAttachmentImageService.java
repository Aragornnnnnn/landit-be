// 문의 이미지의 개수·크기·실제 이미지 형식을 검증한다.

package com.landit.landitbe.feature.mailbox.feedback.attachment.service;

import com.landit.landitbe.feature.mailbox.feedback.attachment.dto.MailboxAttachmentImage;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 문의 이미지의 개수·크기·실제 이미지 형식을 검증한다. */
@Service
public class MailboxAttachmentImageService {

  private static final int MAX_FILE_SIZE = 5 * 1024 * 1024;
  private static final long MAX_TOTAL_SIZE = 10L * 1024 * 1024;
  private static final long MAX_PIXELS = 20_000_000;

  /**
   * 모든 파일을 업로드 전에 검증한다. 첨부가 없으면 빈 목록을 반환한다.
   *
   * @param files multipart 첨부 목록. 생략 가능
   * @return 검증된 이미지 목록
   * @throws ApiException 개수·크기·이미지 형식이 올바르지 않거나 파일을 읽을 수 없는 경우
   */
  public List<MailboxAttachmentImage> validate(List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      return List.of();
    }
    if (files.size() > 3) {
      throw invalid("이미지는 최대 3장까지 첨부할 수 있습니다.");
    }
    List<MailboxAttachmentImage> images = new ArrayList<>();
    long totalSize = 0;
    for (MultipartFile file : files) {
      byte[] content = readContent(file);
      totalSize += content.length;
      if (totalSize > MAX_TOTAL_SIZE) {
        throw invalid("첨부 이미지 합계는 10 MiB 이하여야 합니다.");
      }
      images.add(
          new MailboxAttachmentImage(content, validateImage(content, file.getContentType())));
    }
    return List.copyOf(images);
  }

  private byte[] readContent(MultipartFile file) {
    if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
      throw invalid("빈 이미지 또는 5 MiB를 초과한 이미지는 첨부할 수 없습니다.");
    }
    try (var input = file.getInputStream()) {
      byte[] content = input.readNBytes(MAX_FILE_SIZE + 1);
      if (content.length == 0 || content.length > MAX_FILE_SIZE) {
        throw invalid("이미지는 1바이트 이상 5 MiB 이하여야 합니다.");
      }
      return content;
    } catch (IOException exception) {
      throw invalid("첨부 이미지를 읽을 수 없습니다.");
    }
  }

  private String validateImage(byte[] content, String declaredType) {
    try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw invalid("PNG 또는 JPEG 이미지 파일이 필요합니다.");
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        String type = imageContentType(reader.getFormatName());
        if (!type.equals(declaredType)) {
          throw invalid("이미지 내용과 MIME 유형이 일치하지 않습니다.");
        }
        validateDimensions(reader);
        reader.read(0).flush();
        return type;
      } finally {
        reader.dispose();
      }
    } catch (IOException | IllegalArgumentException exception) {
      throw invalid("이미지 파일이 손상됐거나 지원하지 않는 형식입니다.");
    }
  }

  private void validateDimensions(ImageReader reader) throws IOException {
    int width = reader.getWidth(0);
    int height = reader.getHeight(0);
    if (width < 1 || height < 1 || (long) width * height > MAX_PIXELS) {
      throw invalid("이미지 해상도는 2천만 픽셀 이하여야 합니다.");
    }
  }

  private String imageContentType(String format) {
    return switch (format.toLowerCase(Locale.ROOT)) {
      case "jpeg", "jpg" -> "image/jpeg";
      case "png" -> "image/png";
      default -> throw invalid("PNG 또는 JPEG 이미지만 첨부할 수 있습니다.");
    };
  }

  private ApiException invalid(String message) {
    return new ApiException(ErrorCode.VALIDATION_FAILED, message);
  }
}
