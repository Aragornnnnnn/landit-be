// 시나리오의 전체 완료 회차와 대화·피드백 응답을 정의한다.

package com.landit.landitbe.feature.learning.scenario.history.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.scenario.feedback.dto.SessionFeedbackResponse;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 시나리오의 완료 회차를 최신 완료순으로 제공한다.
 *
 * @param scenarioId 시나리오 ID
 * @param sessions 완료 회차 목록. 기록이 없으면 빈 목록
 */
public record ScenarioHistoryResponse(Long scenarioId, List<Session> sessions) {

  /**
   * 한 회차의 대화와 저장된 피드백이다.
   *
   * @param sessionId 학습 세션 ID
   * @param startedAt 시작 시각
   * @param endedAt 완료 시각
   * @param messages 메시지 순서 오름차순의 대화 내역
   * @param feedback 저장된 완료 피드백. 아직 없으면 null
   */
  public record Session(
      Long sessionId,
      LocalDateTime startedAt,
      LocalDateTime endedAt,
      List<Message> messages,
      @JsonInclude(JsonInclude.Include.ALWAYS)
          @Schema(description = "저장된 완료 피드백. 아직 없거나 미완료이면 null이며 조회 시 생성하지 않는다.", nullable = true)
          SessionFeedbackResponse feedback) {}

  /**
   * 채팅 화면에 표시할 저장 당시 메시지다.
   *
   * @param messageId 메시지 ID
   * @param messageSequence 회차 안의 메시지 순서
   * @param turnNumber 대화 턴 번호
   * @param role 발화 주체
   * @param content 발화 본문
   * @param translatedContent 번역
   * @param innerThought 사용자 발화에 대한 상대 역할의 속마음
   * @param innerThoughtType 속마음 유형
   * @param innerThoughtProcessingStatus 속마음 처리 상태. 대상이 아니면 null
   */
  public record Message(
      Long messageId,
      int messageSequence,
      int turnNumber,
      ConversationSpeaker role,
      String content,
      String translatedContent,
      String innerThought,
      InnerThoughtType innerThoughtType,
      ProcessingStatus innerThoughtProcessingStatus) {

    /**
     * 저장 당시 대화 값을 응답으로 변환한다.
     *
     * @param message 저장된 메시지
     * @return 채팅 메시지 응답
     */
    public static Message from(SessionHistoryMessageSnapshot message) {
      return new Message(
          message.id(),
          message.messageSequence(),
          message.turnNumber(),
          message.role(),
          message.content(),
          message.translatedContent(),
          message.innerThought(),
          message.innerThoughtType(),
          message.innerThoughtProcessingStatus());
    }
  }
}
