// 활성 수신자에게 문의 연결 없이 직접 편지를 발송하고 감사 기록을 저장한다.

package com.landit.landitbe.feature.mailbox.admin.letter.service;

import com.landit.landitbe.feature.audit.domain.AdminAction;
import com.landit.landitbe.feature.audit.service.AdminAuditService;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxDirectLetterRequest;
import com.landit.landitbe.feature.mailbox.admin.letter.dto.AdminMailboxDirectLetterResponse;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterRecipient;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRepository;
import com.landit.landitbe.feature.profile.service.UserProfileService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 활성 수신자에게 문의 연결 없이 직접 편지를 발송하고 감사 기록을 저장한다. */
@Service
@RequiredArgsConstructor
public class AdminMailboxDirectLetterService {

  private final AdminMailboxLetterRepository letterRepository;
  private final AdminMailboxLetterRecipientRepository recipientRepository;
  private final UserProfileService userProfileService;
  private final AdminAuditService adminAuditService;

  /**
   * 검증된 요청의 수신자 전원에게 동일한 직접 편지를 즉시 발송한다. 푸시는 보내지 않는다.
   *
   * @param adminUserProfileId 작업 관리자 ID
   * @param request Bean Validation을 통과한 발송 요청
   * @return 편지 ID와 수신자 수, 발송 시각
   * @throws ApiException 수신자가 중복되거나 존재하지 않거나 탈퇴한 경우
   */
  @Transactional
  public AdminMailboxDirectLetterResponse sendLetter(
      Long adminUserProfileId, AdminMailboxDirectLetterRequest request) {
    List<Long> recipientIds = requireActiveRecipients(request.userProfileIds());
    MailboxLetter letter = createLetter(request);
    recipientRepository.saveAll(
        recipientIds.stream()
            .map(userId -> new MailboxLetterRecipient(letter.getId(), userId, null))
            .toList());
    adminAuditService.record(
        adminUserProfileId,
        AdminAction.MAILBOX_DIRECT_LETTER_SENT,
        "MAILBOX_LETTER",
        String.valueOf(letter.getId()),
        null,
        "recipientCount=" + recipientIds.size());
    return new AdminMailboxDirectLetterResponse(
        letter.getId(), recipientIds.size(), letter.getPublishedAt());
  }

  private List<Long> requireActiveRecipients(List<Long> requestedIds) {
    if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "수신자 ID가 중복됐습니다.");
    }
    List<Long> activeIds = userProfileService.findActiveIdsForUpdate(requestedIds).ids();
    if (activeIds.size() != requestedIds.size()) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "활성 수신자를 찾을 수 없습니다.");
    }
    return activeIds;
  }

  private MailboxLetter createLetter(AdminMailboxDirectLetterRequest request) {
    return letterRepository.save(
        new MailboxLetter(
            MailboxLetterType.DIRECT,
            request.title(),
            null,
            request.bodyText(),
            request.bodyText(),
            MailboxPublicationStatus.PUBLISHED,
            false,
            LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)));
  }
}
