// 턴 교정을 다시 요청할 입력이 첫 시도와 같은 재료로 조립되는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.learning.freetalk.topic.domain.FreeTalkTopic;
import com.landit.landitbe.feature.learning.freetalk.topic.repository.FreeTalkTopicRepository;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.retrieval.service.FreeTalkMemoryRetrievalService;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.Locale;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 턴 교정을 다시 요청할 입력이 첫 시도와 같은 재료로 조립되는지 검증한다. */
class FreeTalkCorrectionRequestServiceTest {

  private static final long HISTORY_ID = 3100L;
  private static final long LEARNING_SESSION_ID = 800L;
  private static final long FREE_TALK_SESSION_ID = 30L;
  private static final long USER_ID = 1207L;
  private static final long TOPIC_ID = 12L;

  private final ConversationMessageService conversationMessageService =
      mock(ConversationMessageService.class);
  private final SessionHistoryService sessionHistoryService = mock(SessionHistoryService.class);
  private final FreeTalkSessionRepository freeTalkSessionRepository =
      mock(FreeTalkSessionRepository.class);
  private final FreeTalkTopicRepository freeTalkTopicRepository =
      mock(FreeTalkTopicRepository.class);
  private final FreeTalkMemoryRetrievalService memoryRetrievalService =
      mock(FreeTalkMemoryRetrievalService.class);
  private final FreeTalkCorrectionRequestService service =
      new FreeTalkCorrectionRequestService(
          conversationMessageService,
          sessionHistoryService,
          freeTalkSessionRepository,
          freeTalkTopicRepository,
          memoryRetrievalService);

  private final FreeTalkSession session = mock(FreeTalkSession.class);
  private final SessionHistoryMessageSnapshot opening = message(55018L, 1, 1, "AI", "Hi!");
  private final SessionHistoryMessageSnapshot firstUser =
      message(55019L, 2, 1, "USER", "I go to a gym.");
  private final SessionHistoryMessageSnapshot firstReply = message(55020L, 3, 2, "AI", "Nice!");
  private final SessionHistoryMessageSnapshot secondUser =
      message(55021L, 4, 2, "USER", "Yesterday I go again.");

  @BeforeEach
  void stubSession() {
    SessionHistorySnapshot history = mock(SessionHistorySnapshot.class);
    when(history.getLearningSessionId()).thenReturn(LEARNING_SESSION_ID);
    when(history.getUserProfileId()).thenReturn(USER_ID);
    when(history.getTargetLocale()).thenReturn(Locale.EN);
    when(history.getBaseLocale()).thenReturn(Locale.KR);
    when(sessionHistoryService.findHistory(HISTORY_ID)).thenReturn(Optional.of(history));
    when(session.getId()).thenReturn(FREE_TALK_SESSION_ID);
    when(session.getCharacterId()).thenReturn("chloe");
    when(session.getTitle()).thenReturn("헬스장 이야기");
    when(freeTalkSessionRepository.findByLearningSessionId(LEARNING_SESSION_ID))
        .thenReturn(Optional.of(session));
    when(conversationMessageService.findAll(HISTORY_ID))
        .thenReturn(List.of(opening, firstUser, firstReply, secondUser));
    when(conversationMessageService.findMessage(55019L)).thenReturn(Optional.of(firstUser));
  }

  @DisplayName("그 발화까지의 대화만 싣고, 발화의 턴 번호와 세션의 언어·캐릭터·검색된 기억을 첫 시도와 같게 채운다.")
  @Test
  void rebuildsRequestWithHistoryThroughTheSubmittedMessage() {
    List<AiFreeTalkMemoryContext> memories =
        List.of(new AiFreeTalkMemoryContext(9012L, ConversationMemoryType.PROFILE, "헬스장에 다닌다."));
    when(memoryRetrievalService.retrievedContexts(FREE_TALK_SESSION_ID, USER_ID))
        .thenReturn(memories);

    AiFreeTalkInnerThoughtRequest request = service.rebuild(55019L).orElseThrow();

    assertThat(request.sessionId()).isEqualTo(FREE_TALK_SESSION_ID);
    assertThat(request.characterId()).isEqualTo("chloe");
    assertThat(request.submittedMessageId()).isEqualTo(55019L);
    assertThat(request.submittedTurnNumber()).isEqualTo(1);
    assertThat(request.targetLocale()).isEqualTo("EN");
    assertThat(request.baseLocale()).isEqualTo("KR");
    assertThat(request.memoryContext()).isEqualTo(memories);
    // 그 뒤에 이어진 AI 답과 다음 발화는 첫 시도 때 없던 입력이라 싣지 않는다. 마지막은 교정 대상 발화여야 AI 서버가 받는다.
    assertThat(request.conversationHistory())
        .containsExactly(
            new AiConversationHistoryMessage(55018L, 1, "AI", "Hi!", null, null, 1),
            new AiConversationHistoryMessage(55019L, 1, "USER", "I go to a gym.", null, null, 2));
  }

  @DisplayName("주제로 시작한 세션은 주제의 이름과 설명을 함께 싣는다.")
  @Test
  void sendsFullTopicWhenSessionHasTopic() {
    FreeTalkTopic topic = mock(FreeTalkTopic.class);
    when(topic.getId()).thenReturn(TOPIC_ID);
    when(topic.getDisplayName()).thenReturn("운동");
    when(topic.getPromptDescription()).thenReturn("요즘 하는 운동 이야기");
    when(session.getTopicId()).thenReturn(TOPIC_ID);
    when(freeTalkTopicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));

    assertThat(service.rebuild(55019L).orElseThrow().topic())
        .isEqualTo(new AiFreeTalkTopic(TOPIC_ID, "운동", "요즘 하는 운동 이야기"));
  }

  @DisplayName("주제 없이 시작했거나 주제가 지워진 세션은 세션 제목만 싣는다.")
  @Test
  void sendsTitleOnlyWhenTopicIsMissing() {
    assertThat(service.rebuild(55019L).orElseThrow().topic())
        .isEqualTo(new AiFreeTalkTopic(null, "헬스장 이야기", null));

    when(session.getTopicId()).thenReturn(TOPIC_ID);
    when(freeTalkTopicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

    assertThat(service.rebuild(55019L).orElseThrow().topic())
        .isEqualTo(new AiFreeTalkTopic(null, "헬스장 이야기", null));
  }

  @DisplayName("발화가 없거나 사용자 발화가 아니거나 세션을 찾을 수 없으면 다시 보낼 요청을 만들지 않는다.")
  @Test
  void returnsEmptyWhenTheRequestCannotBeRebuilt() {
    when(conversationMessageService.findMessage(1L)).thenReturn(Optional.empty());
    when(conversationMessageService.findMessage(55020L)).thenReturn(Optional.of(firstReply));

    assertThat(service.rebuild(1L)).isEmpty();
    assertThat(service.rebuild(55020L)).isEmpty();

    when(freeTalkSessionRepository.findByLearningSessionId(LEARNING_SESSION_ID))
        .thenReturn(Optional.empty());

    assertThat(service.rebuild(55019L)).isEmpty();
  }

  private static SessionHistoryMessageSnapshot message(
      long id, int sequence, int turnNumber, String role, String content) {
    SessionHistoryMessageSnapshot message = mock(SessionHistoryMessageSnapshot.class);
    when(message.getId()).thenReturn(id);
    when(message.getSessionHistoryId()).thenReturn(HISTORY_ID);
    when(message.getMessageSequence()).thenReturn(sequence);
    when(message.getTurnNumber()).thenReturn(turnNumber);
    when(message.getRole()).thenReturn(ConversationSpeaker.valueOf(role));
    when(message.getContent()).thenReturn(content);
    return message;
  }
}
