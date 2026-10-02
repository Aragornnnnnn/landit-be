// 직접 편지 발송 이력을 관리자에게 읽음 상태 변경 없이 제공한다.

package com.landit.landitbe.feature.mailbox.admin.letter.service;

import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxDirectLetterDetailResponse;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxDirectLetterDetailResponse.Recipient;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxDirectLetterListResponse;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxDirectLetterSummary;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 직접 편지 발송 이력을 관리자에게 제공한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMailboxDirectLetterQueryService {

  private final AdminMailboxLetterRepository letterRepository;
  private final AdminMailboxLetterRecipientRepository recipientRepository;

  /**
   * 직접 편지 발송 목록을 최신순으로 조회한다.
   *
   * @param page 0부터 시작하는 페이지 번호
   * @param size 1~100 사이 페이지 크기
   * @return 발송 건별 요약 페이지
   * @throws ApiException 페이지 조건이 유효하지 않을 때
   */
  public AdminMailboxDirectLetterListResponse getLetters(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
    Page<AdminMailboxDirectLetterSummary> letters =
        letterRepository.findDirectLetters(PageRequest.of(page, size));
    return new AdminMailboxDirectLetterListResponse(
        letters.getContent(), page, size, letters.getTotalElements(), letters.getTotalPages());
  }

  /**
   * 직접 편지의 본문과 수신자별 최초 읽음 정보를 조회한다. 읽음 상태는 바꾸지 않는다.
   *
   * @param letterId 편지 ID
   * @return 탈퇴 수신자를 포함한 발송 상세
   * @throws ApiException 편지가 없거나 발송된 직접 편지가 아닐 때
   */
  public AdminMailboxDirectLetterDetailResponse getLetter(Long letterId) {
    MailboxLetter letter =
        letterRepository
            .findById(letterId)
            .filter(value -> value.getLetterType() == MailboxLetterType.DIRECT)
            .filter(value -> value.getPublicationStatus() == MailboxPublicationStatus.PUBLISHED)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    List<Recipient> recipients =
        recipientRepository.findByLetterIdOrderByUserProfileIdAsc(letterId).stream()
            .map(value -> new Recipient(value.getUserProfileId(), value.getReadAt()))
            .toList();
    return new AdminMailboxDirectLetterDetailResponse(
        letter.getId(),
        letter.getTitle(),
        letter.getBodyText(),
        letter.getPublishedAt(),
        recipients.size(),
        recipients);
  }
}
