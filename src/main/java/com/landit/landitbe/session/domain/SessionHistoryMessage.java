// 세션 히스토리에 남길 AI와 사용자 메시지를 저장한다.
package com.landit.landitbe.session.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.common.domain.BaseTimeEntity;
import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.common.domain.InnerThoughtType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "session_history_message")
public class SessionHistoryMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_history_id", nullable = false)
    private Long sessionHistoryId;

    @Column(name = "message_sequence", nullable = false)
    private int messageSequence;

    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationSpeaker role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "translated_content", columnDefinition = "text")
    private String translatedContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 20)
    private SessionMessageInputType inputType;

    @Column(name = "inner_thought", columnDefinition = "text")
    private String innerThought;

    @Enumerated(EnumType.STRING)
    @Column(name = "inner_thought_type", length = 20)
    private InnerThoughtType innerThoughtType;

    @Column(name = "pronunciation_score")
    private Integer pronunciationScore;

    @Column(name = "intonation_score")
    private Integer intonationScore;

    @Column(name = "fluency_score")
    private Integer fluencyScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "speech_analysis_payload", columnDefinition = "jsonb")
    private JsonNode speechAnalysisPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reused_expression_payload", columnDefinition = "jsonb")
    private JsonNode reusedExpressionPayload;

    protected SessionHistoryMessage() {
    }

    private SessionHistoryMessage(
            Long sessionHistoryId,
            int messageSequence,
            int turnNumber,
            ConversationSpeaker role,
            String content,
            String translatedContent,
            SessionMessageInputType inputType,
            String innerThought,
            InnerThoughtType innerThoughtType
    ) {
        this.sessionHistoryId = sessionHistoryId;
        this.messageSequence = messageSequence;
        this.turnNumber = turnNumber;
        this.role = role;
        this.content = content;
        this.translatedContent = translatedContent;
        this.inputType = inputType;
        this.innerThought = innerThought;
        this.innerThoughtType = innerThoughtType;
    }

    /** AI first 시나리오의 첫 AI 메시지를 생성한다. */
    public static SessionHistoryMessage aiOpening(
            Long sessionHistoryId,
            String content,
            String translatedContent,
            String innerThought,
            InnerThoughtType innerThoughtType
    ) {
        return new SessionHistoryMessage(
                sessionHistoryId,
                1,
                1,
                ConversationSpeaker.AI,
                content,
                translatedContent,
                SessionMessageInputType.GENERATED,
                innerThought,
                innerThoughtType
        );
    }

    /** 사용자가 제출한 메시지를 생성한다. */
    public static SessionHistoryMessage user(
            Long sessionHistoryId,
            int messageSequence,
            int turnNumber,
            String content,
            SessionMessageInputType inputType
    ) {
        return new SessionHistoryMessage(
                sessionHistoryId,
                messageSequence,
                turnNumber,
                ConversationSpeaker.USER,
                content,
                null,
                inputType,
                null,
                null
        );
    }

    /** 히스토리에 저장할 AI 후속 메시지를 생성한다. */
    public static SessionHistoryMessage aiGenerated(
            Long sessionHistoryId,
            int messageSequence,
            int turnNumber,
            String content,
            String translatedContent
    ) {
        return new SessionHistoryMessage(
                sessionHistoryId,
                messageSequence,
                turnNumber,
                ConversationSpeaker.AI,
                content,
                translatedContent,
                SessionMessageInputType.GENERATED,
                null,
                null
        );
    }

    /** 사용자 메시지를 들은 상대 역할의 속마음을 기록한다. */
    public void recordInnerThought(String innerThought, InnerThoughtType innerThoughtType) {
        this.innerThought = innerThought;
        this.innerThoughtType = innerThoughtType;
    }

}
