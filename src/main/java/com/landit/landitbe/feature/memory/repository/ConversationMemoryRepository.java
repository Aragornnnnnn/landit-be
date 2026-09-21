// 장기기억과 원문 source를 하나의 트랜잭션으로 저장한다.

package com.landit.landitbe.feature.memory.repository;

import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.domain.NewConversationMemory;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 장기기억 원문과 원본 메시지 계보를 저장한다. */
@Repository
@RequiredArgsConstructor
public class ConversationMemoryRepository {

  private static final String INSERT_MEMORY_SQL =
      """
      INSERT INTO conversation_memory (
          user_profile_id, character_id, memory_type, content, content_locale,
          confidence, status, valid_from, valid_to, observed_at, recorded_at, superseded_at,
          superseded_by_id, invalidated_at, invalidation_reason, extractor_version,
          embedding_model, embedding)
      VALUES (
          :userProfileId, :characterId, :memoryType, :content, :contentLocale,
          :confidence, 'ACTIVE', :validFrom, :validTo, :observedAt, :recordedAt, NULL,
          NULL, NULL, NULL, :extractorVersion, :embeddingModel,
          CAST(:embedding AS extensions.vector))
      """;

  private static final String SUPERSEDE_ACTIVE_SQL =
      """
      UPDATE conversation_memory
      SET status = 'SUPERSEDED', valid_to = :validTo, superseded_at = :supersededAt,
          superseded_by_id = :newMemoryId
      WHERE id = :oldMemoryId
        AND status = 'ACTIVE'
      """;

  private static final String INSERT_SOURCE_SQL =
      """
      INSERT INTO conversation_memory_source (memory_id, session_history_message_id)
      VALUES (:memoryId, :sourceMessageId)
      """;

  private static final String DELETE_RETRIEVAL_TRACE_SQL =
      """
      DELETE FROM free_talk_memory_retrieval
      WHERE free_talk_session_id IN (
          SELECT fts.id
          FROM free_talk_session fts
          JOIN learning_session ls ON ls.id = fts.learning_session_id
          WHERE ls.user_profile_id = :userProfileId)
      """;

  private static final String DELETE_MEMORY_SOURCE_SQL =
      """
      DELETE FROM conversation_memory_source
      WHERE memory_id IN (
          SELECT id FROM conversation_memory WHERE user_profile_id = :userProfileId)
      """;

  private static final String DELETE_MEMORY_SQL =
      "DELETE FROM conversation_memory WHERE user_profile_id = :userProfileId";

  private static final String FIND_RECENT_ACTIVE_SQL =
      """
      SELECT id, memory_type, content, valid_from, valid_to, observed_at
      FROM conversation_memory
      WHERE user_profile_id = :userProfileId
        AND status = 'ACTIVE'
        AND (character_id IS NULL OR character_id = :characterId)
        %s
      ORDER BY observed_at DESC, id DESC
      LIMIT :limit
      """;
  private static final String EXISTS_ACTIVE_SQL =
      """
      SELECT COUNT(*)
      FROM conversation_memory
      WHERE id = :memoryId
        AND user_profile_id = :userProfileId
        AND status = 'ACTIVE'
      """;
  private static final String EXCLUDED_IDS_CONDITION = "AND id NOT IN (:excludedMemoryIds)";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  /**
   * 사용자가 이 캐릭터와 나눌 수 있는 활성 기억을 최근에 말한 순으로 읽는다.
   *
   * <p>범위는 기억 검색과 같다(캐릭터 공용 기억 + 그 캐릭터의 기억). 기억 내용은 컬럼 길이(500자)가 AI 서버의 상한과 같아 따로 거르지 않는다.
   *
   * @param userProfileId 기억 소유 사용자 프로필 ID
   * @param characterId 현재 대화 캐릭터 ID
   * @param excludedMemoryIds 결과에서 뺄 기억 ID. 상한만큼의 자리를 쓸 수 있는 기억으로 채우려고 조회할 때 뺀다
   * @param limit 돌려줄 최대 기억 수
   * @return 최근에 말한 순의 기억 문맥
   */
  public List<AiFreeTalkMemoryContext> findRecentActiveContexts(
      long userProfileId, String characterId, List<Long> excludedMemoryIds, int limit) {
    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("userProfileId", userProfileId)
            .addValue("characterId", characterId)
            .addValue("limit", limit);
    // 빈 목록의 NOT IN ()은 SQL 문법 오류라 뺄 기억이 있을 때만 조건을 붙인다.
    boolean excludes = !excludedMemoryIds.isEmpty();
    if (excludes) {
      parameters.addValue("excludedMemoryIds", excludedMemoryIds);
    }
    return jdbcTemplate.query(
        FIND_RECENT_ACTIVE_SQL.formatted(excludes ? EXCLUDED_IDS_CONDITION : ""),
        parameters,
        (resultSet, rowNumber) ->
            new AiFreeTalkMemoryContext(
                resultSet.getLong("id"),
                ConversationMemoryType.valueOf(resultSet.getString("memory_type")),
                resultSet.getString("content"),
                toLocalDateTime(resultSet.getTimestamp("valid_from")),
                toLocalDateTime(resultSet.getTimestamp("valid_to")),
                toLocalDateTime(resultSet.getTimestamp("observed_at"))));
  }

  /**
   * 그 사용자의 기억이 지금도 활성 상태인지 확인한다.
   *
   * @param userProfileId 기억 소유 사용자 프로필 ID
   * @param memoryId 확인할 장기기억 ID
   * @return 본인 소유의 활성 기억이면 true. 대체·무효화됐거나 없거나 다른 사용자의 기억이면 false
   */
  public boolean existsActive(long userProfileId, long memoryId) {
    Integer count =
        jdbcTemplate.queryForObject(
            EXISTS_ACTIVE_SQL,
            new MapSqlParameterSource()
                .addValue("userProfileId", userProfileId)
                .addValue("memoryId", memoryId),
            Integer.class);
    return count != null && count > 0;
  }

  private static LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  /**
   * 탈퇴 사용자의 기억·원본 계보·검색 trace를 FK 의존 순서로 제거한다.
   *
   * @param userProfileId 삭제할 사용자의 프로필 ID
   * @return 삭제한 장기기억 행 수
   */
  public int deleteAllByUserProfileId(long userProfileId) {
    MapSqlParameterSource parameters =
        new MapSqlParameterSource().addValue("userProfileId", userProfileId);
    deleteRetrievalTraces(parameters);
    deleteMemorySources(parameters);
    return deleteMemories(parameters);
  }

  private void deleteRetrievalTraces(MapSqlParameterSource parameters) {
    jdbcTemplate.update(DELETE_RETRIEVAL_TRACE_SQL, parameters);
  }

  private void deleteMemorySources(MapSqlParameterSource parameters) {
    jdbcTemplate.update(DELETE_MEMORY_SOURCE_SQL, parameters);
  }

  private int deleteMemories(MapSqlParameterSource parameters) {
    return jdbcTemplate.update(DELETE_MEMORY_SQL, parameters);
  }

  /**
   * 장기기억과 원본 메시지 source를 하나의 트랜잭션으로 저장한다.
   *
   * @param memory 저장할 장기기억 입력
   * @param sourceMessageIds 기억의 근거가 되는 원본 메시지 ID 목록
   * @return 생성된 장기기억 ID
   * @throws IllegalArgumentException source ID가 비어 있거나 중복·비양수인 경우
   * @throws IllegalStateException 장기기억 ID를 생성하지 못한 경우
   */
  @Transactional
  public long save(NewConversationMemory memory, List<Long> sourceMessageIds) {
    validateSourceMessageIds(sourceMessageIds);
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        INSERT_MEMORY_SQL, memoryParameters(memory), keyHolder, new String[] {"id"});

    Number memoryId = keyHolder.getKey();
    if (memoryId == null) {
      throw new IllegalStateException("장기기억 ID를 생성하지 못했습니다.");
    }
    jdbcTemplate.batchUpdate(
        INSERT_SOURCE_SQL,
        sourceMessageIds.stream()
            .map(
                sourceMessageId ->
                    new MapSqlParameterSource()
                        .addValue("memoryId", memoryId.longValue())
                        .addValue("sourceMessageId", sourceMessageId))
            .toArray(MapSqlParameterSource[]::new));
    return memoryId.longValue();
  }

  /**
   * 기존 활성 기억을 새 기억으로 조건부 대체하고 유효 기간을 닫는다.
   *
   * @param oldMemoryId 대체할 기존 기억 ID
   * @param newMemoryId 기존 기억이 가리킬 새 기억 ID
   * @param validTo 기존 기억의 유효 종료 시각
   * @param supersededAt 대체 상태가 기록된 시각
   * @return 기존 기억이 활성 상태여서 갱신됐으면 true
   * @throws IllegalArgumentException ID 또는 시각 값이 유효하지 않은 경우
   */
  public boolean supersedeActive(
      long oldMemoryId, long newMemoryId, LocalDateTime validTo, LocalDateTime supersededAt) {
    if (oldMemoryId <= 0 || newMemoryId <= 0 || validTo == null || supersededAt == null) {
      throw new IllegalArgumentException("대체할 장기기억 상태 값이 유효하지 않습니다.");
    }
    int updated =
        jdbcTemplate.update(
            SUPERSEDE_ACTIVE_SQL,
            new MapSqlParameterSource()
                .addValue("oldMemoryId", oldMemoryId)
                .addValue("newMemoryId", newMemoryId)
                .addValue("validTo", validTo)
                .addValue("supersededAt", supersededAt));
    return updated == 1;
  }

  private static MapSqlParameterSource memoryParameters(NewConversationMemory memory) {
    return new MapSqlParameterSource()
        .addValue("userProfileId", memory.userProfileId())
        .addValue("characterId", memory.characterId())
        .addValue("memoryType", memory.memoryType().name())
        .addValue("content", memory.content())
        .addValue("contentLocale", memory.contentLocale().toLanguageTag())
        .addValue("confidence", memory.confidence())
        .addValue("validFrom", memory.validFrom())
        .addValue("validTo", memory.validTo())
        .addValue("observedAt", memory.observedAt())
        .addValue("recordedAt", memory.recordedAt())
        .addValue("extractorVersion", memory.extractorVersion())
        .addValue("embeddingModel", memory.embeddingModel())
        .addValue("embedding", memory.embedding().toString().replace(" ", ""));
  }

  private static void validateSourceMessageIds(List<Long> sourceMessageIds) {
    if (sourceMessageIds == null || sourceMessageIds.isEmpty()) {
      throw new IllegalArgumentException("원본 메시지 ID가 필요합니다.");
    }
    Set<Long> uniqueIds = new HashSet<>();
    for (Long sourceMessageId : sourceMessageIds) {
      if (sourceMessageId == null || sourceMessageId <= 0 || !uniqueIds.add(sourceMessageId)) {
        throw new IllegalArgumentException("원본 메시지 ID 목록이 유효하지 않습니다.");
      }
    }
  }
}
