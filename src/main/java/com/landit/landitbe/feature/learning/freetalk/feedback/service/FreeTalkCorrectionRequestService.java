// 이미 저장된 대화 기록으로 턴 교정을 다시 요청할 AI 입력을 조립한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.learning.freetalk.topic.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.memory.retrieval.service.FreeTalkMemoryRetrievalService;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 턴 교정을 다시 시도할 때 쓸 속마음·교정 요청을 저장된 기록에서 다시 만든다.
 *
 * <p>첫 시도의 요청을 통째로 저장해 두지 않는다. 대화 기록을 발화마다 복제해 저장하지 않기 위함이다. 대신 같은 재료를 같은 규칙으로 다시 읽는다: 그 발화까지의 대화,
 * 세션의 주제, 세션에서 검색한 장기기억.
 *
 * <p>그래서 첫 시도와 글자 그대로 같지는 않을 수 있다. 대화·언어·캐릭터·턴 번호는 같지만, 첫 시도 뒤에 세션에 더해진 것은 함께 실린다. 주제 없이 시작한 세션의
 * 제목은 세션 도중에 지어지고, 사용자가 먼저 말을 건 세션의 첫 턴은 그 턴에서 검색한 기억이 첫 시도 때는 아직 없었다. 둘 다 교정에 문맥을 더하는 쪽이라 받아들인다.
 */
@RequiredArgsConstructor
@Service
public class FreeTalkCorrectionRequestService {

  private final ConversationMessageService conversationMessageService;
  private final SessionHistoryService sessionHistoryService;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final FreeTalkTopicRepository freeTalkTopicRepository;
  private final FreeTalkMemoryRetrievalService memoryRetrievalService;
  private final FreeTalkWatchPatternService watchPatternService;

  /**
   * 교정 대상 발화의 AI 요청을 다시 조립한다.
   *
   * @param messageId 교정 대상 사용자 발화 ID
   * @return 다시 보낼 요청. 발화·대화 기록·프리톡 세션을 찾을 수 없거나 사용자 발화가 아니면 비어 있다
   */
  @Transactional(readOnly = true)
  public Optional<AiFreeTalkInnerThoughtRequest> rebuild(long messageId) {
    Optional<SessionHistoryMessageSnapshot> message =
        conversationMessageService
            .findMessage(messageId)
            .filter(FreeTalkCorrectionRequestService::isUser);
    if (message.isEmpty()) {
      return Optional.empty();
    }
    Optional<SessionHistorySnapshot> history =
        sessionHistoryService.findHistory(message.get().getSessionHistoryId());
    Optional<FreeTalkSession> session =
        history.flatMap(
            found ->
                freeTalkSessionRepository.findByLearningSessionId(found.getLearningSessionId()));
    if (session.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(request(message.get(), history.get(), session.get()));
  }

  private AiFreeTalkInnerThoughtRequest request(
      SessionHistoryMessageSnapshot message,
      SessionHistorySnapshot history,
      FreeTalkSession session) {
    return new AiFreeTalkInnerThoughtRequest(
        session.getId(),
        session.getCharacterId(),
        message.getId(),
        message.getTurnNumber(),
        history.getTargetLocale().name(),
        history.getBaseLocale().name(),
        topic(session),
        historyThrough(message),
        memoryRetrievalService.retrievedContexts(session.getId(), history.getUserProfileId()),
        // 직전 세션은 끝난 세션이라 첫 시도와 같은 값이 나온다.
        watchPatternService.watchPatterns(session.getLearningSessionId()));
  }

  // AI 서버는 제출한 발화가 이력의 마지막이어야 받는다. 그 뒤에 이어진 대화는 첫 시도 때 없던 입력이라 넣지 않는다.
  private List<AiConversationHistoryMessage> historyThrough(SessionHistoryMessageSnapshot message) {
    return conversationMessageService.findAll(message.getSessionHistoryId()).stream()
        .filter(candidate -> candidate.getMessageSequence() <= message.getMessageSequence())
        .map(
            candidate ->
                new AiConversationHistoryMessage(
                    candidate.getId(),
                    candidate.getTurnNumber(),
                    candidate.getRole().name(),
                    candidate.getContent(),
                    candidate.getTranslatedContent()))
        .toList();
  }

  // 발화를 처리할 때와 같은 규칙이다. 주제 없이 시작했거나 주제가 지워졌으면 세션 제목만 보낸다.
  private AiFreeTalkTopic topic(FreeTalkSession session) {
    if (session.getTopicId() == null) {
      return new AiFreeTalkTopic(null, session.getTitle(), null);
    }
    return freeTalkTopicRepository
        .findById(session.getTopicId())
        .map(
            topic ->
                new AiFreeTalkTopic(
                    topic.getId(), topic.getDisplayName(), topic.getPromptDescription()))
        .orElseGet(() -> new AiFreeTalkTopic(null, session.getTitle(), null));
  }

  private static boolean isUser(SessionHistoryMessageSnapshot message) {
    return message.getRole() == ConversationSpeaker.USER;
  }
}
