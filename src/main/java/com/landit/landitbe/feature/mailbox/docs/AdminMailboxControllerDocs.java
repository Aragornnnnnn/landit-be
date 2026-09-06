// 편지함 어드민 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.mailbox.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackType;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxFeedbackDetailResponse;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxLetterCreateRequest;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxLetterListResponse;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxLetterPatchRequest;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxLetterResponse;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxReplyRequest;
import com.landit.landitbe.feature.mailbox.dto.AdminMailboxReplyResponse;
import com.landit.landitbe.feature.mailbox.service.AdminMailboxService.FeedbackSort;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;

/** 편지함 어드민 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Admin Mailbox", description = "편지함 어드민 API")
public interface AdminMailboxControllerDocs {

  /**
   * 공지·업데이트 목록을 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param type 편지 유형
   * @param publicationStatus 게시 상태
   * @param pinned 상단 고정 여부
   * @return 편지 페이지
   */
  @Operation(summary = "공지·업데이트 목록", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminMailboxLetterListResponse> getLetters(
      int page,
      int size,
      MailboxLetterType type,
      MailboxPublicationStatus publicationStatus,
      Boolean pinned);

  /**
   * 공지·업데이트 초안을 생성한다.
   *
   * @param principal 인증된 관리자
   * @param request 초안 생성 요청
   * @return 생성된 초안
   */
  @Operation(summary = "공지·업데이트 초안 생성", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<AdminMailboxLetterResponse>> createLetter(
      AuthUserPrincipal principal, @Valid AdminMailboxLetterCreateRequest request);

  /**
   * 공지·업데이트를 수정한다.
   *
   * @param principal 인증된 관리자
   * @param letterId 편지 ID
   * @param request 편지 수정 요청
   * @return 수정된 편지
   */
  @Operation(summary = "공지·업데이트 수정", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminMailboxLetterResponse> updateLetter(
      AuthUserPrincipal principal, Long letterId, @Valid AdminMailboxLetterPatchRequest request);

  /**
   * 피드백을 검색·필터링한다.
   *
   * @param keyword 본문 검색어
   * @param type 피드백 유형
   * @param status 처리 상태
   * @param createdFrom 검색 시작일
   * @param createdTo 검색 종료일
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 정렬 방향
   * @return 피드백 페이지
   */
  @Operation(summary = "피드백 검색", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminMailboxFeedbackListResponse> getFeedbacks(
      String keyword,
      UserFeedbackType type,
      UserFeedbackStatus status,
      LocalDate createdFrom,
      LocalDate createdTo,
      int page,
      int size,
      FeedbackSort sort);

  /**
   * 피드백 상세와 최신 답장을 조회한다.
   *
   * @param feedbackId 피드백 ID
   * @return 피드백 상세
   * @throws ApiException 피드백을 찾을 수 없을 때
   */
  @Operation(summary = "피드백 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<AdminMailboxFeedbackDetailResponse> getFeedback(Long feedbackId);

  /**
   * 여러 사용자에게 같은 답장을 보낸다.
   *
   * @param principal 인증된 관리자
   * @param request 일괄 답장 요청
   * @return 일괄 답장 결과
   */
  @Operation(summary = "피드백 일괄 답장", security = @SecurityRequirement(name = "bearerAuth"))
  ResponseEntity<ApiResponse<AdminMailboxReplyResponse>> sendReplies(
      AuthUserPrincipal principal, @Valid AdminMailboxReplyRequest request);
}
