// H2와 PostgreSQL 장기기억 검색이 공유하는 입력·JDBC 값 검증을 제공한다.

package com.landit.landitbe.feature.memory.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

final class ConversationMemorySearchSupport {

  private static final int EMBEDDING_DIMENSION = 1536;

  private ConversationMemorySearchSupport() {}

  static String validateSearchArguments(
      List<Float> queryEmbedding, long userProfileId, String characterId, int limit) {
    validateEmbedding(queryEmbedding);
    if (userProfileId <= 0 || limit <= 0) {
      throw new IllegalArgumentException("검색 사용자와 제한 값이 유효하지 않습니다.");
    }
    if (characterId == null || characterId.isBlank()) {
      throw new IllegalArgumentException("검색 캐릭터는 필수입니다.");
    }
    return characterId.trim();
  }

  static LocalDateTime toLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
    var timestamp = resultSet.getTimestamp(columnName);
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  private static void validateEmbedding(List<Float> queryEmbedding) {
    if (queryEmbedding == null || queryEmbedding.size() != EMBEDDING_DIMENSION) {
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
  }
}
