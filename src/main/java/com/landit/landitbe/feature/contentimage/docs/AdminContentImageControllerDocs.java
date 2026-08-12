// 관리자 콘텐츠 이미지 업로드 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.contentimage.docs;

import com.landit.landitbe.feature.contentimage.dto.AdminContentImagePresignRequest;
import com.landit.landitbe.feature.contentimage.dto.AdminContentImagePresignResponse;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** 관리자 콘텐츠 이미지 업로드 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Admin Content Image", description = "관리자 콘텐츠 이미지 업로드 API")
public interface AdminContentImageControllerDocs {

  /**
   * S3 직접 업로드에 사용할 presigned PUT URL을 발급한다.
   *
   * @param request 이미지 파일 메타데이터
   * @return presigned PUT URL과 CloudFront 조회 정보
   * @throws ApiException 지원하지 않는 형식이거나 요청 크기가 허용 범위를 벗어날 때
   */
  @Operation(summary = "콘텐츠 이미지 업로드 URL 발급", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminContentImagePresignResponse> createPresignedUrl(
      @Valid AdminContentImagePresignRequest request);
}
