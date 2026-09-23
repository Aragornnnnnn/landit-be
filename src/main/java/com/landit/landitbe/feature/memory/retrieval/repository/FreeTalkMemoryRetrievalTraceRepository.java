// 프리톡 장기기억 검색 후보와 실제 사용 결과를 최소 정보로 기록한다.

package com.landit.landitbe.feature.memory.retrieval.repository;

import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.retrieval.domain.MemoryRetrievalStage;
import com.landit.landitbe.feature.memory.retrieval.dto.ConversationMemoryMatch;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 세션별 장기기억 검색을 한 번으로 제한하고 후보·사용 trace를 저장한다. */
@Repository
@RequiredArgsConstructor
public class FreeTalkMemoryRetrievalTraceRepository {

  private final JdbcTemplate jdbcTemplate;

  /**
   * 세션과 검색 단계의 첫 조회를 원자적으로 선점하고 중복 선점은 false로 처리한다.
   *
   * @param sessionId 장기기억을 조회할 프리톡 세션 ID
   * @param stage 조회를 수행하는 세션 시작 단계
   * @param policyVersion 조회 정책 버전
   * @return 처음 선점했으면 true, 이미 선점했거나 중복 제약에 걸리면 false
   */
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

  /**
   * 선점 marker를 보존하고 검색 후보를 저장한다.
   *
   * @param sessionId 장기기억을 조회한 프리톡 세션 ID
   * @param stage 조회를 수행한 세션 시작 단계
   * @param matches 저장할 검색 후보 목록
   * @param policyVersion 조회 정책 버전
   */
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

  /**
   * 세션 시작 단계들에서 이미 검색해 AI에 제공했던 기억을 가까운 순으로 다시 읽는다.
   *
   * <p>새로 검색하지 않는다. 다른 사용자의 기억이나 그사이 비활성화된 기억은 제외한다.
   *
   * @param sessionId 프리톡 세션 ID
   * @param userProfileId 세션 소유 사용자 프로필 ID
   * @param limit 돌려줄 최대 기억 수
   * @return 같은 기억이 여러 단계에 있어도 한 번만 담은 기억 문맥 목록
   */
  public List<AiFreeTalkMemoryContext> findRetrievedContexts(
      long sessionId, long userProfileId, int limit) {
    return jdbcTemplate.query(
        """
        SELECT cm.id, cm.memory_type, cm.content, cm.valid_from, cm.valid_to, cm.observed_at
        FROM conversation_memory cm
        JOIN (
            SELECT memory_id, MIN(distance) AS best_distance
            FROM free_talk_memory_retrieval
            WHERE free_talk_session_id = ?
              AND memory_id IS NOT NULL
              AND retrieval_stage IN ('OPENING', 'FIRST_USER_TURN')
            GROUP BY memory_id
        ) retrieved ON retrieved.memory_id = cm.id
        WHERE cm.user_profile_id = ? AND cm.status = 'ACTIVE'
        ORDER BY retrieved.best_distance, cm.id
        LIMIT ?
        """,
        (resultSet, rowNumber) ->
            new AiFreeTalkMemoryContext(
                resultSet.getLong("id"),
                ConversationMemoryType.valueOf(resultSet.getString("memory_type")),
                resultSet.getString("content"),
                ConversationMemorySearchSupport.toLocalDateTime(resultSet, "valid_from"),
                ConversationMemorySearchSupport.toLocalDateTime(resultSet, "valid_to"),
                ConversationMemorySearchSupport.toLocalDateTime(resultSet, "observed_at")),
        sessionId,
        userProfileId,
        limit);
  }

  /**
   * AI가 실제 사용했다고 응답한 후보와 연결 응답을 기록한다.
   *
   * @param sessionId 장기기억을 조회한 프리톡 세션 ID
   * @param stage 조회를 수행한 세션 시작 단계
   * @param usedMemoryIds AI가 실제 사용한 장기기억 ID 목록
   * @param responseMessageId 장기기억을 사용한 AI 응답 메시지 ID
   */
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
