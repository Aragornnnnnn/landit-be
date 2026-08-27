// PostgreSQL pgvector로 범위가 제한된 장기기억 exact 검색을 수행한다.

package com.landit.landitbe.feature.memory.repository;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL pgvector로 범위가 제한된 장기기억 exact 검색을 수행한다. */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "landit.memory-search",
    name = "mode",
    havingValue = "pgvector",
    matchIfMissing = true)
public class PgVectorConversationMemorySearchRepository
    implements ConversationMemorySearchRepository {

  static final String SEARCH_SQL =
      """
      SELECT cm.id, cm.memory_type, cm.content,
             cm.valid_from, cm.valid_to, cm.observed_at,
             cm.embedding <=> CAST(:embedding AS extensions.vector) AS distance
      FROM conversation_memory cm
      WHERE cm.user_profile_id = :userProfileId
        AND cm.status = 'ACTIVE'
        AND (cm.character_id IS NULL OR cm.character_id = :characterId)
      ORDER BY cm.embedding <=> CAST(:embedding AS extensions.vector), cm.id
      LIMIT :limit
      """;

  static final String COMPARABLE_SEARCH_SQL =
      """
      SELECT cm.id, cm.memory_type, cm.content,
             cm.valid_from, cm.valid_to, cm.observed_at,
             cm.embedding <=> CAST(:embedding AS extensions.vector) AS distance
      FROM conversation_memory cm
      WHERE cm.user_profile_id = :userProfileId
        AND cm.status = 'ACTIVE'
        AND cm.memory_type = :memoryType
        AND cm.character_id IS NULL
      ORDER BY cm.embedding <=> CAST(:embedding AS extensions.vector), cm.id
      LIMIT :limit
      """;
  private final NamedParameterJdbcTemplate jdbcTemplate;

  /** {@inheritDoc} */
  @Override
  public List<ConversationMemoryMatch> searchActive(
      long userProfileId, String characterId, List<Float> queryEmbedding, int limit) {
    String normalizedCharacterId =
        ConversationMemorySearchSupport.validateSearchArguments(
            queryEmbedding, userProfileId, characterId, limit);
    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("embedding", queryEmbedding.toString().replace(" ", ""))
            .addValue("userProfileId", userProfileId)
            .addValue("characterId", normalizedCharacterId)
            .addValue("limit", limit);
    return jdbcTemplate.query(
        SEARCH_SQL, parameters, PgVectorConversationMemorySearchRepository::mapMatch);
  }

  /** {@inheritDoc} */
  @Override
  public List<ConversationMemoryMatch> searchActiveComparable(
      List<Float> queryEmbedding,
      long userProfileId,
      String characterId,
      ConversationMemoryType memoryType,
      int limit) {
    String normalizedCharacterId =
        ConversationMemorySearchSupport.validateComparableSearchArguments(
            queryEmbedding, userProfileId, characterId, memoryType, limit);
    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("embedding", toVectorLiteral(queryEmbedding))
            .addValue("userProfileId", userProfileId)
            .addValue("memoryType", memoryType.name())
            .addValue("limit", limit);
    String sql = COMPARABLE_SEARCH_SQL;
    if (normalizedCharacterId != null) {
      sql =
          COMPARABLE_SEARCH_SQL.replace(
              "cm.character_id IS NULL", "cm.character_id = :characterId");
      parameters.addValue("characterId", normalizedCharacterId);
    }
    return jdbcTemplate.query(
        sql, parameters, PgVectorConversationMemorySearchRepository::mapMatch);
  }

  private static String toVectorLiteral(List<Float> embedding) {
    return embedding.toString().replace(" ", "");
  }

  private static ConversationMemoryMatch mapMatch(ResultSet resultSet, int rowNumber)
      throws SQLException {
    return new ConversationMemoryMatch(
        resultSet.getLong("id"),
        ConversationMemoryType.valueOf(resultSet.getString("memory_type")),
        resultSet.getString("content"),
        ConversationMemorySearchSupport.toLocalDateTime(resultSet, "valid_from"),
        ConversationMemorySearchSupport.toLocalDateTime(resultSet, "valid_to"),
        ConversationMemorySearchSupport.toLocalDateTime(resultSet, "observed_at"),
        resultSet.getDouble("distance"));
  }
}
