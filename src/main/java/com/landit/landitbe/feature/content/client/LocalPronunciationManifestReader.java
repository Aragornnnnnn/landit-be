// 클래스패스 리소스에서 발음 평가 자산 매니페스트를 읽는 로컬·테스트용 구현이다.

package com.landit.landitbe.feature.content.client;

import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 클래스패스 리소스에서 발음 평가 자산 매니페스트를 읽는 로컬·테스트용 구현이다.
 *
 * <p>{@code classpath:pronunciation-manifests/{manifestKey}} 경로의 파일을 읽는다. AWS 자격증명 없이 임포트 플로우를 실행할
 * 수 있게 한다.
 */
@Component
@ConditionalOnProperty(
    prefix = "landit.pronunciation-asset",
    name = "manifest-mode",
    havingValue = "local")
public class LocalPronunciationManifestReader implements PronunciationManifestReader {

  private static final String RESOURCE_PREFIX = "/pronunciation-manifests/";

  /** {@inheritDoc} */
  @Override
  public String read(String manifestKey) {
    try (InputStream stream = getClass().getResourceAsStream(RESOURCE_PREFIX + manifestKey)) {
      if (stream == null) {
        throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }
}
