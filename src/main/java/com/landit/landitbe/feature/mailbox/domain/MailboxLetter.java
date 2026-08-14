// 편지함에 표시되는 공지·업데이트·답장을 저장하는 Entity다.

package com.landit.landitbe.feature.mailbox.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 편지함에 표시되는 공지·업데이트·답장을 저장하는 Entity다. */
@Getter
@Entity
@Table(name = "mailbox_letter")
public class MailboxLetter extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "letter_type", nullable = false, length = 20)
  private MailboxLetterType letterType;

  @Column(nullable = false, length = 200)
  private String title;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "content_blocks", columnDefinition = "jsonb")
  private JsonNode contentBlocks;

  @Column(name = "body_text", columnDefinition = "text")
  private String bodyText;

  @Column(name = "preview_text", nullable = false, columnDefinition = "text")
  private String previewText;

  @Enumerated(EnumType.STRING)
  @Column(name = "publication_status", nullable = false, length = 20)
  private MailboxPublicationStatus publicationStatus;

  @Column(name = "is_pinned", nullable = false)
  private boolean pinned;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  /** JPA에서 사용하는 기본 생성자다. */
  protected MailboxLetter() {}

  /**
   * 편지함 콘텐츠를 저장할 Entity를 생성한다.
   *
   * @param letterType 편지 유형
   * @param title 편지 제목
   * @param contentBlocks 구조화된 본문 블록
   * @param bodyText 텍스트 본문
   * @param previewText 목록 미리보기
   * @param publicationStatus 게시 상태
   * @param pinned 상단 고정 여부
   * @param publishedAt 게시 시각
   */
  public MailboxLetter(
      MailboxLetterType letterType,
      String title,
      JsonNode contentBlocks,
      String bodyText,
      String previewText,
      MailboxPublicationStatus publicationStatus,
      boolean pinned,
      LocalDateTime publishedAt) {
    this.letterType = letterType;
    this.title = title;
    this.contentBlocks = contentBlocks;
    this.bodyText = bodyText;
    this.previewText = previewText;
    this.publicationStatus = publicationStatus;
    this.pinned = pinned;
    this.publishedAt = publishedAt;
  }
}
