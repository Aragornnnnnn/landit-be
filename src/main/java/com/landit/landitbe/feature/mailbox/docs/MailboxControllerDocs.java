// 편지함 사용자 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.mailbox.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.mailbox.dto.MailboxFeedbackSubmitRequest;
import com.landit.landitbe.feature.mailbox.dto.MailboxReceivedDetailResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxReceivedListResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxSentFeedbackDetailResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxSentFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.dto.MailboxUnreadCountResponse;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** 편지함 사용자 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Mailbox", description = "편지함 API")
public interface MailboxControllerDocs {

  /**
   * 인증된 사용자의 피드백을 등록한다.
   *
   * @param principal 인증된 사용자
   * @param request 피드백 등록 요청
   * @return 생성 결과를 담은 HTTP 응답
   */
  @Operation(
      summary = "피드백 등록",
      description = "문의·버그 제보·기능 제안·응원 메시지를 등록한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "등록 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 값 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ResponseEntity<ApiResponse<Void>> submitFeedback(
      AuthUserPrincipal principal, MailboxFeedbackSubmitRequest request);

  /**
   * 인증된 사용자의 피드백 목록을 커서로 조회한다.
   *
   * @param principal 인증된 사용자
   * @param cursor 다음 페이지 커서
   * @param size 페이지 크기
   * @return 보낸 피드백 목록을 담은 HTTP 응답
   */
  @Operation(
      summary = "보낸 피드백 목록 조회",
      description = "인증된 사용자가 등록한 피드백을 최신순으로 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "커서 또는 페이지 크기 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ResponseEntity<ApiResponse<MailboxSentFeedbackListResponse>> getSentFeedbacks(
      AuthUserPrincipal principal,
      @Parameter(description = "다음 페이지 조회용 커서") String cursor,
      @Parameter(description = "페이지 크기 (1~100)", example = "20") int size);

  /**
   * 인증된 사용자의 피드백 상세를 조회한다.
   *
   * @param principal 인증된 사용자
   * @param feedbackId 피드백 ID
   * @return 보낸 피드백 상세를 담은 HTTP 응답
   */
  @Operation(
      summary = "보낸 피드백 상세 조회",
      description = "등록한 피드백의 본문과 처리 상태를 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "피드백 없음 또는 접근 불가")
  })
  ResponseEntity<ApiResponse<MailboxSentFeedbackDetailResponse>> getSentFeedback(
      AuthUserPrincipal principal, long feedbackId);

  /**
   * 인증된 사용자가 볼 수 있는 받은 편지 목록을 커서로 조회한다.
   *
   * @param principal 인증된 사용자
   * @param cursor 다음 페이지 커서
   * @param size 페이지 크기
   * @return 받은 편지 목록을 담은 HTTP 응답
   */
  @Operation(
      summary = "받은 편지 목록 조회",
      description = "공지·업데이트와 답장을 최신순으로 조회한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "커서 또는 페이지 크기 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ResponseEntity<ApiResponse<MailboxReceivedListResponse>> getReceivedLetters(
      AuthUserPrincipal principal,
      @Parameter(description = "다음 페이지 조회용 커서") String cursor,
      @Parameter(description = "페이지 크기 (1~100)", example = "20") int size);

  /**
   * 인증된 사용자가 볼 수 있는 받은 편지 상세를 조회하고 읽음 처리한다.
   *
   * @param principal 인증된 사용자
   * @param letterId 편지 ID
   * @return 받은 편지 상세를 담은 HTTP 응답
   */
  @Operation(
      summary = "받은 편지 상세 조회",
      description = "공지·업데이트와 답장을 조회하고 읽음 처리한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "편지 없음 또는 접근 불가")
  })
  ResponseEntity<ApiResponse<MailboxReceivedDetailResponse>> getReceivedLetter(
      AuthUserPrincipal principal, long letterId);

  /**
   * 인증된 사용자의 편지함 안 읽은 편지 개수를 조회한다.
   *
   * @param principal 인증된 사용자
   * @return 안 읽은 편지 개수를 담은 HTTP 응답
   */
  @Operation(
      summary = "안 읽은 편지 개수 조회",
      description = "읽지 않은 편지 개수를 반환한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패")
  })
  ResponseEntity<ApiResponse<MailboxUnreadCountResponse>> getUnreadCount(
      AuthUserPrincipal principal);
}
