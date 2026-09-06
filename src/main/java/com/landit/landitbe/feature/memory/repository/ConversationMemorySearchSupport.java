// H2와 PostgreSQL 장기기억 검색이 공유하는 입력·JDBC 값 검증을 제공한다.

package com.landit.landitbe.feature.memory.repository;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** H2와 PostgreSQL이 같은 검색 입력 계약과 JDBC 시각 변환을 사용하도록 한다. */
final class ConversationMemorySearchSupport {

  private static final int EMBEDDING_DIMENSION = 1536;

  private ConversationMemorySearchSupport() {}

  /**
   * 일반 검색 입력이 양수 사용자·제한과 비어 있지 않은 캐릭터 범위를 갖는지 확인한다.
   *
   * @param queryEmbedding 검색 쿼리 1,536차원 임베딩
   * @param userProfileId 검색 대상 사용자 프로필 ID
   * @param characterId 현재 대화 캐릭터 ID
   * @param limit 반환할 최대 결과 수
   * @return 앞뒤 공백을 제거한 캐릭터 ID
   * @throws IllegalArgumentException 임베딩·사용자·캐릭터·제한 값이 유효하지 않은 경우
   */
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

  /**
   * 후보와 같은 유형·범위의 비교 검색 입력이 유효한지 확인한다.
   *
   * @param queryEmbedding 검색 쿼리 1,536차원 임베딩
   * @param userProfileId 검색 대상 사용자 프로필 ID
   * @param characterId 비교할 캐릭터 ID. PROFILE은 null이다.
   * @param memoryType 비교할 장기기억 의미 유형
   * @param limit 반환할 최대 결과 수
   * @return 앞뒤 공백을 제거한 캐릭터 ID 또는 PROFILE의 null 범위
   * @throws IllegalArgumentException 임베딩·범위·유형·제한 값이 유효하지 않은 경우
   */
  static String validateComparableSearchArguments(
      List<Float> queryEmbedding,
      long userProfileId,
      String characterId,
      ConversationMemoryType memoryType,
      int limit) {
    validateEmbedding(queryEmbedding);
    if (userProfileId <= 0 || limit <= 0 || memoryType == null) {
      throw new IllegalArgumentException("비교 검색 사용자·유형·제한 값이 유효하지 않습니다.");
    }
    if (memoryType == ConversationMemoryType.PROFILE) {
      if (characterId != null && !characterId.isBlank()) {
        throw new IllegalArgumentException("프로필 비교 검색의 캐릭터 범위가 유효하지 않습니다.");
      }
      return null;
    }
    if (characterId == null || characterId.isBlank()) {
      throw new IllegalArgumentException("이벤트·에피소드 비교 검색 캐릭터가 필요합니다.");
    }
    return characterId.trim();
  }

  /**
   * JDBC timestamp를 애플리케이션의 지역 없는 시각으로 변환한다.
   *
   * @param resultSet JDBC 조회 결과
   * @param columnName 변환할 timestamp 컬럼명
   * @return 컬럼 값 또는 null
   * @throws SQLException JDBC 결과를 읽지 못한 경우
   */
  static LocalDateTime toLocalDateTime(ResultSet resultSet, String columnName) throws SQLException {
    var timestamp = resultSet.getTimestamp(columnName);
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  /** 코사인 검색이 가능한 유한한 1,536차원 비영 임베딩인지 확인한다. */
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
