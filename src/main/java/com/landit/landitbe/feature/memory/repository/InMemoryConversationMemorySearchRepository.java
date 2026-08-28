// H2에서 SQL 범위 선필터 후 자바 exact 코사인 검색을 수행한다.

package com.landit.landitbe.feature.memory.repository;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** H2에서 SQL 범위 선필터 후 자바 exact 코사인 검색을 수행한다. */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "landit.memory-search", name = "mode", havingValue = "in-memory")
public class InMemoryConversationMemorySearchRepository
    implements ConversationMemorySearchRepository {

  private static final String CANDIDATE_SQL =
      """
      SELECT cm.id, cm.memory_type, cm.content,
             cm.valid_from, cm.valid_to, cm.observed_at, cm.embedding
      FROM conversation_memory cm
      WHERE cm.user_profile_id = ?
        AND cm.status = 'ACTIVE'
        AND (cm.character_id IS NULL OR cm.character_id = ?)
      """;

  private final JdbcTemplate jdbcTemplate;

  /** {@inheritDoc} */
  @Override
  public List<ConversationMemoryMatch> searchActive(
      long userProfileId, String characterId, List<Float> queryEmbedding, int limit) {
    String normalizedCharacterId =
        ConversationMemorySearchSupport.validateSearchArguments(
            queryEmbedding, userProfileId, characterId, limit);
    return jdbcTemplate
        .query(
            CANDIDATE_SQL,
            InMemoryConversationMemorySearchRepository::mapMatch,
            userProfileId,
            normalizedCharacterId)
        .stream()
        .map(candidate -> withDistance(candidate, queryEmbedding))
        .sorted(
            Comparator.comparingDouble(ConversationMemoryMatch::distance)
                .thenComparingLong(ConversationMemoryMatch::memoryId))
        .limit(limit)
        .toList();
  }

  private static StoredCandidate mapMatch(ResultSet resultSet, int rowNumber) throws SQLException {
    ConversationMemoryMatch match =
        new ConversationMemoryMatch(
            resultSet.getLong("id"),
            ConversationMemoryType.valueOf(resultSet.getString("memory_type")),
            resultSet.getString("content"),
            ConversationMemorySearchSupport.toLocalDateTime(resultSet, "valid_from"),
            ConversationMemorySearchSupport.toLocalDateTime(resultSet, "valid_to"),
            ConversationMemorySearchSupport.toLocalDateTime(resultSet, "observed_at"),
            0.0);
    return new StoredCandidate(match, parseVector(resultSet.getString("embedding")));
  }

  private static ConversationMemoryMatch withDistance(
      StoredCandidate candidate, List<Float> queryEmbedding) {
    double distance = cosineDistance(queryEmbedding, candidate.embedding());
    ConversationMemoryMatch match = candidate.match();
    return new ConversationMemoryMatch(
        match.memoryId(),
        match.memoryType(),
        match.content(),
        match.validFrom(),
        match.validTo(),
        match.observedAt(),
        distance);
  }

  /** H2 JDBC가 반환한 벡터 문자열을 차원·유한값이 보장된 배열로 변환한다. */
  private static double[] parseVector(String storedVector) {
    if (storedVector == null) {
      throw new IllegalStateException("저장 임베딩이 없습니다.");
    }
    String normalized = storedVector.trim();
    if (!normalized.startsWith("[") || !normalized.endsWith("]")) {
      throw new IllegalStateException("저장 임베딩 형식이 유효하지 않습니다.");
    }
    String body = normalized.substring(1, normalized.length() - 1).trim();
    if (body.isEmpty()) {
      throw new IllegalStateException("저장 임베딩 차원이 유효하지 않습니다.");
    }
    String[] components = body.split(",");
    if (components.length != 1536) {
      throw new IllegalStateException("저장 임베딩 차원이 유효하지 않습니다.");
    }
    return parseComponents(components);
  }

  private static double[] parseComponents(String[] components) {
    double[] vector = new double[components.length];
    for (int index = 0; index < components.length; index++) {
      try {
        vector[index] = Double.parseDouble(components[index].trim());
      } catch (NumberFormatException exception) {
        throw new IllegalStateException("저장 임베딩 값이 유효하지 않습니다.", exception);
      }
      if (!Double.isFinite(vector[index])) {
        throw new IllegalStateException("저장 임베딩 값이 유효하지 않습니다.");
      }
    }
    return vector;
  }

  private static double cosineDistance(List<Float> query, double[] stored) {
    double dotProduct = 0.0;
    double queryNorm = 0.0;
    double storedNorm = 0.0;
    for (int index = 0; index < stored.length; index++) {
      double queryComponent = query.get(index);
      dotProduct += queryComponent * stored[index];
      queryNorm += queryComponent * queryComponent;
      storedNorm += stored[index] * stored[index];
    }
    if (storedNorm == 0.0) {
      throw new IllegalStateException("저장 임베딩은 영벡터일 수 없습니다.");
    }
    return 1.0 - dotProduct / (Math.sqrt(queryNorm) * Math.sqrt(storedNorm));
  }

  private record StoredCandidate(ConversationMemoryMatch match, double[] embedding) {}
}
