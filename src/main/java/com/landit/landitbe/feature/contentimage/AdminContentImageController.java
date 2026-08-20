// 관리자 콘텐츠 이미지 업로드 URL 발급 요청을 처리한다.

package com.landit.landitbe.feature.contentimage;

import com.landit.landitbe.feature.contentimage.docs.AdminContentImageControllerDocs;
import com.landit.landitbe.feature.contentimage.dto.AdminContentImagePresignRequest;
import com.landit.landitbe.feature.contentimage.dto.AdminContentImagePresignResponse;
import com.landit.landitbe.feature.contentimage.service.ContentImageUploadService;
import com.landit.landitbe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 콘텐츠 이미지 업로드 URL 발급 요청을 처리한다. */
@RestController
public class AdminContentImageController implements AdminContentImageControllerDocs {

  private final ContentImageUploadService contentImageUploadService;

  /**
   * 콘텐츠 이미지 업로드 Service를 주입받는다.
   *
   * @param contentImageUploadService 콘텐츠 이미지 업로드 Service
   */
  public AdminContentImageController(ContentImageUploadService contentImageUploadService) {
    this.contentImageUploadService = contentImageUploadService;
  }

  /** {@inheritDoc} */
  @Override
  @PostMapping("/api/v1/admin/content-images/presigned-url")
  public ApiResponse<AdminContentImagePresignResponse> createPresignedUrl(
      @Valid @RequestBody AdminContentImagePresignRequest request) {
    return ApiResponse.success(contentImageUploadService.createPresignedUrl(request));
  }
}
