// 편지함 어드민 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.mailbox.admin.docs;

import com.landit.landitbe.feature.mailbox.admin.feedback.dto.AdminMailboxFeedbackDetailResponse;
import com.landit.landitbe.feature.mailbox.admin.feedback.dto.AdminMailboxFeedbackListResponse;
import com.landit.landitbe.feature.mailbox.admin.feedback.dto.AdminMailboxReplyRequest;
import com.landit.landitbe.feature.mailbox.admin.feedback.dto.AdminMailboxReplyResponse;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxDirectLetterRequest;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxDirectLetterResponse;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxLetterCreateRequest;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxLetterListResponse;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxLetterPatchRequest;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxLetterResponse;
import com.landit.landitbe.feature.mailbox.feedback.domain.MailboxFeedbackSort;
import com.landit.landitbe.feature.mailbox.feedback.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.feedback.domain.UserFeedbackType;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxPublicationStatus;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.response.ApiResponse;
import com.landit.landitbe.shared.security.AuthUserPrincipal;
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
   * 지정한 활성 사용자에게 문의 연결 없이 직접 편지를 발송한다.
   *
   * @param principal 인증된 관리자
   * @param request 수신자 ID 목록과 제목·본문
   * @return 생성된 편지 ID, 수신자 수와 발송 시각
   * @throws ApiException 발송이 비활성화됐거나 중복되거나 유효하지 않은 수신자가 있는 경우
   */
  @Operation(
      summary = "특정 사용자에게 직접 편지 발송",
      description =
          "활성 사용자 1~100명에게 DIRECT 편지를 즉시 발송한다. 푸시는 보내지 않는다. "
              + "중복 ID는 400 INVALID_REQUEST, 잘못된 입력은 400 VALIDATION_FAILED, "
              + "존재하지 않거나 탈퇴한 수신자는 404 RESOURCE_NOT_FOUND다. "
              + "수신자 하나라도 유효하지 않으면 전체 발송을 취소한다. "
              + "같은 요청을 다시 보내면 새 편지가 생성된다. "
              + "발송은 기본 비활성화이며 프런트 DIRECT 지원 후 활성화한다. 비활성 상태는 503이다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "발송 완료")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "503",
      description = "직접 편지 발송 비활성화")
  ResponseEntity<ApiResponse<AdminMailboxDirectLetterResponse>> sendDirectLetter(
      AuthUserPrincipal principal, @Valid AdminMailboxDirectLetterRequest request);

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
      MailboxFeedbackSort sort);

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
