// 세션 히스토리 메시지 엔티티의 저장을 담당한다.
package com.landit.landitbe.session.infrastructure;

import com.landit.landitbe.common.domain.ConversationSpeaker;
import com.landit.landitbe.session.domain.SessionHistoryMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionHistoryMessageRepository extends JpaRepository<SessionHistoryMessage, Long> {

    /** 세션 히스토리의 메시지를 메시지 순서대로 조회한다. */
    List<SessionHistoryMessage> findBySessionHistoryIdOrderByMessageSequenceAsc(Long sessionHistoryId);

    /** 세션 히스토리에서 특정 발화 주체의 메시지 수를 반환한다. */
    long countBySessionHistoryIdAndRole(Long sessionHistoryId, ConversationSpeaker role);
}
