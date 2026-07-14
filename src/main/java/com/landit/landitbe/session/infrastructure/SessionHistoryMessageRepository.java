// 세션 히스토리 메시지 엔티티의 저장을 담당한다.
package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.common.domain.InnerThoughtType;
import com.landit.landitbe.session.domain.ProcessingStatus;
import com.landit.landitbe.session.domain.SessionHistoryMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionHistoryMessageRepository extends JpaRepository<SessionHistoryMessage, Long> {

    /** 세션 히스토리의 메시지를 메시지 순서대로 조회한다. */
    List<SessionHistoryMessage> findBySessionHistoryIdOrderByMessageSequenceAsc(Long sessionHistoryId);

    /** 세션 히스토리에 속한 특정 메시지를 조회한다. */
    Optional<SessionHistoryMessage> findByIdAndSessionHistoryId(Long id, Long sessionHistoryId);

    /** 세션 히스토리에서 특정 발화 주체의 메시지 수를 반환한다. */
    long countBySessionHistoryIdAndRole(Long sessionHistoryId, ConversationSpeaker role);

    /** 준비 상태인 메시지에만 속마음 생성 결과를 반영한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update SessionHistoryMessage message
            set message.innerThought = :innerThought,
                message.innerThoughtType = :innerThoughtType,
                message.innerThoughtProcessingStatus = :completedStatus
            where message.id = :messageId
              and message.innerThoughtProcessingStatus = :preparingStatus
            """)
    int completeInnerThoughtIfPreparing(
            @Param("messageId") long messageId,
            @Param("innerThought") String innerThought,
            @Param("innerThoughtType") InnerThoughtType innerThoughtType,
            @Param("completedStatus") ProcessingStatus completedStatus,
            @Param("preparingStatus") ProcessingStatus preparingStatus
    );

    /** 준비 상태인 메시지의 속마음 처리 상태만 실패로 바꾼다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update SessionHistoryMessage message
            set message.innerThoughtProcessingStatus = :failedStatus
            where message.id = :messageId
              and message.innerThoughtProcessingStatus = :preparingStatus
            """)
    int markInnerThoughtFailedIfPreparing(
            @Param("messageId") long messageId,
            @Param("failedStatus") ProcessingStatus failedStatus,
            @Param("preparingStatus") ProcessingStatus preparingStatus
    );
}
