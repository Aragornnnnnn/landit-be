// 장기기억과 원문 source를 하나의 트랜잭션으로 저장한다.

package com.landit.landitbe.feature.memory.repository;

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

  private final NamedParameterJdbcTemplate jdbcTemplate;

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
