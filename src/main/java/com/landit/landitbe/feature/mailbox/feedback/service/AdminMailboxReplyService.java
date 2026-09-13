// 문의 일괄 답장과 처리 상태·수신자 저장을 하나의 트랜잭션에서 처리한다.

package com.landit.landitbe.feature.mailbox.feedback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.audit.domain.AdminAction;
import com.landit.landitbe.feature.audit.service.AdminAuditService;
import com.landit.landitbe.feature.mailbox.feedback.domain.MailboxFeedback;
import com.landit.landitbe.feature.mailbox.feedback.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.feedback.dto.AdminMailboxReplyRequest;
import com.landit.landitbe.feature.mailbox.feedback.dto.AdminMailboxReplyResponse;
import com.landit.landitbe.feature.mailbox.feedback.event.MailboxReplyCreatedEvent;
import com.landit.landitbe.feature.mailbox.feedback.repository.AdminMailboxFeedbackRepository;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterRecipient;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.letter.domain.MailboxPublicationStatus;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRecipientRepository;
import com.landit.landitbe.feature.mailbox.letter.repository.AdminMailboxLetterRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 문의 일괄 답장과 처리 상태·수신자 저장을 하나의 트랜잭션에서 처리한다. */
@Service
@RequiredArgsConstructor
public class AdminMailboxReplyService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final AdminMailboxLetterRepository letterRepository;
  private final AdminMailboxFeedbackRepository feedbackRepository;
  private final AdminMailboxLetterRecipientRepository recipientRepository;
  private final AdminAuditService adminAuditService;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * 여러 사용자에게 같은 답장을 보내고 선택 피드백을 처리 완료한다.
   *
   * @param adminUserProfileId 작업 관리자 ID
   * @param request 일괄 답장 요청
   * @return 일괄 답장 처리 결과
   * @throws ApiException 피드백 ID가 없거나 요청 값이 올바르지 않을 때
   */
  @Transactional
  public AdminMailboxReplyResponse sendReplies(
      Long adminUserProfileId, AdminMailboxReplyRequest request) {
    List<MailboxFeedback> feedbacks = findFeedbacksForReply(request.feedbackIds());
    MailboxLetter reply = createReply(request);
    Map<Long, List<MailboxFeedback>> feedbacksByUser = groupFeedbacksByUser(feedbacks);
    Map<Long, MailboxFeedback> representatives = findRepresentatives(feedbacksByUser);
    int completedFeedbackCount = completePendingFeedbacks(feedbacksByUser, representatives);
    List<MailboxLetterRecipient> recipients = createRecipients(reply.getId(), representatives);
    recipientRepository.saveAll(recipients);

    List<Long> representativeFeedbackIds =
        representatives.values().stream().map(MailboxFeedback::getId).toList();
    adminAuditService.record(
        adminUserProfileId,
        AdminAction.MAILBOX_REPLY_SENT,
        "MAILBOX_REPLY_BATCH",
        String.valueOf(reply.getId()),
        null,
        "recipientCount=%d,completedFeedbackCount=%d,representativeFeedbackIds=%s"
            .formatted(recipients.size(), completedFeedbackCount, representativeFeedbackIds));
    applicationEventPublisher.publishEvent(
        new MailboxReplyCreatedEvent(
            reply.getId(),
            recipients.stream().map(MailboxLetterRecipient::getUserProfileId).toList(),
            reply.getTitle()));
    return new AdminMailboxReplyResponse(
        reply.getId(), recipients.size(), completedFeedbackCount, representativeFeedbackIds);
  }

  private JsonNode toJsonNode(Object contentBlocks) {
    return OBJECT_MAPPER.valueToTree(contentBlocks);
  }

  private void validateContent(String title, JsonNode contentBlocks, String preview) {
    if (title == null || title.isBlank() || preview == null || preview.isBlank()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
    if (contentBlocks == null || !contentBlocks.isArray() || contentBlocks.isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "본문 블록은 비어 있지 않은 배열이어야 합니다.");
    }
  }

  private List<Long> uniqueFeedbackIds(List<Long> feedbackIds) {
    if (feedbackIds == null
        || feedbackIds.isEmpty()
        || feedbackIds.stream().anyMatch(id -> id == null)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
    Set<Long> uniqueIds = new LinkedHashSet<>(feedbackIds);
    if (uniqueIds.size() != feedbackIds.size()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "피드백 ID가 중복됐습니다.");
    }
    return uniqueIds.stream().sorted().toList();
  }

  private List<MailboxFeedback> findFeedbacksForReply(List<Long> requestedFeedbackIds) {
    List<Long> feedbackIds = uniqueFeedbackIds(requestedFeedbackIds);
    List<MailboxFeedback> feedbacks = feedbackRepository.findAllByIdInOrderByIdAsc(feedbackIds);
    if (feedbacks.size() != feedbackIds.size()) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "피드백을 찾을 수 없습니다.");
    }
    return feedbacks;
  }

  private MailboxLetter createReply(AdminMailboxReplyRequest request) {
    LocalDateTime publishedAt = LocalDateTime.now();
    return letterRepository.save(
        new MailboxLetter(
            MailboxLetterType.REPLY,
            request.title(),
            null,
            request.bodyText(),
            request.bodyText(),
            MailboxPublicationStatus.PUBLISHED,
            false,
            publishedAt));
  }

  private Map<Long, List<MailboxFeedback>> groupFeedbacksByUser(List<MailboxFeedback> feedbacks) {
    return feedbacks.stream()
        .collect(
            Collectors.groupingBy(
                MailboxFeedback::getUserProfileId, LinkedHashMap::new, Collectors.toList()));
  }

  /** 사용자별로 작성 시각과 ID가 가장 작은 피드백을 대표 건으로 선택한다. */
  private Map<Long, MailboxFeedback> findRepresentatives(
      Map<Long, List<MailboxFeedback>> feedbacksByUser) {
    Map<Long, MailboxFeedback> representatives = new LinkedHashMap<>();
    feedbacksByUser.forEach(
        (userProfileId, feedbacks) ->
            representatives.put(userProfileId, findRepresentative(feedbacks)));
    return representatives;
  }

  /** 이미 완료된 피드백은 유지하고 대기 중인 피드백만 사용자별 대표 건과 함께 완료한다. */
  private int completePendingFeedbacks(
      Map<Long, List<MailboxFeedback>> feedbacksByUser,
      Map<Long, MailboxFeedback> representatives) {
    int completedFeedbackCount = 0;
    for (Map.Entry<Long, List<MailboxFeedback>> entry : feedbacksByUser.entrySet()) {
      MailboxFeedback representative = representatives.get(entry.getKey());
      for (MailboxFeedback feedback : entry.getValue()) {
        if (feedback.getProcessingStatus() == UserFeedbackStatus.PENDING) {
          feedback.complete(feedback.equals(representative) ? null : representative.getId());
          completedFeedbackCount++;
        }
      }
    }
    return completedFeedbackCount;
  }

  private List<MailboxLetterRecipient> createRecipients(
      Long replyId, Map<Long, MailboxFeedback> representatives) {
    return representatives.entrySet().stream()
        .map(entry -> new MailboxLetterRecipient(replyId, entry.getKey(), entry.getValue().getId()))
        .toList();
  }

  private MailboxFeedback findRepresentative(Collection<MailboxFeedback> feedbacks) {
    return feedbacks.stream()
        .min(
            Comparator.comparing(MailboxFeedback::getCreatedAt)
                .thenComparing(MailboxFeedback::getId))
        .orElseThrow();
  }
}
