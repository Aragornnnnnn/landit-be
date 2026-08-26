// PostgreSQL pgvector로 범위가 제한된 장기기억 exact 검색을 수행한다.

package com.landit.landitbe.feature.memory.repository;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL pgvector로 범위가 제한된 장기기억 exact 검색을 수행한다. */
@Repository
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

  private final NamedParameterJdbcTemplate jdbcTemplate;

  /**
   * Named parameter JDBC 접근자로 PostgreSQL 장기기억 검색 저장소를 구성한다.
   *
   * @param jdbcTemplate pgvector 검색에 사용할 JDBC 접근자
   */
  public PgVectorConversationMemorySearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** {@inheritDoc} */
  @Override
  public List<ConversationMemoryMatch> searchActive(
      long userProfileId, String characterId, List<Float> queryEmbedding, int limit) {
    String normalizedCharacterId =
        validateSearchArguments(queryEmbedding, userProfileId, characterId, limit);
    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("embedding", queryEmbedding.toString().replace(" ", ""))
            .addValue("userProfileId", userProfileId)
            .addValue("characterId", normalizedCharacterId)
            .addValue("limit", limit);
    return jdbcTemplate.query(
        SEARCH_SQL, parameters, PgVectorConversationMemorySearchRepository::mapMatch);
  }

  static String validateSearchArguments(
      List<Float> queryEmbedding, long userProfileId, String characterId, int limit) {
    if (queryEmbedding == null || queryEmbedding.size() != 1536) {
      throw new IllegalArgumentException("쿼리 임베딩 차원이 유효하지 않습니다.");
    }
    double queryNorm = 0.0;
    for (Float component : queryEmbedding) {
      if (component == null || !Float.isFinite(component)) {
        throw new IllegalArgumentException("쿼리 임베딩 값이 유효하지 않습니다.");
      }
      queryNorm += (double) component * component;
    }
    if (queryNorm == 0.0) {
      throw new IllegalArgumentException("쿼리 임베딩은 영벡터일 수 없습니다.");
    }
    if (userProfileId <= 0 || limit <= 0) {
      throw new IllegalArgumentException("검색 사용자와 제한 값이 유효하지 않습니다.");
    }
    if (characterId == null || characterId.isBlank()) {
      throw new IllegalArgumentException("검색 캐릭터는 필수입니다.");
    }
    return characterId.trim();
  }

  private static ConversationMemoryMatch mapMatch(ResultSet resultSet, int rowNumber)
      throws SQLException {
    return new ConversationMemoryMatch(
        resultSet.getLong("id"),
        ConversationMemoryType.valueOf(resultSet.getString("memory_type")),
        resultSet.getString("content"),
        toLocalDateTime(resultSet, "valid_from"),
        toLocalDateTime(resultSet, "valid_to"),
        toLocalDateTime(resultSet, "observed_at"),
        resultSet.getDouble("distance"));
  }

  static LocalDateTime toLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
    var timestamp = resultSet.getTimestamp(columnName);
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }
}
