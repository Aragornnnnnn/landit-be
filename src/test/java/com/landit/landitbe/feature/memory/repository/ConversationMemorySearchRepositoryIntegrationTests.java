// 장기기억 범위 선필터와 exact 코사인 검색 계약을 검증한다.

package com.landit.landitbe.feature.memory.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 장기기억 범위 선필터와 exact 코사인 검색 계약을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class ConversationMemorySearchRepositoryIntegrationTests {

  private static final long USER_ID = 997001L;
  private static final long OTHER_USER_ID = 997002L;
  private static final List<Float> QUERY_EMBEDDING = queryEmbedding();

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ConversationMemorySearchRepository searchRepository;

  @AfterEach
  void clearFixtures() {
    jdbcTemplate.update("delete from conversation_memory where id in (?, ?)", 997106L, 997107L);
    jdbcTemplate.update(
        "delete from conversation_memory where user_profile_id between ? and ?",
        USER_ID,
        OTHER_USER_ID);
    jdbcTemplate.update(
        "delete from user_profile where id between ? and ?", USER_ID, OTHER_USER_ID);
  }

  @Test
  void searchesOnlyCurrentScopeAndOrdersByDistanceThenMemoryId() {
    seedUser(USER_ID);
    seedUser(OTHER_USER_ID);
    seedMemory(997101L, USER_ID, null, "PROFILE", "ACTIVE", "[1,0,0]");
    seedMemory(997102L, USER_ID, "chloe", "EVENT", "ACTIVE", "[0.6,0.8,0]");
    seedMemory(997103L, USER_ID, "chloe", "EVENT", "ACTIVE", "[0.6,0.8,0]");
    seedMemory(997104L, USER_ID, "marco", "EVENT", "ACTIVE", "[1,0,0]");
    seedMemory(997106L, USER_ID, "chloe", "EVENT", "SUPERSEDED", "[1,0,0]");
    seedMemory(997107L, USER_ID, "chloe", "EVENT", "INVALIDATED", "[1,0,0]");
    seedMemory(997108L, OTHER_USER_ID, "chloe", "EVENT", "ACTIVE", "[1,0,0]");

    List<ConversationMemoryMatch> matches =
        searchRepository.searchActive(USER_ID, " chloe ", QUERY_EMBEDDING, 3);

    assertThat(matches)
        .extracting(ConversationMemoryMatch::memoryId)
        .containsExactly(997101L, 997102L, 997103L);
    assertThat(matches.get(0).distance()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-9));
    assertThat(matches.get(1).distance()).isCloseTo(0.4, org.assertj.core.data.Offset.offset(1e-9));
  }

  @Test
  void searchesComparableMemoriesByExactTypeAndScope() {
    seedUser(USER_ID);
    seedMemory(997120L, USER_ID, null, "PROFILE", "ACTIVE", "[1,0,0]");
    seedMemory(997122L, USER_ID, "chloe", "EVENT", "ACTIVE", "[0.9,0.1,0]");
    seedMemory(997123L, USER_ID, "chloe", "EVENT", "ACTIVE", "[0.8,0.2,0]");
    seedMemory(997124L, USER_ID, "chloe", "EPISODE", "ACTIVE", "[1,0,0]");

    List<ConversationMemoryMatch> matches =
        searchRepository.searchActiveComparable(
            QUERY_EMBEDDING, USER_ID, "chloe", ConversationMemoryType.EVENT, 3);

    assertThat(matches)
        .extracting(ConversationMemoryMatch::memoryId)
        .containsExactly(997122L, 997123L);
  }

  @Test
  void rejectsInvalidSearchArgumentsBeforeSql() {
    assertThatThrownBy(() -> searchRepository.searchActive(USER_ID, "chloe", List.of(0.1f), 1))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
    List<Float> nonFinite = validEmbedding();
    nonFinite.set(0, Float.NaN);
    assertThatThrownBy(() -> searchRepository.searchActive(USER_ID, "chloe", nonFinite, 1))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> searchRepository.searchActive(0, "chloe", QUERY_EMBEDDING, 1))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> searchRepository.searchActive(USER_ID, "chloe", QUERY_EMBEDDING, 0))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> searchRepository.searchActive(USER_ID, " ", QUERY_EMBEDDING, 1))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsStoredDimensionAndZeroVector() {
    seedUser(USER_ID);
    seedMemory(997110L, USER_ID, "chloe", "EVENT", "ACTIVE", "[1,0]");
    assertThatThrownBy(() -> searchRepository.searchActive(USER_ID, "chloe", QUERY_EMBEDDING, 1))
        .hasRootCauseInstanceOf(IllegalStateException.class);

    jdbcTemplate.update("delete from conversation_memory where id = ?", 997110L);
    seedMemory(997111L, USER_ID, "chloe", "EVENT", "ACTIVE", "[0,0,0]");
    assertThatThrownBy(() -> searchRepository.searchActive(USER_ID, "chloe", QUERY_EMBEDDING, 1))
        .hasRootCauseInstanceOf(IllegalStateException.class);
  }

  private void seedUser(long userId) {
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level,
            push_permission_status, status, created_at, updated_at)
        values (?, 'memory-search-user', 'EN', 'KR', 1, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId);
  }

  private void seedMemory(
      long memoryId,
      long userId,
      String characterId,
      String memoryType,
      String status,
      String embedding) {
    String supersededAt = "SUPERSEDED".equals(status) ? "CURRENT_TIMESTAMP" : "NULL";
    String supersededById = "SUPERSEDED".equals(status) ? "997101" : "NULL";
    String invalidatedAt = "INVALIDATED".equals(status) ? "CURRENT_TIMESTAMP" : "NULL";
    String invalidationReason = "INVALIDATED".equals(status) ? "'policy'" : "NULL";
    jdbcTemplate.update(
        """
        insert into conversation_memory (
            id, user_profile_id, character_id, memory_type, content, content_locale,
            confidence, status, valid_from, valid_to, observed_at, recorded_at,
            superseded_at, superseded_by_id, invalidated_at, invalidation_reason,
            extractor_version, embedding_model, embedding)
        values (?, ?, ?, ?, 'search memory', 'en', 0.8, ?, CURRENT_TIMESTAMP, NULL,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, %s, %s, %s, %s, 'extractor-v1',
            'embedding-v1', CAST(? AS extensions.vector))
        """
            .formatted(supersededAt, supersededById, invalidatedAt, invalidationReason),
        memoryId,
        userId,
        characterId,
        memoryType,
        status,
        normalizeEmbedding(embedding));
  }

  private static String normalizeEmbedding(String embedding) {
    if ("[1,0]".equals(embedding) || "[0,0,0]".equals(embedding)) {
      return embedding;
    }
    String[] components = embedding.substring(1, embedding.length() - 1).split(",");
    StringBuilder normalized = new StringBuilder("[");
    for (int index = 0; index < 1536; index++) {
      if (index > 0) {
        normalized.append(',');
      }
      normalized.append(index < components.length ? components[index] : "0");
    }
    return normalized.append(']').toString();
  }

  private static List<Float> queryEmbedding() {
    List<Float> embedding = validEmbedding();
    embedding.set(0, 1.0f);
    return List.copyOf(embedding);
  }

  private static List<Float> validEmbedding() {
    return new ArrayList<>(Collections.nCopies(1536, 0.0f));
  }
}
