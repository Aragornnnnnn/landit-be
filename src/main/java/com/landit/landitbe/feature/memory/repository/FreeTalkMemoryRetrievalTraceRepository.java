// 프리톡 장기기억 검색 후보와 실제 사용 결과를 최소 정보로 기록한다.

package com.landit.landitbe.feature.memory.repository;

import com.landit.landitbe.feature.memory.service.MemoryRetrievalStage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 세션별 장기기억 검색을 한 번으로 제한하고 후보·사용 trace를 저장한다. */
@Repository
public class FreeTalkMemoryRetrievalTraceRepository {

  private final JdbcTemplate jdbcTemplate;

  /** 검색 trace 저장에 사용할 JDBC 접근자로 저장소를 구성한다. */
  public FreeTalkMemoryRetrievalTraceRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** 세션과 검색 단계의 첫 조회를 원자적으로 선점한다. */
  public boolean claim(long sessionId, MemoryRetrievalStage stage, String policyVersion) {
    try {
      return jdbcTemplate.update(
              """
              INSERT INTO free_talk_memory_retrieval (
                  free_talk_session_id, retrieval_stage, candidate_rank, policy_version,
                  used, created_at)
              VALUES (?, ?, 0, ?, FALSE, CURRENT_TIMESTAMP)
              """,
              sessionId,
              stage.name(),
              policyVersion)
          == 1;
    } catch (DataIntegrityViolationException exception) {
      return false;
    }
  }

  /** 선점 marker를 보존하고 검색 후보를 저장한다. */
  public void saveCandidates(
      long sessionId,
      MemoryRetrievalStage stage,
      List<ConversationMemoryMatch> matches,
      String policyVersion) {
    if (matches == null || matches.isEmpty()) {
      return;
    }
    for (int index = 0; index < matches.size(); index++) {
      ConversationMemoryMatch match = matches.get(index);
      jdbcTemplate.update(
          """
          INSERT INTO free_talk_memory_retrieval (
              free_talk_session_id, retrieval_stage, memory_id, candidate_rank,
              distance, policy_version, used, created_at)
          VALUES (?, ?, ?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP)
          """,
          sessionId,
          stage.name(),
          match.memoryId(),
          index + 1,
          match.distance(),
          policyVersion);
    }
  }

  /** AI가 실제 사용했다고 응답한 후보와 연결 응답을 기록한다. */
  public void recordUsage(
      long sessionId,
      MemoryRetrievalStage stage,
      List<Long> usedMemoryIds,
      Long responseMessageId) {
    List<Long> ids = usedMemoryIds == null ? List.of() : usedMemoryIds;
    final String placeholders =
        ids.isEmpty() ? "NULL" : String.join(", ", java.util.Collections.nCopies(ids.size(), "?"));
    List<Object> parameters = new ArrayList<>(ids);
    parameters.add(responseMessageId);
    parameters.add(sessionId);
    parameters.add(stage.name());
    jdbcTemplate.update(
        """
        UPDATE free_talk_memory_retrieval
        SET used = CASE WHEN memory_id IN (%s) THEN TRUE ELSE FALSE END,
            response_message_id = ?
        WHERE free_talk_session_id = ? AND retrieval_stage = ?
        """
            .formatted(placeholders),
        parameters.toArray());
  }
}
