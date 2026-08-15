// 콘텐츠 이미지 업로드 URL 발급 외부 의존성의 경계를 정의한다.

package com.landit.landitbe.feature.contentimage.client;

import java.net.URI;

/** 콘텐츠 이미지 업로드 URL을 외부 저장소에서 발급한다. */
public interface ContentImageUploadClient {

  /**
   * 지정한 업로드 조건으로 presigned URL을 발급한다.
   *
   * @param command 업로드 서명 조건
   * @return presigned PUT URL
   */
  URI presign(ContentImageUploadCommand command);
}
