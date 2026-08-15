// 편지함 어드민 공지·업데이트 응답을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetter;
import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.domain.MailboxPublicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 편지함 어드민 공지·업데이트 응답이다.
 *
 * @param letterId 편지 ID
 * @param type 편지 유형
 * @param title 편지 제목
 * @param contentBlocks 구조화된 본문 블록
 * @param preview 목록 미리보기
 * @param publicationStatus 게시 상태
 * @param pinned 상단 고정 여부
 * @param publishedAt 게시 시각
 * @param createdAt 생성 시각
 * @param updatedAt 수정 시각
 */
@Schema(description = "편지함 어드민 공지·업데이트 응답")
public record AdminMailboxLetterResponse(
    Long letterId,
    MailboxLetterType type,
    String title,
    @Schema(description = "구조화된 본문 블록") List<Object> contentBlocks,
    String preview,
    MailboxPublicationStatus publicationStatus,
    boolean pinned,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  // Entity의 Jackson 2 JsonNode를 Spring Web의 Jackson 3 직렬화 가능 값으로 변환한다.
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<Object>> CONTENT_BLOCKS_TYPE = new TypeReference<>() {};

  /**
   * 편지 Entity를 어드민 응답으로 변환한다.
   *
   * @param letter 변환할 편지
   * @return 어드민 편지 응답
   */
  public static AdminMailboxLetterResponse from(MailboxLetter letter) {
    return new AdminMailboxLetterResponse(
        letter.getId(),
        letter.getLetterType(),
        letter.getTitle(),
        toContentBlocks(letter.getContentBlocks()),
        letter.getPreviewText(),
        letter.getPublicationStatus(),
        letter.isPinned(),
        letter.getPublishedAt(),
        letter.getCreatedAt(),
        letter.getUpdatedAt());
  }

  private static List<Object> toContentBlocks(JsonNode contentBlocks) {
    return contentBlocks == null
        ? null
        : OBJECT_MAPPER.convertValue(contentBlocks, CONTENT_BLOCKS_TYPE);
  }
}
